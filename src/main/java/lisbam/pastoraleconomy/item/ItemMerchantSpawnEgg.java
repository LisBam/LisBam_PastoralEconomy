package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.entity.EntityMerchant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Legacy 1.12.2 merchant spawn egg; unbound entities are adopted by a village on maintenance. */
public final class ItemMerchantSpawnEgg extends Item {
    public ItemMerchantSpawnEgg() {
        setRegistryName(LisBamPastoralEconomy.MODID, "merchant_spawn_egg");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".merchant_spawn_egg");
        setCreativeTab(ModItems.CREATIVE_TAB);
        setMaxStackSize(64);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        BlockPos spawnPos = pos.offset(facing);
        if (!world.isRemote && world.isAirBlock(spawnPos) && world.isAirBlock(spawnPos.up())) {
            EntityMerchant merchant = new EntityMerchant(world);
            merchant.setPosition(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
            if (world.spawnEntity(merchant) && !player.capabilities.isCreativeMode) {
                player.getHeldItem(hand).shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }
}
