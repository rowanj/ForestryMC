package forestry.factory.recipes.jei.fabricator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import forestry.api.recipes.IFabricatorSmeltingRecipe;
import forestry.core.recipes.jei.ForestryRecipeCategory;
import forestry.core.recipes.jei.ForestryRecipeCategoryUid;
import forestry.factory.recipes.FabricatorSmeltingRecipeManager;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.ICraftingGridHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

public class FabricatorRecipeCategory extends ForestryRecipeCategory {

	private static final int planSlot = 0;
	private static final int smeltingInputSlot = 1;
	private static final int craftOutputSlot = 2;
	private static final int craftInputSlot = 3;
	
	private static final int inputTank = 0;
	
	private final static ResourceLocation guiTexture = new ResourceLocation("forestry", "textures/gui/fabricator.png");
	@Nonnull
	private final ICraftingGridHelper craftingGridHelper;
	@Nonnull
	protected final IDrawable tankOverlay;
	
	public FabricatorRecipeCategory(IGuiHelper guiHelper) {
		super(guiHelper.createDrawable(guiTexture, 0, 0, 166, 65), "tile.for.factory2.0.name");
		
		craftingGridHelper = guiHelper.createCraftingGridHelper(craftInputSlot, craftOutputSlot);
		this.tankOverlay = guiHelper.createDrawable(guiTexture, 176, 0, 16, 58);
	}
	
	@Nonnull
	@Override
	public String getUid() {
		return ForestryRecipeCategoryUid.FABRICATOR;
	}

	@Override
	public void drawExtras(Minecraft minecraft) {	
	}

	@Override
	public void drawAnimations(Minecraft minecraft) {
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper recipeWrapper) {
		IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
		IGuiFluidStackGroup guiFluidStacks = recipeLayout.getFluidStacks();
		
		guiItemStacks.init(planSlot, true, 134, 6);
		
		guiItemStacks.init(smeltingInputSlot, true, 21, 10);
		
		guiItemStacks.init(craftOutputSlot, false, 134, 42);
		
		for (int y = 0; y < 3; ++y) {
			for (int x = 0; x < 3; ++x) {
				int index = craftInputSlot + x + (y * 3);
				guiItemStacks.init(index, true, 62 + x * 18, 6 + y * 18);
			}
		}
		
		guiFluidStacks.init(inputTank, true, 21, 37, 16, 16, 10000, false, tankOverlay);
		
		FabricatorRecipeWrapper wrapper = (FabricatorRecipeWrapper) recipeWrapper;
		guiItemStacks.set(planSlot, wrapper.getRecipe().getPlan());
		
		List<ItemStack> smeltingInput = new ArrayList<>();
		for (IFabricatorSmeltingRecipe s : getSmeltingInputs().get(wrapper.getRecipe().getLiquid().getFluid())) {
			smeltingInput.add(s.getResource());
		}
		if (!smeltingInput.isEmpty()) {
			guiItemStacks.set(smeltingInputSlot, smeltingInput);
		}
		
		craftingGridHelper.setOutput(guiItemStacks, wrapper.getOutputs());
		List<Object> inputs = new ArrayList<>();
		for(Object ingredient : wrapper.getRecipe().getIngredients()){
			inputs.add(ingredient);
		}
		craftingGridHelper.setInput(guiItemStacks, inputs, wrapper.getRecipe().getWidth(), wrapper.getRecipe().getHeight());
		
		guiFluidStacks.set(inputTank, wrapper.getFluidInputs());
		
	}
	
	private static Map<Fluid, List<IFabricatorSmeltingRecipe>> getSmeltingInputs() {
		Map<Fluid, List<IFabricatorSmeltingRecipe>> smeltingInputs = new HashMap<>();
		for (IFabricatorSmeltingRecipe smelting : FabricatorSmeltingRecipeManager.recipes) {
			Fluid fluid = smelting.getProduct().getFluid();
			if (!smeltingInputs.containsKey(fluid)) {
				smeltingInputs.put(fluid, new ArrayList<IFabricatorSmeltingRecipe>());
			}
			smeltingInputs.get(fluid).add(smelting);
		}
		return smeltingInputs;
	}

}