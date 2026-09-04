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
        ModelLoader.setCustomModelResourceLocation(
                ModItems.MARKET_BOOK,
                0,
                new ModelResourceLocation(new ResourceLocation(LisBamPastoralEconomy.MODID, "market_book"), "inventory")
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
    }
}
