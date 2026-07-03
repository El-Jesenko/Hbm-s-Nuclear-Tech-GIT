package com.hbm.uninos.networkproviders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.tileentity.machine.TileEntityMachineAutocrafter;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube.PneumaticChannel;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import com.hbm.uninos.NodeNet;
import com.hbm.util.BobMathUtil;
import com.hbm.util.ItemStackUtil;
import com.hbm.util.Tuple.Triplet;

import api.hbm.ntl.ISlotMonitorProvider;
import api.hbm.ntl.StackCache;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

public class PneumaticNetwork extends NodeNet {

	public static final byte SEND_FIRST = 0;
	public static final byte SEND_LAST = 1;
	public static final byte SEND_RANDOM = 2;
	public static final byte RECEIVE_ROBIN = 0;
	public static final byte RECEIVE_EVEN = 1;
	public static final byte RECEIVE_RANDOM = 2;
	public static final int SHARED_AIR_CAPACITY = 4_000;
	
	public Random rand = new Random();

	protected static final int timeout = 1_000;
	public static final int ITEMS_PER_TRANSFER = 64;

	// while the system has parts that expects IInventires to be TileEntities to work properly (mostly range checks),
	// it can actually handle non-TileEntities just fine.
	public HashMap<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>> receivers = new HashMap();
	public HashMap<TileEntityPneumoTube, Long> compressors = new HashMap();
	public HashMap<AirKey, Integer> sharedAir = new HashMap();

	public LinkedHashSet<StackCache> accessors = new LinkedHashSet();
	public LinkedHashSet<ISlotMonitorProvider> storages = new LinkedHashSet();
	
	@Override
	public void destroy() {
		super.destroy();
		receivers.clear();
		compressors.clear();
		sharedAir.clear();
		for(StackCache cache : accessors) cache.dissolveCache();
		accessors.clear();
		storages.clear();
	}

	public void addReceiver(IInventory inventory, ForgeDirection pipeDir, TileEntityPneumoTube endpoint) {
		receivers.put(inventory, new Triplet(pipeDir, System.currentTimeMillis(), endpoint));
	}

	public void addCompressor(TileEntityPneumoTube tube) {
		compressors.put(tube, System.currentTimeMillis());
		AirKey key = AirKey.fromTube(tube);
		int current = sharedAir.containsKey(key) ? sharedAir.get(key) : 0;
		if(tube.compair.getFill() > current) {
			sharedAir.put(key, tube.compair.getFill());
			syncAirState(key);
		} else if(!sharedAir.containsKey(key)) {
			sharedAir.put(key, tube.compair.getFill());
		}
	}
	
	public void addStackCache(StackCache accessor) {
		if(accessors.add(accessor)) {
			for(ISlotMonitorProvider storage : storages) storage.onNewCacheHasJoined(accessor, this);
		}
	}

	@Override public void update() {

		// weeds out invalid targets
		// technically not necessary since that step is taken during the send operation,
		// but we still want to reap garbage data that would otherwise accumulate
		long timestamp = System.currentTimeMillis();
		receivers.entrySet().removeIf(x -> { return (timestamp - x.getValue().getY() > timeout) || NodeNet.isBadLink(x.getKey()); });
		compressors.entrySet().removeIf(x -> { return (timestamp - x.getValue() > timeout) || NodeNet.isBadLink(x.getKey()); });
		sharedAir.entrySet().removeIf(x -> !hasCompressorFor(x.getKey()));
		accessors.removeIf(x -> { return x.hasExpired; });
	}

	public long getAirAvailable(FluidType type, int pressure) {
		return sharedAir.containsKey(new AirKey(type, pressure)) ? sharedAir.get(new AirKey(type, pressure)) : 0;
	}

	public long getAirDemand(FluidType type, int pressure) {
		return SHARED_AIR_CAPACITY - getAirAvailable(type, pressure);
	}

	public long addAir(FluidType type, int pressure, long amount) {
		AirKey key = new AirKey(type, pressure);
		int stored = (int) getAirAvailable(type, pressure);
		int toAdd = (int) Math.min(amount, SHARED_AIR_CAPACITY - stored);
		sharedAir.put(key, stored + toAdd);
		syncAirState(key);
		return amount - toAdd;
	}

	public void useAir(FluidType type, int pressure, long amount) {
		AirKey key = new AirKey(type, pressure);
		int stored = (int) getAirAvailable(type, pressure);
		sharedAir.put(key, Math.max(0, stored - (int) amount));
		syncAirState(key);
	}

