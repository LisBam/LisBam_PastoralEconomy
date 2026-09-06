package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.equipment.BackpackStorage;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

/** Exact requested recipe with a data-loss guard for non-empty ingredient backpacks. */
public final class RecipeAdvancedBackpack extends ShapedRecipes {
    public RecipeAdvancedBackpack() {
        super(LisBamPastoralEconomy.MODID, 3, 3, createIngredients(),
                new ItemStack(ModItems.ADVANCED_BACKPACK));
        setRegistryName(LisBamPastoralEconomy.MODID, "advanced_backpack");
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        if (!super.matches(inventory, world)) {
            return false;
        }
        for (int index = 0; index < inventory.getSizeInventory(); index++) {
            ItemStack stack = inventory.getStackInSlot(index);
            if (stack.getItem() == ModItems.BACKPACK && !BackpackStorage.isEmpty(stack)) {
                return false;
            }
        }
        return true;
    }

    private static NonNullList<Ingredient> createIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        ingredients.set(0, Ingredient.fromItem(Items.LEATHER));
        ingredients.set(1, Ingredient.fromItem(ModItems.BACKPACK));
        ingredients.set(2, Ingredient.fromItem(Items.LEATHER));
        ingredients.set(3, Ingredient.fromItem(Items.LEATHER));
        ingredients.set(4, Ingredient.fromItem(ModItems.BACKPACK));
        ingredients.set(5, Ingredient.fromItem(Items.LEATHER));
        ingredients.set(6, Ingredient.fromItem(Items.LEAD));
        ingredients.set(8, Ingredient.fromItem(Items.LEAD));
        return ingredients;
    }
}
