package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.block.ModBlocks;
import lisbam.pastoraleconomy.item.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Physical-client-only model registration for formal items. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ClientModelRegistry {
    private ClientModelRegistry() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerItemModel(ModItems.BACKPACK, "backpack");
        registerItemModel(ModItems.ADVANCED_BACKPACK, "advanced_backpack");
        registerItemModel(ModItems.SUPER_BACKPACK, "super_backpack");
        registerItemModel(ModItems.FEATHER_WINGS, "feather_wings");
        ModelLoader.setCustomModelResourceLocation(
                ModItems.MARKET_BOOK,
                0,
                new ModelResourceLocation(new ResourceLocation(LisBamPastoralEconomy.MODID, "market_book"), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                ModItems.TRADE_VOUCHER,
                0,
                new ModelResourceLocation(new ResourceLocation(LisBamPastoralEconomy.MODID,
                        "trade_voucher"), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                ModItems.GOLDEN_BONE_MEAL,
                0,
                new ModelResourceLocation(new ResourceLocation(LisBamPastoralEconomy.MODID, "golden_bone_meal"), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                ModItems.MERCHANT_SPAWN_EGG,
                0,
                new ModelResourceLocation(new ResourceLocation(LisBamPastoralEconomy.MODID, "merchant_spawn_egg"), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                ModItems.CRAB_TRAP_ITEM,
                0,
                new ModelResourceLocation(ModBlocks.CRAB_TRAP.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                ModItems.TRANSPORT_STATION_ITEM,
                0,
                new ModelResourceLocation(ModBlocks.TRANSPORT_STATION.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                ModItems.CHUNK_LOADER_ITEM,
                0,
                new ModelResourceLocation(ModBlocks.CHUNK_LOADER.getRegistryName(), "inventory")
        );
    }

    private static void registerItemModel(net.minecraft.item.Item item, String path) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(new ResourceLocation(LisBamPastoralEconomy.MODID, path), "inventory"));
    }
}
