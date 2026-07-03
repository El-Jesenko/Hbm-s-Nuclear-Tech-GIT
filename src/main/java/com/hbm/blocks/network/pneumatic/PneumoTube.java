package com.hbm.blocks.network.pneumatic;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.Library;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import com.hbm.util.Compat;

import api.hbm.block.IToolable;
import api.hbm.ntl.IPneumaticConnector;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

public class PneumoTube extends BlockContainer implements IToolable, ITooltipProvider {

	@SideOnly(Side.CLIENT) public IIcon baseIcon;
	@SideOnly(Side.CLIENT) public IIcon iconIn;
	@SideOnly(Side.CLIENT) public IIcon iconOut;
	@SideOnly(Side.CLIENT) public IIcon iconOutGreen;
	@SideOnly(Side.CLIENT) public IIcon iconOutRed;
	@SideOnly(Side.CLIENT) public IIcon iconConnector;
	@SideOnly(Side.CLIENT) public IIcon iconStraight;
	@SideOnly(Side.CLIENT) public IIcon activeIcon;

	public static final int HIT_CENTER = 0;
	public static final int HIT_INSERTION = 1;
	public static final int HIT_EJECTION = 2;

	public boolean[] renderSides = new boolean[] {true, true, true, true, true, true};

	public PneumoTube() {
		super(Material.iron);
	}

	public static int renderID = RenderingRegistry.getNextAvailableRenderId();

	@Override public int getRenderType() { return renderID; }
	@Override public boolean isOpaqueCube() { return false; }
	@Override public boolean renderAsNormalBlock() { return false; }

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityPneumoTube();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		super.registerBlockIcons(reg);

		iconIn = reg.registerIcon(RefStrings.MODID + ":pneumatic_tube_in");
		iconOutGreen = reg.registerIcon(RefStrings.MODID + ":pneumatic_tube_out_green");
		iconOut = iconOutGreen;
		iconOutRed = reg.registerIcon(RefStrings.MODID + ":pneumatic_tube_out_red");
		iconConnector = reg.registerIcon(RefStrings.MODID + ":pneumatic_tube_connector");
		iconStraight = reg.registerIcon(RefStrings.MODID + ":pneumatic_tube_straight");

