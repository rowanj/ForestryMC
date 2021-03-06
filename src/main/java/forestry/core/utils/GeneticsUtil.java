/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.core.utils;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

import com.mojang.authlib.GameProfile;
import forestry.api.arboriculture.ArboricultureCapabilities;
import forestry.api.arboriculture.EnumGermlingType;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.TreeManager;
import forestry.api.core.IArmorNaturalist;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.ICheckPollinatable;
import forestry.api.genetics.IChromosomeType;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ILeafTranslator;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.IPollinatable;
import forestry.api.genetics.ISaplingTranslator;
import forestry.api.genetics.ISpeciesRoot;
import forestry.api.genetics.ISpeciesRootPollinatable;
import forestry.api.lepidopterology.IButterfly;
import forestry.api.lepidopterology.IButterflyNursery;
import forestry.core.genetics.ItemGE;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GeneticsUtil {

	public static boolean hasNaturalistEye(EntityPlayer player) {
		if (player == null) {
			return false;
		}

		ItemStack armorItemStack = player.inventory.armorInventory[3];
		if (armorItemStack == null) {
			return false;
		}

		final Item armorItem = armorItemStack.getItem();
		final IArmorNaturalist armorNaturalist;
		if (armorItem instanceof IArmorNaturalist) { // legacy
			armorNaturalist = (IArmorNaturalist) armorItem;
		} else if (armorItemStack.hasCapability(ArboricultureCapabilities.ARMOR_NATURALIST, null)) {
			armorNaturalist = armorItemStack.getCapability(ArboricultureCapabilities.ARMOR_NATURALIST, null);
		} else {
			return false;
		}

		return armorNaturalist.canSeePollination(player, armorItemStack, true);
	}

	public static boolean canNurse(IButterfly butterfly, World world, final BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);

		if (tile instanceof IButterflyNursery) {
			return ((IButterflyNursery) tile).canNurse(butterfly);
		}

		// vanilla leaves can always be converted and then nurse
		return getPollen(world, pos) != null;
	}

	/**
	 * Returns an ICheckPollinatable that can be checked but not mated.
	 * Used to check for pollination traits without altering the world by changing vanilla leaves to forestry ones.
	 */
	public static ICheckPollinatable getCheckPollinatable(World world, final BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);

		if (tile instanceof IPollinatable) {
			return (IPollinatable) tile;
		}

		IIndividual pollen = getPollen(world, pos);
		if (pollen != null) {
			ISpeciesRoot root = pollen.getGenome().getSpeciesRoot();
			if(root instanceof ISpeciesRootPollinatable){
				return ((ISpeciesRootPollinatable)root).createPollinatable(pollen);
			}
		}

		return null;
	}

	/**
	 * Returns an IPollinatable that can be mated. This will convert vanilla leaves to Forestry leaves.
	 */
	public static IPollinatable getOrCreatePollinatable(GameProfile owner, World world, final BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);

		if (tile instanceof IPollinatable) {
			return (IPollinatable) tile;
		}

		final IIndividual pollen = getPollen(world, pos);
		if(pollen != null){
			ISpeciesRoot root = pollen.getGenome().getSpeciesRoot();
			if(root instanceof ISpeciesRootPollinatable){
				return ((ISpeciesRootPollinatable) root).tryConvertToPollinatable(owner, world, pos, pollen);
			}
		}

		return null;
	}

	/**
	 * Gets pollen from a location. Does not affect the pollen source.
	 */
	@Nullable
	public static IIndividual getPollen(World world, final BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);

		if (tile instanceof ICheckPollinatable) {
			return ((ICheckPollinatable) tile).getPollen();
		}

		IBlockState blockState = world.getBlockState(pos);
		if (blockState == null) {
			return null;
		}
		Block block = blockState.getBlock();

		ILeafTranslator leafTranslator = AlleleManager.leafTranslators.get(block);
		if (leafTranslator == null) {
			return null;
		}

		return leafTranslator.getTreeFromLeaf(blockState);
	}

	public static IIndividual getGeneticEquivalent(ItemStack itemStack) {
		if (itemStack == null) {
			return null;
		}

		Item item = itemStack.getItem();
		if (item instanceof ItemGE) {
			return ((ItemGE) item).getIndividual(itemStack);
		}

		ISaplingTranslator saplingTranslator = AlleleManager.saplingTranslation.get(item);
		if (saplingTranslator == null) {
			return null;
		}
		return saplingTranslator.getTreeFromSapling(itemStack);
	}

	public static ItemStack convertToGeneticEquivalent(ItemStack foreign) {
		if (foreign == null) {
			return null;
		}

		if (AlleleManager.alleleRegistry.getSpeciesRoot(foreign) != null) {
			return foreign;
		}

		IIndividual individual = getGeneticEquivalent(foreign);
		if (individual instanceof ITree) {
			ItemStack equivalent = TreeManager.treeRoot.getMemberStack(individual, EnumGermlingType.SAPLING);
			equivalent.stackSize = foreign.stackSize;
			return equivalent;
		}

		return null;
	}

	public static int getResearchComplexity(IAlleleSpecies species, IChromosomeType speciesChromosome) {
		return 1 + getGeneticAdvancement(species, new HashSet<>(), speciesChromosome);
	}

	private static int getGeneticAdvancement(IAlleleSpecies species, Set<IAlleleSpecies> exclude, IChromosomeType speciesChromosome) {
		int highest = 0;
		exclude.add(species);

		for (IMutation mutation : species.getRoot().getPaths(species, speciesChromosome)) {
			highest = getHighestAdvancement(mutation.getAllele0(), highest, exclude, speciesChromosome);
			highest = getHighestAdvancement(mutation.getAllele1(), highest, exclude, speciesChromosome);
		}

		return 1 + highest;
	}

	private static int getHighestAdvancement(IAlleleSpecies mutationSpecies, int highest, Set<IAlleleSpecies> exclude, IChromosomeType speciesChromosome) {
		if (exclude.contains(mutationSpecies) || AlleleManager.alleleRegistry.isBlacklisted(mutationSpecies.getUID())) {
			return highest;
		}

		int otherAdvance = getGeneticAdvancement(mutationSpecies, exclude, speciesChromosome);
		return otherAdvance > highest ? otherAdvance : highest;
	}
}