	public long getAirReceiverSpeed(FluidType type, int pressure) {
		return Math.max(1, (SHARED_AIR_CAPACITY - (int) getAirAvailable(type, pressure)) / 25);
	}

	protected boolean hasCompressorFor(AirKey key) {
		for(TileEntityPneumoTube tube : compressors.keySet()) {
			if(key.matches(tube)) return true;
		}

		return false;
	}

	protected void syncAirState(AirKey key) {
		int stored = sharedAir.containsKey(key) ? sharedAir.get(key) : 0;

		for(TileEntityPneumoTube tube : compressors.keySet()) {
			if(key.matches(tube) && tube.compair.getFill() != stored) {
				tube.compair.setFill(stored);
				tube.markDirty();
			}
		}
	}

	public boolean send(IInventory source, TileEntityPneumoTube tube, ForgeDirection accessDir, int sendOrder, int receiveOrder, int maxRange, int nextReceiver, int roundRobinAmount) {

		// turns out there may be a short time window where the cleanup hasn't happened yet, but chunkloading has already caused tiles to go invalid
		// so we just run it again here, just to be sure.
		long timestamp = System.currentTimeMillis();
		receivers.entrySet().removeIf(x -> { return (timestamp - x.getValue().getY() > timeout) || NodeNet.isBadLink(x.getKey()); });
		compressors.entrySet().removeIf(x -> { return (timestamp - x.getValue() > timeout) || NodeNet.isBadLink(x.getKey()); });
		
		if(receivers.isEmpty()) return false;
		if(this.getAirAvailable(tube.compair.getTankType(), tube.compair.getPressure()) < 50) return false;

		int sourceSide = accessDir.ordinal();
		int[] sourceSlotAccess = getSlotAccess(source, sourceSide);

		if(sendOrder == SEND_LAST) BobMathUtil.reverseIntArray(sourceSlotAccess);
		if(sendOrder == SEND_RANDOM) BobMathUtil.shuffleIntArray(sourceSlotAccess);

		ISidedInventory sidedSource = source instanceof ISidedInventory ? (ISidedInventory) source : null;
		boolean hasItem = false;

		for(int i : sourceSlotAccess) {
			ItemStack stack = source.getStackInSlot(i);
			if(stack != null) {
				if(sidedSource != null && !sidedSource.canExtractItem(i, stack, sourceSide)) continue;
				boolean match = tube.matchesFilter(stack);
				if((match && !tube.whitelist) || (!match && tube.whitelist)) continue;
				hasItem = true;
				break;
			}
		}

		// return early if there arent any items in the source inventory, saves on some cpu usage for idle networks
		if(!hasItem) return false;

		// for round robin, receivers are ordered by proximity to the source
		ReceiverComparator comparator = new ReceiverComparator(tube);
		List<Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>>> receiverList = new ArrayList(receivers.size());
		receiverList.addAll(receivers.entrySet());

		if(receiveOrder == RECEIVE_ROBIN || receiveOrder == RECEIVE_EVEN) receiverList.sort(comparator);
		if(receiveOrder == RECEIVE_RANDOM) Collections.shuffle(receiverList);

		TileEntity tile1 = source instanceof TileEntity ? (TileEntity) source : null;
		int attempts = 0;
		int maxAttempts = receiverList.size();
		int itemsLeftToSend = receiveOrder == RECEIVE_ROBIN ? MathHelper.clamp_int(roundRobinAmount, 0, ITEMS_PER_TRANSFER) : ITEMS_PER_TRANSFER;
		boolean didSomething = false;
		LinkedHashSet<IInventory> dirtyInventories = new LinkedHashSet();

		// try all receivers for both modes, in an attempts based system.
		// instead of bailing out of trying after the first failure (which means you have to wait 0.25 seconds), we just try the next one.
		while(attempts < maxAttempts) {
			int index = (receiveOrder == RECEIVE_RANDOM) ? attempts : (nextReceiver + attempts) % receiverList.size();

			Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>> candidate = receiverList.get(index);

			if(NodeNet.isBadLink(candidate.getKey())) {
				receivers.remove(candidate.getKey());
				attempts++;
				continue;
			}

			IInventory dest = candidate.getKey();
			TileEntityPneumoTube endpointTile = candidate.getValue().getZ();
			TileEntity tile2 = dest instanceof TileEntity ? (TileEntity) dest : null;

			if(endpointTile != null) {
				PneumaticChannel sourceChannel = PneumaticChannel.fromId(tube.sendChannel);
				PneumaticChannel targetChannel = PneumaticChannel.fromId(endpointTile.receiveChannel);
				if(sourceChannel != targetChannel) {
					attempts++;
					continue;
				}
			}

			// range check for our compression level, skip if either source or dest aren't tile entities
			if(tile1 != null && tile2 != null) {
				int sq = (tile1.xCoord - tile2.xCoord) * (tile1.xCoord - tile2.xCoord) + (tile1.yCoord - tile2.yCoord) * (tile1.yCoord - tile2.yCoord) + (tile1.zCoord - tile2.zCoord) * (tile1.zCoord - tile2.zCoord);
				if(sq > maxRange * maxRange) {
					attempts++;
					continue;
				}
			}

			ISidedInventory sidedDest = dest instanceof ISidedInventory ? (ISidedInventory) dest : null;
			int destSide = candidate.getValue().getX().getOpposite().ordinal();
			int[] destSlotAccess = getSlotAccess(dest, destSide);
			int itemHardCap = dest instanceof TileEntityMachineAutocrafter ? 1 : ITEMS_PER_TRANSFER;
			if(receiveOrder == RECEIVE_EVEN) {
				int transferableItems = countTransferableItems(source, tube, sourceSlotAccess, sidedSource, sourceSide, itemsLeftToSend);
				int remainingReceivers = countRemainingAcceptingReceivers(receiverList, attempts, nextReceiver, source, tube, sourceSlotAccess, sidedSource, sourceSide, tile1, maxRange, itemsLeftToSend);
				if(transferableItems > 0 && remainingReceivers > 0) {
					itemHardCap = Math.max(1, (int) Math.ceil((double) transferableItems / (double) remainingReceivers));
				}
			}
			ReceiverTransferResult result = tryTransferToReceiver(source, dest, tube, endpointTile, sourceSlotAccess, destSlotAccess, sidedSource, sidedDest, sourceSide, destSide, itemHardCap, itemsLeftToSend);
			itemsLeftToSend = result.itemsLeftToSend;
			if(result.didSomething) {
				didSomething = true;
				dirtyInventories.add(dest);
			}

			// make sure both parties are saved to disk and increment the counter for round robin
			if(didSomething && (receiveOrder == RECEIVE_ROBIN || receiveOrder != RECEIVE_EVEN || itemsLeftToSend <= 0)) {
				dirtyInventories.add(source);
				for(IInventory inventory : dirtyInventories) inventory.markDirty();
				this.useAir(tube.compair.getTankType(), tube.compair.getPressure(), 50);
				return true;
			}

			attempts++;
		}

		if(didSomething) {
			dirtyInventories.add(source);
			for(IInventory inventory : dirtyInventories) inventory.markDirty();
			this.useAir(tube.compair.getTankType(), tube.compair.getPressure(), 50);
			return true;
		}

		return false;
	}

