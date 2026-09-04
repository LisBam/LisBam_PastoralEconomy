package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.gui.GuiIds;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/** Infinite-use display item. It opens the same GUI from either hand. */
public final class ItemMarketBook extends Item {
    public ItemMarketBook() {
        setRegistryName(LisBamPastoralEconomy.MODID, "market_book");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".market_book");
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack heldStack = player.getHeldItem(hand);
        if (!world.isRemote) {
            player.openGui(LisBamPastoralEconomy.INSTANCE, GuiIds.MARKET_BOOK, world, 0, 0, 0);
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, heldStack);
    }
}
