package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.block.BlockChunkLoader;
import lisbam.pastoraleconomy.block.ModBlocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

/** ItemBlock with the explicit base display key used by the chunk-loader block. */
public final class ItemChunkLoader extends ItemBlock {
    public ItemChunkLoader() {
        super(ModBlocks.CHUNK_LOADER);
        setRegistryName(ModBlocks.CHUNK_LOADER.getRegistryName());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.translateToLocal(BlockChunkLoader.DISPLAY_NAME_KEY);
    }
}