	protected ReceiverTransferResult tryTransferToReceiver(IInventory source, IInventory dest, TileEntityPneumoTube tube, TileEntityPneumoTube endpointTile, int[] sourceSlotAccess, int[] destSlotAccess, ISidedInventory sidedSource, ISidedInventory sidedDest, int sourceSide, int destSide, int itemHardCap, int itemsLeftToSend) {
		boolean didSomething = false;
		int receiverCapLeft = itemHardCap;

		for(int sourceIndex : sourceSlotAccess) {
			if(receiverCapLeft <= 0) break;
			ItemStack sourceStack = source.getStackInSlot(sourceIndex);
			if(sourceStack == null) continue;
			if(sidedSource != null && !sidedSource.canExtractItem(sourceIndex, sourceStack, sourceSide)) continue;

			boolean match = tube.matchesFilter(sourceStack);
			if((match && !tube.whitelist) || (!match && tube.whitelist)) continue;

			if(endpointTile != null && endpointTile != tube) {
				match = endpointTile.matchesFilter(sourceStack);
				if((match && !endpointTile.whitelist) || (!match && endpointTile.whitelist)) continue;
			}

			int proportionalValue = MathHelper.clamp_int(64 / sourceStack.getMaxStackSize(), 1, 64);

			for(int destIndex : destSlotAccess) {
				if(receiverCapLeft <= 0) break;
				ItemStack destStack = dest.getStackInSlot(destIndex);
				if(destStack == null) continue;
				if(!ItemStackUtil.areStacksCompatible(sourceStack, destStack)) continue;
				int toMove = BobMathUtil.min(sourceStack.stackSize, destStack.getMaxStackSize() - destStack.stackSize, dest.getInventoryStackLimit() - destStack.stackSize, itemsLeftToSend / proportionalValue, receiverCapLeft);
				if(toMove <= 0) continue;

				ItemStack checkStack = destStack.copy();
				checkStack.stackSize += toMove;
				if(!dest.isItemValidForSlot(destIndex, checkStack)) continue;
				if(sidedDest != null && !sidedDest.canInsertItem(destIndex, checkStack, destSide)) continue;

				sourceStack.stackSize -= toMove;
				if(sourceStack.stackSize <= 0) source.setInventorySlotContents(sourceIndex, null);
				destStack.stackSize += toMove;
				itemsLeftToSend -= toMove * proportionalValue;
				receiverCapLeft -= toMove;
				didSomething = true;
				if(itemsLeftToSend <= 0) return new ReceiverTransferResult(true, 0);
			}

			if(itemsLeftToSend > 0 && receiverCapLeft > 0 && sourceStack.stackSize > 0) for(int destIndex : destSlotAccess) {
				if(dest.getStackInSlot(destIndex) != null) continue;
				int toMove = BobMathUtil.min(sourceStack.stackSize, dest.getInventoryStackLimit(), itemsLeftToSend / proportionalValue, receiverCapLeft);
				if(toMove <= 0) continue;

				ItemStack checkStack = sourceStack.copy();
				checkStack.stackSize = toMove;
				if(!dest.isItemValidForSlot(destIndex, checkStack)) continue;
				if(sidedDest != null && !sidedDest.canInsertItem(destIndex, checkStack, destSide)) continue;

				ItemStack newStack = sourceStack.copy();
				newStack.stackSize = toMove;
				sourceStack.stackSize -= toMove;
				if(sourceStack.stackSize <= 0) source.setInventorySlotContents(sourceIndex, null);
				dest.setInventorySlotContents(destIndex, newStack);
				itemsLeftToSend -= toMove * proportionalValue;
				receiverCapLeft -= toMove;
				didSomething = true;
				if(itemsLeftToSend <= 0) return new ReceiverTransferResult(true, 0);
			}
		}

		return new ReceiverTransferResult(didSomething, itemsLeftToSend);
	}

