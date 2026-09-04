package lisbam.pastoraleconomy.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

/** One discoverable creative-inventory page containing every mod item. */
public final class PastoralCreativeTab extends CreativeTabs {
    public static final PastoralCreativeTab INSTANCE = new PastoralCreativeTab();

    private PastoralCreativeTab() {
        super("lisbam_pastoral_economy");
    }

    @Override
    public ItemStack getTabIconItem() {
        return new ItemStack(ModItems.GOLDEN_BONE_MEAL);
    }
}