		this.activeIcon = this.baseIcon = this.blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return this.activeIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
		return renderSides[side % 6];
	}

	public void resetRenderSides() {
		for(int i = 0; i < 6; i++) renderSides[i] = true;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(player.getHeldItem() != null && ToolType.getType(player.getHeldItem()) == ToolType.SCREWDRIVER) return false;
		if(!player.isSneaking()) {
			TileEntity tile = world.getTileEntity(x, y, z);
			if(tile instanceof TileEntityPneumoTube) {
				TileEntityPneumoTube tube = (TileEntityPneumoTube) tile;
				int hitPart = resolveHitPart(world, x, y, z, player, tube, side, hitX, hitY, hitZ);
				boolean clickedInsertion = hitPart == HIT_INSERTION && tube.isCompressor();
				boolean clickedEjection = hitPart == HIT_EJECTION && tube.isEndpoint();
				boolean preferInsertion = clickedInsertion || (hitPart == HIT_CENTER && tube.isCompressor() && !tube.isEndpoint());

				if(preferInsertion) {
					if(player.getHeldItem() != null && player.getHeldItem().getItem() instanceof IItemFluidIdentifier) {
						if(!world.isRemote) {
							FluidType type = ((IItemFluidIdentifier) player.getHeldItem().getItem()).getType(world, x, y, z, player.getHeldItem());
							boolean canUse = !type.hasTrait(FT_Amat.class) && !type.hasTrait(FT_Corrosive.class) && (type.hasTrait(FT_Gaseous.class) || type.hasTrait(FT_Gaseous_ART.class));

							if(canUse) {
								tube.compair.setTankType(type);
								tube.markDirty();
								player.addChatComponentMessage(new ChatComponentText("Changed type to ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)).appendSibling(new ChatComponentTranslation(type.getConditionalName())).appendSibling(new ChatComponentText("!")));
							} else {
								player.addChatComponentMessage(new ChatComponentText("Invalid gas!").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
							}
						}
					} else {
						FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, x, y, z);
					}

					return true;
				} else if(clickedEjection || tube.isEndpoint()) {
					FMLNetworkHandler.openGui(player, MainRegistry.instance, 1, world, x, y, z);
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY, float fZ, ToolType tool) {
		if(tool != ToolType.SCREWDRIVER) return false;
		if(world.isRemote) return true;

		TileEntityPneumoTube tube = (TileEntityPneumoTube) world.getTileEntity(x, y, z);

		boolean targetEjection = player.isSneaking();
		int hitPart = resolveHitPart(world, x, y, z, player, tube, side, fX, fY, fZ);
		if(hitPart == HIT_INSERTION && tube.isCompressor()) targetEjection = false;
		if(hitPart == HIT_EJECTION && tube.isEndpoint()) targetEjection = true;

		ForgeDirection rot = targetEjection ? tube.ejectionDir : tube.insertionDir;
		ForgeDirection oth = targetEjection ? tube.insertionDir : tube.ejectionDir;

		for(int i = 0; i < 7; i++) {
			rot = ForgeDirection.getOrientation((rot.ordinal() + 1) % 7);
			if(rot == ForgeDirection.UNKNOWN) break; //unknown is always valid, simply disables this part
			if(rot == oth) continue; //skip if both positions collide
			TileEntity tile = Compat.getTileStandard(world, x + rot.offsetX, y + rot.offsetY, z + rot.offsetZ);
			if(tile instanceof TileEntityPneumoTube) continue;
			if(tile instanceof IInventory) break; //valid if connected to an IInventory
		}

		if(targetEjection) tube.ejectionDir = rot; else tube.insertionDir = rot;

		tube.markDirty();
		if(world instanceof WorldServer) ((WorldServer) world).getPlayerManager().markBlockForUpdate(x, y, z);

		return true;
	}

	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB entityBounding, List list, Entity entity) {

		for(HitBoxEntry entry : getHitBoxes(world, x, y, z)) {
			if(entityBounding.intersectsWith(entry.box)) {
				list.add(entry.box);
			}
		}
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return AxisAlignedBB.getBoundingBox(x + this.minX, y + this.minY, z + this.minZ, x + this.maxX, y + this.maxY, z + this.maxZ);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return AxisAlignedBB.getBoundingBox(x + this.minX, y + this.minY, z + this.minZ, x + this.maxX, y + this.maxY, z + this.maxZ);
	}

	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 startVec, Vec3 endVec) {

		MovingObjectPosition bestHit = null;
		double bestDistance = Double.MAX_VALUE;

		for(HitBoxEntry entry : getHitBoxes(world, x, y, z)) {
			MovingObjectPosition hit = entry.box.calculateIntercept(startVec, endVec);
			if(hit == null) continue;

			double distance = startVec.squareDistanceTo(hit.hitVec);
			if(distance < bestDistance) {
				bestDistance = distance;
				bestHit = new MovingObjectPosition(x, y, z, hit.sideHit, hit.hitVec);
				bestHit.subHit = entry.part;
			}
		}

		return bestHit;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {

		float lower = 0.375F;
		float upper = 0.625F;

		TileEntity te = world.getTileEntity(x, y, z);

		boolean nX = canConnectTo(world, x, y, z, Library.NEG_X) || canConnectToAir(world, x, y, z, Library.NEG_X);
		boolean pX = canConnectTo(world, x, y, z, Library.POS_X) || canConnectToAir(world, x, y, z, Library.POS_X);
		boolean nY = canConnectTo(world, x, y, z, Library.NEG_Y) || canConnectToAir(world, x, y, z, Library.NEG_Y);
		boolean pY = canConnectTo(world, x, y, z, Library.POS_Y) || canConnectToAir(world, x, y, z, Library.POS_Y);
		boolean nZ = canConnectTo(world, x, y, z, Library.NEG_Z) || canConnectToAir(world, x, y, z, Library.NEG_Z);
		boolean pZ = canConnectTo(world, x, y, z, Library.POS_Z) || canConnectToAir(world, x, y, z, Library.POS_Z);

		if(te instanceof TileEntityPneumoTube) {
			TileEntityPneumoTube tube = (TileEntityPneumoTube) te;
			nX |= tube.insertionDir == Library.NEG_X || tube.ejectionDir == Library.NEG_X;
			pX |= tube.insertionDir == Library.POS_X || tube.ejectionDir == Library.POS_X;
			nY |= tube.insertionDir == Library.NEG_Y || tube.ejectionDir == Library.NEG_Y;
			pY |= tube.insertionDir == Library.POS_Y || tube.ejectionDir == Library.POS_Y;
			nZ |= tube.insertionDir == Library.NEG_Z || tube.ejectionDir == Library.NEG_Z;
			pZ |= tube.insertionDir == Library.POS_Z || tube.ejectionDir == Library.POS_Z;
		}

		this.setBlockBounds(
				nX ? 0F : lower,
				nY ? 0F : lower,
				nZ ? 0F : lower,
				pX ? 1F : upper,
				pY ? 1F : upper,
				pZ ? 1F : upper);
	}

	protected List<HitBoxEntry> getHitBoxes(IBlockAccess world, int x, int y, int z) {

		List<HitBoxEntry> bbs = new ArrayList();
		double lower = 0.375D;
		double upper = 0.625D;
		double extLower = 0.25D;
		double extUpper = 0.75D;

		bbs.add(new HitBoxEntry(AxisAlignedBB.getBoundingBox(x + lower, y + lower, z + lower, x + upper, y + upper, z + upper), HIT_CENTER));

		if(canConnectTo(world, x, y, z, Library.POS_X) || canConnectToAir(world, x, y, z, Library.POS_X)) bbs.add(new HitBoxEntry(AxisAlignedBB.getBoundingBox(x + upper, y + lower, z + lower, x + 1, y + upper, z + upper), HIT_CENTER));
		if(canConnectTo(world, x, y, z, Library.NEG_X) || canConnectToAir(world, x, y, z, Library.NEG_X)) bbs.add(new HitBoxEntry(AxisAlignedBB.getBoundingBox(x, y + lower, z + lower, x + lower, y + upper, z + upper), HIT_CENTER));
		if(canConnectTo(world, x, y, z, Library.POS_Y) || canConnectToAir(world, x, y, z, Library.POS_Y)) bbs.add(new HitBoxEntry(AxisAlignedBB.getBoundingBox(x + lower, y + upper, z + lower, x + upper, y + 1, z + upper), HIT_CENTER));
		if(canConnectTo(world, x, y, z, Library.NEG_Y) || canConnectToAir(world, x, y, z, Library.NEG_Y)) bbs.add(new HitBoxEntry(AxisAlignedBB.getBoundingBox(x + lower, y, z + lower, x + upper, y + lower, z + upper), HIT_CENTER));
		if(canConnectTo(world, x, y, z, Library.POS_Z) || canConnectToAir(world, x, y, z, Library.POS_Z)) bbs.add(new HitBoxEntry(AxisAlignedBB.getBoundingBox(x + lower, y + lower, z + upper, x + upper, y + upper, z + 1), HIT_CENTER));
		if(canConnectTo(world, x, y, z, Library.NEG_Z) || canConnectToAir(world, x, y, z, Library.NEG_Z)) bbs.add(new HitBoxEntry(AxisAlignedBB.getBoundingBox(x + lower, y + lower, z, x + upper, y + upper, z + lower), HIT_CENTER));

		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntityPneumoTube) {
			TileEntityPneumoTube tube = (TileEntityPneumoTube) te;
			if(tube.insertionDir != ForgeDirection.UNKNOWN) bbs.add(new HitBoxEntry(getPortBox(x, y, z, tube.insertionDir, extLower, extUpper), HIT_INSERTION));
			if(tube.ejectionDir != ForgeDirection.UNKNOWN) bbs.add(new HitBoxEntry(getPortBox(x, y, z, tube.ejectionDir, extLower, extUpper), HIT_EJECTION));
		}

		return bbs;
	}

	protected AxisAlignedBB getPortBox(int x, int y, int z, ForgeDirection dir, double extLower, double extUpper) {
		if(dir == Library.POS_X) return AxisAlignedBB.getBoundingBox(x + extUpper, y + extLower, z + extLower, x + 1, y + extUpper, z + extUpper);
		if(dir == Library.NEG_X) return AxisAlignedBB.getBoundingBox(x, y + extLower, z + extLower, x + extLower, y + extUpper, z + extUpper);
		if(dir == Library.POS_Y) return AxisAlignedBB.getBoundingBox(x + extLower, y + extUpper, z + extLower, x + extUpper, y + 1, z + extUpper);
		if(dir == Library.NEG_Y) return AxisAlignedBB.getBoundingBox(x + extLower, y, z + extLower, x + extUpper, y + extLower, z + extUpper);
		if(dir == Library.POS_Z) return AxisAlignedBB.getBoundingBox(x + extLower, y + extLower, z + extUpper, x + extUpper, y + extUpper, z + 1);
		if(dir == Library.NEG_Z) return AxisAlignedBB.getBoundingBox(x + extLower, y + extLower, z, x + extUpper, y + extUpper, z + extLower);
		return AxisAlignedBB.getBoundingBox(x + 0.375D, y + 0.375D, z + 0.375D, x + 0.625D, y + 0.625D, z + 0.625D);
	}

	protected int resolveHitPart(World world, int x, int y, int z, EntityPlayer player, TileEntityPneumoTube tube, int side, float hitX, float hitY, float hitZ) {
		MovingObjectPosition mop = Library.rayTrace(player, 6D, 1F, false, true, false);
		if(mop != null && mop.blockX == x && mop.blockY == y && mop.blockZ == z) {
			if(mop.subHit == HIT_INSERTION && tube.isCompressor()) return HIT_INSERTION;
			if(mop.subHit == HIT_EJECTION && tube.isEndpoint()) return HIT_EJECTION;
			if(mop.subHit == HIT_CENTER) return HIT_CENTER;
		}

		return getHitPart(tube, side, hitX, hitY, hitZ);
	}

	protected int getHitPart(TileEntityPneumoTube tube, int side, float hitX, float hitY, float hitZ) {
		ForgeDirection face = ForgeDirection.getOrientation(side);
		if(face == tube.insertionDir && isPortArea(face, hitX, hitY, hitZ)) return HIT_INSERTION;
		if(face == tube.ejectionDir && isPortArea(face, hitX, hitY, hitZ)) return HIT_EJECTION;
		return HIT_CENTER;
	}

	protected boolean isPortArea(ForgeDirection face, float hitX, float hitY, float hitZ) {
		float min = 0.25F;
		float max = 0.75F;

		if(face == Library.POS_X || face == Library.NEG_X) return hitY >= min && hitY <= max && hitZ >= min && hitZ <= max;
		if(face == Library.POS_Y || face == Library.NEG_Y) return hitX >= min && hitX <= max && hitZ >= min && hitZ <= max;
		if(face == Library.POS_Z || face == Library.NEG_Z) return hitX >= min && hitX <= max && hitY >= min && hitY <= max;
		return false;
	}

	protected static class HitBoxEntry {
		public final AxisAlignedBB box;
		public final int part;

		protected HitBoxEntry(AxisAlignedBB box, int part) {
			this.box = box;
			this.part = part;
		}
	}

	public boolean canConnectTo(IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
		TileEntity tile = world instanceof World ? Compat.getTileStandard((World) world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ) : world.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
		if(tile instanceof TileEntityPneumoTube) return true;
		if(tile instanceof IPneumaticConnector) return ((IPneumaticConnector) tile).canConnectPneumatic(dir.getOpposite());
		return false;
	}

	public boolean canConnectToAir(IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
		FluidType air = Fluids.AIR;
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntityPneumoTube) {
			TileEntityPneumoTube tube = (TileEntityPneumoTube) te;
			if(!tube.isCompressor()) return false;
			if(tube.ejectionDir == dir || tube.insertionDir == dir) return false;
			air = tube.compair.getTankType();
		}
		TileEntity tile = world.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
		if(tile instanceof TileEntityPneumoTube) return false;
		return Library.canConnectFluid(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, dir, air);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		addStandardInfo(stack, player, list, ext);
	}
}