	protected int countTransferableItems(IInventory source, TileEntityPneumoTube tube, int[] sourceSlotAccess, ISidedInventory sidedSource, int sourceSide, int itemsLeftToSend) {
		int transferable = 0;

		for(int sourceIndex : sourceSlotAccess) {
			if(itemsLeftToSend <= 0) break;
			ItemStack sourceStack = source.getStackInSlot(sourceIndex);
			if(sourceStack == null) continue;
			if(sidedSource != null && !sidedSource.canExtractItem(sourceIndex, sourceStack, sourceSide)) continue;

			boolean match = tube.matchesFilter(sourceStack);
			if((match && !tube.whitelist) || (!match && tube.whitelist)) continue;

			int proportionalValue = MathHelper.clamp_int(64 / sourceStack.getMaxStackSize(), 1, 64);
			int canMove = Math.min(sourceStack.stackSize, itemsLeftToSend / proportionalValue);
			if(canMove <= 0) continue;

			transferable += canMove;
			itemsLeftToSend -= canMove * proportionalValue;
		}

		return transferable;
	}

	protected int countRemainingAcceptingReceivers(List<Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>>> receiverList, int attempts, int nextReceiver, IInventory source, TileEntityPneumoTube tube, int[] sourceSlotAccess, ISidedInventory sidedSource, int sourceSide, TileEntity sourceTile, int maxRange, int itemsLeftToSend) {
		int receivers = 0;

		for(int offset = attempts; offset < receiverList.size(); offset++) {
			int index = (nextReceiver + offset) % receiverList.size();
			Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>> candidate = receiverList.get(index);
			if(canReceiverAcceptAny(candidate, source, tube, sourceSlotAccess, sidedSource, sourceSide, sourceTile, maxRange, itemsLeftToSend)) {
				receivers++;
			}
		}

		return receivers;
	}

