package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.block.BlockTransportStation;
import lisbam.pastoraleconomy.block.ModBlocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

/** ItemBlock with the same explicit display key as its station block. */
public final class ItemTransportStation extends ItemBlock {
    public ItemTransportStation() {
        super(ModBlocks.TRANSPORT_STATION);
        setRegistryName(ModBlocks.TRANSPORT_STATION.getRegistryName());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.translateToLocal(BlockTransportStation.DISPLAY_NAME_KEY);
    }
}
