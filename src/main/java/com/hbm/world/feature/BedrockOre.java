package com.hbm.world.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm.blocks.BlockEnums;
import com.hbm.blocks.BlockEnums.EnumStoneType;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBedrockOreTE.TileEntityBedrockOre;
import com.hbm.config.GeneralConfig;
import com.hbm.config.WorldConfig;
import com.hbm.inventory.FluidStack;
import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBedrockOre.EnumBedrockOre;
import com.hbm.util.WeightedRandomGeneric;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.SolarSystem.Body;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class BedrockOre {

	public static List<WeightedRandomGeneric<BedrockOreDefinition>> weightedOres = new ArrayList<>();
	public static List<WeightedRandomGeneric<BedrockOreDefinition>> weightedOresNether = new ArrayList<>();
	public static Map<SolarSystem.Body, List<WeightedRandomGeneric<BedrockOreDefinition>>> weightedPlanetOres = new HashMap<>();

	public static HashMap<String, BedrockOreDefinition> replacements = new HashMap<>();

	public static void init() {

		BedrockOreDefinition ironrich = new BedrockOreDefinition(DictFrame.fromOne(ModBlocks.ore_ironrich, BlockEnums.EnumBedrockOreType.IRONRICH,2), 1,0xD8D8D8);
		BedrockOreDefinition copperrich = new BedrockOreDefinition(DictFrame.fromOne(ModBlocks.ore_copperrich, BlockEnums.EnumBedrockOreType.COPPERRICH,2),1, 0xA35D2B);
		BedrockOreDefinition carbonrich = new BedrockOreDefinition(DictFrame.fromOne(ModBlocks.ore_carbonrich, BlockEnums.EnumBedrockOreType.CARBONRICH ,2),1,0x141414);

		// NTMain bedrock ores
		BedrockOreDefinition iron = new BedrockOreDefinition(EnumBedrockOre.IRON,													1);
		BedrockOreDefinition copper = new BedrockOreDefinition(EnumBedrockOre.COPPER,												1);
		BedrockOreDefinition borax = new BedrockOreDefinition(EnumBedrockOre.BORAX,													3,	new FluidStack(Fluids.SULFURIC_ACID, 500));
		BedrockOreDefinition chlorocalcite = new BedrockOreDefinition(EnumBedrockOre.CHLOROCALCITE,											3,	new FluidStack(Fluids.SULFURIC_ACID, 500));
		BedrockOreDefinition asbestos = new BedrockOreDefinition(EnumBedrockOre.ASBESTOS,												2);
		BedrockOreDefinition niobium = new BedrockOreDefinition(EnumBedrockOre.NIOBIUM,												2,	new FluidStack(Fluids.PEROXIDE, 500));
		BedrockOreDefinition neodymium = new BedrockOreDefinition(EnumBedrockOre.NEODYMIUM,												3,	new FluidStack(Fluids.PEROXIDE, 500));
		BedrockOreDefinition titanium = new BedrockOreDefinition(EnumBedrockOre.TITANIUM,												2,	new FluidStack(Fluids.SULFURIC_ACID, 500));
		BedrockOreDefinition tungsten = new BedrockOreDefinition(EnumBedrockOre.TUNGSTEN,												2,	new FluidStack(Fluids.PEROXIDE, 500));
		BedrockOreDefinition gold = new BedrockOreDefinition(EnumBedrockOre.GOLD,													1);
		BedrockOreDefinition uranium = new BedrockOreDefinition(EnumBedrockOre.URANIUM,												4,	new FluidStack(Fluids.SULFURIC_ACID, 500));
		BedrockOreDefinition thorium = new BedrockOreDefinition(EnumBedrockOre.THORIUM,												4,	new FluidStack(Fluids.SULFURIC_ACID, 500));
		BedrockOreDefinition fluorite = new BedrockOreDefinition(EnumBedrockOre.FLUORITE,												1);
		BedrockOreDefinition coal = new BedrockOreDefinition(new ItemStack(Items.coal, 8),											1,	0x202020);
		BedrockOreDefinition niter = new BedrockOreDefinition(new ItemStack(ModItems.niter, 4),										2,	0x808080,	new FluidStack(Fluids.PEROXIDE, 500));
		BedrockOreDefinition redstone = new BedrockOreDefinition(new ItemStack(Items.redstone, 4),										1,	0xd01010);
		BedrockOreDefinition emerald = new BedrockOreDefinition(new ItemStack(Items.emerald, 4),										1,	0x3FDD85);
		BedrockOreDefinition rare = new BedrockOreDefinition(DictFrame.fromOne(ModItems.chunk_ore, EnumChunkType.RARE),			2,	0x8F9999,	new FluidStack(Fluids.PEROXIDE, 500));
		BedrockOreDefinition bauxite = new BedrockOreDefinition(DictFrame.fromOne(ModBlocks.stone_resource, EnumStoneType.BAUXITE, 2),1,	0xEF7213);

		BedrockOreDefinition glowstone = new BedrockOreDefinition(new ItemStack(Items.glowstone_dust, 4),		1,	0xF9FF4D);
		BedrockOreDefinition phosporous = new BedrockOreDefinition(new ItemStack(ModItems.powder_fire, 4),		1,	0xD7341F);
		BedrockOreDefinition quartz = new BedrockOreDefinition(new ItemStack(Items.quartz, 4),				1,	0xF0EFDD);

		// NTM Space Fork ores
		BedrockOreDefinition nickel = new BedrockOreDefinition(EnumBedrockOre.NICKEL,												2);
		BedrockOreDefinition cadmium = new BedrockOreDefinition(EnumBedrockOre.CAD, 3, new FluidStack(Fluids.SULFURIC_ACID, 500));
		BedrockOreDefinition zinc = new BedrockOreDefinition(EnumBedrockOre.ZINC,												2);
		BedrockOreDefinition lithium = new BedrockOreDefinition(new ItemStack(ModItems.powder_lithium, 4), 1, 0xFFFFFF);
		BedrockOreDefinition ice = new BedrockOreDefinition(new ItemStack(Blocks.packed_ice, 8), 1, 0xAAFFFF);
		BedrockOreDefinition coltan = new BedrockOreDefinition(new ItemStack(ModItems.fragment_coltan), 1, 0xA78D7A);
		BedrockOreDefinition lanthanium = new BedrockOreDefinition(new ItemStack(ModItems.powder_lanthanium), 2, 0xA1B9B9);
		BedrockOreDefinition schrabidium = new BedrockOreDefinition(new ItemStack(ModItems.powder_schrabidium, 1), 4, 0x00FFFF, new FluidStack(Fluids.NITRIC_ACID, 500));
		BedrockOreDefinition cinnabar = new BedrockOreDefinition(new ItemStack(ModItems.cinnebar, 4), 1, 0xFF0000);
		BedrockOreDefinition hematite = new BedrockOreDefinition(DictFrame.fromOne(ModBlocks.stone_resource, EnumStoneType.HEMATITE, 2), 1, 0xEF7213);



		registerBedrockOre(weightedOres, ironrich, 100);
		registerBedrockOre(weightedOres, copperrich, 100);
		registerBedrockOre(weightedOres, carbonrich, 100);

		// Earth ores de sand orsch
		//registerBedrockOre(weightedOres, iron, WorldConfig.bedrockIronSpawn);
		//registerBedrockOre(weightedOres, copper, WorldConfig.bedrockCopperSpawn);
		//registerBedrockOre(weightedOres, coal, WorldConfig.bedrockCoalSpawn);
		//registerBedrockOre(weightedOres, bauxite, WorldConfig.bedrockBauxiteSpawn);

		// Nether ores
		registerBedrockOre(weightedOresNether, glowstone, WorldConfig.bedrockGlowstoneSpawn);
		registerBedrockOre(weightedOresNether, phosporous, WorldConfig.bedrockPhosphorusSpawn);
		registerBedrockOre(weightedOresNether, quartz, WorldConfig.bedrockQuartzSpawn);

		// Moon ores
		registerBedrockOre(Body.MUN, lithium, 100);
		registerBedrockOre(Body.MUN, niter, WorldConfig.bedrockNiterSpawn);
		registerBedrockOre(Body.MUN, zinc, 100);
		registerBedrockOre(Body.MUN, ice, 100);
		registerBedrockOre(Body.MUN, iron, WorldConfig.bedrockIronSpawn);
		registerBedrockOre(Body.MUN, copper, WorldConfig.bedrockCopperSpawn);
		registerBedrockOre(Body.MUN, quartz, WorldConfig.bedrockQuartzSpawn);
		registerBedrockOre(Body.MUN, titanium, WorldConfig.bedrockTitaniumSpawn);

		// Minmus ores
		registerBedrockOre(Body.MINMUS, ice, 100);
		registerBedrockOre(Body.MINMUS, iron, WorldConfig.bedrockIronSpawn);
		registerBedrockOre(Body.MINMUS, copper, WorldConfig.bedrockCopperSpawn);
		registerBedrockOre(Body.MINMUS, neodymium, WorldConfig.bedrockNeodymiumSpawn);
		registerBedrockOre(Body.MINMUS, borax, WorldConfig.bedrockBoraxSpawn);
		registerBedrockOre(Body.MINMUS, tungsten, WorldConfig.bedrockTungstenSpawn);

		// Duna ores
		registerBedrockOre(Body.DUNA, ice, 100);
		registerBedrockOre(Body.DUNA, iron, WorldConfig.bedrockIronSpawn);
		registerBedrockOre(Body.DUNA, copper, WorldConfig.bedrockCopperSpawn);
		registerBedrockOre(Body.DUNA, neodymium, WorldConfig.bedrockNeodymiumSpawn);
		registerBedrockOre(Body.DUNA, thorium, WorldConfig.bedrockThoriumSpawn);
		registerBedrockOre(Body.DUNA, fluorite, WorldConfig.bedrockFluoriteSpawn);
		registerBedrockOre(Body.DUNA, niter, WorldConfig.bedrockNiterSpawn);
		registerBedrockOre(Body.DUNA, hematite, 100);

		// Ike ores
		registerBedrockOre(Body.IKE, iron, WorldConfig.bedrockIronSpawn);
		registerBedrockOre(Body.IKE, copper, WorldConfig.bedrockCopperSpawn);
		registerBedrockOre(Body.IKE, nickel, 100);
		registerBedrockOre(Body.IKE, zinc, 100);
		registerBedrockOre(Body.IKE, quartz, WorldConfig.bedrockQuartzSpawn);
		registerBedrockOre(Body.IKE, titanium, WorldConfig.bedrockTitaniumSpawn);
		registerBedrockOre(Body.IKE, redstone, WorldConfig.bedrockRedstoneSpawn);

		// Dres ores
		registerBedrockOre(Body.DRES, iron, WorldConfig.bedrockIronSpawn);
		registerBedrockOre(Body.DRES, copper, WorldConfig.bedrockCopperSpawn);
		registerBedrockOre(Body.DRES, cadmium, 100);
		registerBedrockOre(Body.DRES, coltan, 100);
		registerBedrockOre(Body.DRES, lanthanium, 50);
		registerBedrockOre(Body.DRES, gold, WorldConfig.bedrockGoldSpawn);
		registerBedrockOre(Body.DRES, redstone, WorldConfig.bedrockRedstoneSpawn);

		// Laythe ores
		registerBedrockOre(Body.LAYTHE, iron, WorldConfig.bedrockIronSpawn);
		registerBedrockOre(Body.LAYTHE, copper, WorldConfig.bedrockCopperSpawn);
		registerBedrockOre(Body.LAYTHE, rare, WorldConfig.bedrockRareEarthSpawn);
		registerBedrockOre(Body.LAYTHE, asbestos, WorldConfig.bedrockAsbestosSpawn);
		registerBedrockOre(Body.LAYTHE, emerald, WorldConfig.bedrockEmeraldSpawn);
		registerBedrockOre(Body.LAYTHE, uranium, WorldConfig.bedrockUraniumSpawn);

		// Moho ores
		registerBedrockOre(Body.MOHO, iron, WorldConfig.bedrockIronSpawn);
		registerBedrockOre(Body.MOHO, copper, WorldConfig.bedrockCopperSpawn);
		registerBedrockOre(Body.MOHO, glowstone, WorldConfig.bedrockGlowstoneSpawn);
		registerBedrockOre(Body.MOHO, phosporous, WorldConfig.bedrockPhosphorusSpawn);
		registerBedrockOre(Body.MOHO, chlorocalcite, WorldConfig.bedrockChlorocalciteSpawn);
		registerBedrockOre(Body.MOHO, cinnabar, 100);

		// Eve ores
		registerBedrockOre(Body.EVE, uranium, WorldConfig.bedrockUraniumSpawn);
		registerBedrockOre(Body.EVE, thorium, WorldConfig.bedrockThoriumSpawn);
		registerBedrockOre(Body.EVE, niobium, WorldConfig.bedrockNiobiumSpawn);
		registerBedrockOre(Body.EVE, rare, WorldConfig.bedrockRareEarthSpawn);
		registerBedrockOre(Body.EVE, schrabidium, 100);
		registerBedrockOre(Body.EVE, cinnabar, 100);
	}

	public static void registerBedrockOre(List<WeightedRandomGeneric<BedrockOreDefinition>> list, BedrockOreDefinition def, int weight) {
		WeightedRandomGeneric<BedrockOreDefinition> weighted = new WeightedRandomGeneric<BedrockOreDefinition>(def, weight);
		list.add(weighted);
	}

	public static void registerBedrockOre(SolarSystem.Body body, BedrockOreDefinition def, int weight) {
		WeightedRandomGeneric<BedrockOreDefinition> weighted = new WeightedRandomGeneric<BedrockOreDefinition>(def, weight);

		if(!weightedPlanetOres.containsKey(body))
			weightedPlanetOres.put(body, new ArrayList<WeightedRandomGeneric<BedrockOreDefinition>>());

		List<WeightedRandomGeneric<BedrockOreDefinition>> list = weightedPlanetOres.get(body);
		list.add(weighted);
	}

	public static void generate(World world, int x, int z, ItemStack stack, FluidStack acid, int color, int tier) {
		generate(world, x, z, stack, acid, color, tier, ModBlocks.stone_depth, Blocks.stone);
	}

	public static void generate(World world, int x, int z, ItemStack stack, FluidStack acid, int color, int tier, Block depthRock, Block targetBlock) {

		for(int ix = x - 1; ix <= x + 1; ix++) {
			for(int iz = z - 1; iz <= z + 1; iz++) {

				Block b = world.getBlock(ix, 0, iz);
				if(b.isReplaceableOreGen(world, ix, 0, iz, Blocks.bedrock)) {
					if((ix == x && iz == z) || world.rand.nextBoolean()) {

						world.setBlock(ix, 0, iz, ModBlocks.ore_bedrock);
						TileEntityBedrockOre ore = (TileEntityBedrockOre) world.getTileEntity(ix, 0, iz);
						ore.resource = stack;
						ore.color = color;
						ore.shape = world.rand.nextInt(10);
						ore.acidRequirement = acid;
						ore.tier = tier;
						world.markBlockForUpdate(ix, 0, iz);
						world.markTileEntityChunkModified(ix, 0, iz, ore);
					}
				}
			}
		}

		for(int ix = x - 3; ix <= x + 3; ix++) {
			for(int iz = z - 3; iz <= z + 3; iz++) {

				for(int iy = 1; iy < 7; iy++) {
					if(iy < 3 || world.getBlock(ix, iy, iz) == Blocks.bedrock) {

						Block b = world.getBlock(ix, iy, iz);
						if(b.isReplaceableOreGen(world, ix, iy, iz, targetBlock) || b.isReplaceableOreGen(world, ix, iy, iz, Blocks.bedrock)) {
							world.setBlock(ix, iy, iz, depthRock);
						}
					}
				}
			}
		}
		if(targetBlock == Blocks.stone) { // Overworld-Markierung
			int surfaceY = world.getTopSolidOrLiquidBlock(x, z);

			// Standard-Blöcke (Failsafe)
			Block oreBlock = Blocks.coal_ore;
			Block centerBlock = Blocks.coal_block;
			Block stoneType = Blocks.stone; // Basis für den Hügel

			// Dynamische Auswahl basierend auf dem Stack-Inhalt
			String displayName = stack.getDisplayName().toLowerCase();

			if(displayName.contains("iron")) {
				oreBlock = ModBlocks.ore_iron; // Oder Blocks.iron_ore
				centerBlock = Blocks.iron_block;
			} else if(displayName.contains("copper")) {
				oreBlock = ModBlocks.ore_copper;
				centerBlock = ModBlocks.block_copper; // Prüfe ob ModBlocks.block_copper bei dir so heißt
			} else if(displayName.contains("carbon") || displayName.contains("coal")) {
				oreBlock = Blocks.coal_ore;
				centerBlock = Blocks.coal_block;
			}

			// Hügel-Generierung (3x3 oder 5x5 kleiner "Bump")
			for(int ix = x - 2; ix <= x + 2; ix++) {
				for(int iz = z - 2; iz <= z + 2; iz++) {

					// Distanz zum Zentrum für Hügelform
					double dist = Math.sqrt(Math.pow(ix - x, 2) + Math.pow(iz - z, 2));
					int y = world.getTopSolidOrLiquidBlock(ix, iz);

					if(dist < 2.5) {
						// Erzeuge den Hügel (1-2 Blöcke hoch)
						int height = dist < 1.2 ? 2 : 1;

						for(int h = 0; h < height; h++) {
							Block toPlace = stoneType;

							// Zufällige Erz-Flecken im Hügel
							if(world.rand.nextFloat() < 0.4F) toPlace = oreBlock;

							// Das absolute Zentrum
							if(ix == x && iz == z && h == height - 1) toPlace = centerBlock;

							world.setBlock(ix, y + h, iz, toPlace);
						}
					}
				}
			}
		}
	}

	public static class BedrockOreDefinition {
		public ItemStack stack;
		public FluidStack acid;
		public String id;
		public int tier;
		public int color;

		public BedrockOreDefinition(ItemStack stack, int tier, int color) {
			this(stack, tier, color, null);
		}

		public BedrockOreDefinition(ItemStack stack, int tier, int color, FluidStack acid) {
			this.stack = stack;
			this.id = stack.toString();
			this.tier = tier;
			this.color = color;
			this.acid = acid;
		}

		public BedrockOreDefinition(EnumBedrockOre type, int tier) {
			this(type, tier, null);
		}

		public BedrockOreDefinition(EnumBedrockOre type, int tier, FluidStack acid) {
			this.stack = DictFrame.fromOne(ModItems.ore_bedrock, type);
			this.id = "ore" + type.oreName;
			this.color = type.color;
			this.tier = tier;
			this.acid = acid;
		}
	}
}