	protected boolean canReceiverAcceptAny(Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>> candidate, IInventory source, TileEntityPneumoTube tube, int[] sourceSlotAccess, ISidedInventory sidedSource, int sourceSide, TileEntity sourceTile, int maxRange, int itemsLeftToSend) {
		if(NodeNet.isBadLink(candidate.getKey())) return false;

		IInventory dest = candidate.getKey();
		TileEntityPneumoTube endpointTile = candidate.getValue().getZ();
		TileEntity destTile = dest instanceof TileEntity ? (TileEntity) dest : null;

		if(endpointTile != null) {
			PneumaticChannel sourceChannel = PneumaticChannel.fromId(tube.sendChannel);
			PneumaticChannel targetChannel = PneumaticChannel.fromId(endpointTile.receiveChannel);
			if(sourceChannel != targetChannel) return false;
		}

		if(sourceTile != null && destTile != null) {
			int sq = (sourceTile.xCoord - destTile.xCoord) * (sourceTile.xCoord - destTile.xCoord) + (sourceTile.yCoord - destTile.yCoord) * (sourceTile.yCoord - destTile.yCoord) + (sourceTile.zCoord - destTile.zCoord) * (sourceTile.zCoord - destTile.zCoord);
			if(sq > maxRange * maxRange) return false;
		}

		ISidedInventory sidedDest = dest instanceof ISidedInventory ? (ISidedInventory) dest : null;
		int destSide = candidate.getValue().getX().getOpposite().ordinal();
		int[] destSlotAccess = getSlotAccess(dest, destSide);

		for(int sourceIndex : sourceSlotAccess) {
			if(itemsLeftToSend <= 0) break;
			ItemStack sourceStack = source.getStackInSlot(sourceIndex);
			if(sourceStack == null) continue;
			if(sidedSource != null && !sidedSource.canExtractItem(sourceIndex, sourceStack, sourceSide)) continue;

			boolean match = tube.matchesFilter(sourceStack);
			if((match && !tube.whitelist) || (!match && tube.whitelist)) continue;

			if(endpointTile != null && endpointTile != tube) {
				match = endpointTile.matchesFilter(sourceStack);
				if((match && !endpointTile.whitelist) || (!match && endpointTile.whitelist)) continue;
			}

			int proportionalValue = MathHelper.clamp_int(64 / sourceStack.getMaxStackSize(), 1, 64);
			if(itemsLeftToSend / proportionalValue <= 0) continue;

			for(int destIndex : destSlotAccess) {
				ItemStack destStack = dest.getStackInSlot(destIndex);

				if(destStack != null) {
					if(!ItemStackUtil.areStacksCompatible(sourceStack, destStack)) continue;
					if(destStack.stackSize >= destStack.getMaxStackSize()) continue;

					ItemStack checkStack = destStack.copy();
					checkStack.stackSize += 1;
					if(!dest.isItemValidForSlot(destIndex, checkStack)) continue;
					if(sidedDest != null && !sidedDest.canInsertItem(destIndex, checkStack, destSide)) continue;
					return true;
				}

				ItemStack checkStack = sourceStack.copy();
				checkStack.stackSize = 1;
				if(!dest.isItemValidForSlot(destIndex, checkStack)) continue;
				if(sidedDest != null && !sidedDest.canInsertItem(destIndex, checkStack, destSide)) continue;
				return true;
			}
		}

		return false;
	}

	protected int countPotentialReceivers(List<Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>>> receiverList, IInventory source, TileEntityPneumoTube tube, int[] sourceSlotAccess, ISidedInventory sidedSource, int sourceSide, TileEntity sourceTile, int maxRange) {
		int receivers = 0;

		for(Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>> candidate : receiverList) {
			if(NodeNet.isBadLink(candidate.getKey())) continue;

			IInventory dest = candidate.getKey();
			TileEntityPneumoTube endpointTile = candidate.getValue().getZ();
			TileEntity destTile = dest instanceof TileEntity ? (TileEntity) dest : null;

			if(endpointTile != null) {
				PneumaticChannel sourceChannel = PneumaticChannel.fromId(tube.sendChannel);
				PneumaticChannel targetChannel = PneumaticChannel.fromId(endpointTile.receiveChannel);
				if(sourceChannel != targetChannel) continue;
			}

			if(sourceTile != null && destTile != null) {
				int sq = (sourceTile.xCoord - destTile.xCoord) * (sourceTile.xCoord - destTile.xCoord) + (sourceTile.yCoord - destTile.yCoord) * (sourceTile.yCoord - destTile.yCoord) + (sourceTile.zCoord - destTile.zCoord) * (sourceTile.zCoord - destTile.zCoord);
				if(sq > maxRange * maxRange) continue;
			}

			for(int sourceIndex : sourceSlotAccess) {
				ItemStack sourceStack = source.getStackInSlot(sourceIndex);
				if(sourceStack == null) continue;
				if(sidedSource != null && !sidedSource.canExtractItem(sourceIndex, sourceStack, sourceSide)) continue;

				boolean match = tube.matchesFilter(sourceStack);
				if((match && !tube.whitelist) || (!match && tube.whitelist)) continue;

				if(endpointTile != null && endpointTile != tube) {
					match = endpointTile.matchesFilter(sourceStack);
					if((match && !endpointTile.whitelist) || (!match && endpointTile.whitelist)) continue;
				}

				receivers++;
				break;
			}
		}

		return receivers;
	}

