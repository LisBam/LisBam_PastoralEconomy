package lisbam.pastoraleconomy.registry;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.block.ModBlocks;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import lisbam.pastoraleconomy.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class RegistrationHandler {
    private static final ResourceLocation LEGACY_SHEARS_EFFICIENCY =
            new ResourceLocation(LisBamPastoralEconomy.MODID, "shears_efficiency");

    private RegistrationHandler() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(ModBlocks.VILLAGE_STATION, ModBlocks.CRAB_TRAP, ModBlocks.TRANSPORT_STATION);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ModItems.MARKET_BOOK, ModItems.GOLDEN_BONE_MEAL, ModItems.MERCHANT_SPAWN_EGG,
                ModItems.CRAB_TRAP_ITEM, ModItems.TRANSPORT_STATION_ITEM);
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        event.getRegistry().registerAll(ModEnchantments.getAll());
    }

    /**
     * The short-lived custom shears Efficiency registry entry was removed in
     * favor of vanilla Efficiency. Keep existing item NBT valid by remapping
     * worlds that recorded its old registry name to the native enchantment.
     */
    @SubscribeEvent
    public static void remapLegacyShearsEfficiency(RegistryEvent.MissingMappings<Enchantment> event) {
        for (RegistryEvent.MissingMappings.Mapping<Enchantment> mapping : event.getMappings()) {
            if (LEGACY_SHEARS_EFFICIENCY.equals(mapping.key)) {
                mapping.remap(Enchantments.EFFICIENCY);
            }
        }
    }
}