	protected static class ReceiverTransferResult {
		public final boolean didSomething;
		public final int itemsLeftToSend;

		public ReceiverTransferResult(boolean didSomething, int itemsLeftToSend) {
			this.didSomething = didSomething;
			this.itemsLeftToSend = itemsLeftToSend;
		}
	}

	/** Returns an array of accessible slots from the given side of an IInventory. If it's an ISidedInventory, uses the sided restrictions instead. */
	public static int[] getSlotAccess(IInventory inventory, int dir) {

		if(inventory instanceof ISidedInventory) {
			int[] slotAccess = ((ISidedInventory) inventory).getAccessibleSlotsFromSide(dir);
			return Arrays.copyOf(slotAccess, slotAccess.length); //we mess with the order, so better not use the original array
		} else {
			int[] slotAccess = new int[inventory.getSizeInventory()];
			for(int i = 0; i < inventory.getSizeInventory(); i++) slotAccess[i] = i;
			return slotAccess;
		}
	}

	/** Compares IInventory by distance, going off the assumption that they are TileEntities. Uses positional data for tie-breaking if the distance is the same. */
	public static class ReceiverComparator implements Comparator<Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>>> {

		private TileEntityPneumoTube origin;

		public ReceiverComparator(TileEntityPneumoTube origin) {
			this.origin = origin;
		}

		@Override
		public int compare(Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>> o1, Entry<IInventory, Triplet<ForgeDirection, Long, TileEntityPneumoTube>> o2) {

			TileEntity tile1 = o1.getKey() instanceof TileEntity ? (TileEntity) o1.getKey() : null;
			TileEntity tile2 = o2.getKey() instanceof TileEntity ? (TileEntity) o2.getKey() : null;

			// prioritize actual TileEntities
			if(tile1 == null && tile2 != null) return 1;
			if(tile1 != null && tile2 == null) return -1;
			if(tile1 == null && tile2 == null) return 0;

			// calculate distances from origin
			int dist1 = (tile1.xCoord - origin.xCoord) * (tile1.xCoord - origin.xCoord) + (tile1.yCoord - origin.yCoord) * (tile1.yCoord - origin.yCoord) + (tile1.zCoord - origin.zCoord) * (tile1.zCoord - origin.zCoord);
			int dist2 = (tile2.xCoord - origin.xCoord) * (tile2.xCoord - origin.xCoord) + (tile2.yCoord - origin.yCoord) * (tile2.yCoord - origin.yCoord) + (tile2.zCoord - origin.zCoord) * (tile2.zCoord - origin.zCoord);

			// tier-breaker: use hash value instead
			if(dist1 == dist2) {
				return TileEntityPneumoTube.getIdentifier(tile1.xCoord, tile1.yCoord, tile1.zCoord) - TileEntityPneumoTube.getIdentifier(tile2.xCoord, tile2.yCoord, tile2.zCoord);
			}
			
			// no tie? return difference of the distances
			return dist1 - dist2;
		}
	}

	public static class AirKey {

		public final FluidType type;
		public final int pressure;

		public AirKey(FluidType type, int pressure) {
			this.type = type;
			this.pressure = pressure;
		}

		public static AirKey fromTube(TileEntityPneumoTube tube) {
			return new AirKey(tube.compair.getTankType(), tube.compair.getPressure());
		}

		public boolean matches(TileEntityPneumoTube tube) {
			return tube.compair.getTankType() == this.type && tube.compair.getPressure() == this.pressure;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj) return true;
			if(!(obj instanceof AirKey)) return false;
			AirKey other = (AirKey) obj;
			return this.type == other.type && this.pressure == other.pressure;
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, pressure);
		}
	}
}
