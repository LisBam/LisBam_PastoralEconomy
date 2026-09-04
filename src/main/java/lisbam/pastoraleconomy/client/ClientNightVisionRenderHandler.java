package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Physical-client-only visual aid. Gamma is raised only between render START
 * and END and restored verbatim; no potion, world light, player data, or server
 * state is changed.
 */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ClientNightVisionRenderHandler {
    private static boolean gammaOverridden;
    private static float originalGamma;

    private ClientNightVisionRenderHandler() {
    }

    @SubscribeEvent
    public static void renderNightVision(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            beginFrame();
        } else if (event.phase == TickEvent.Phase.END) {
            restoreGamma();
        }
    }

    @SubscribeEvent
    public static void restoreAfterDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        restoreGamma();
    }

    private static void beginFrame() {
        restoreGamma();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.gameSettings == null) {
            return;
        }
        ItemStack helmet = minecraft.player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (!ModEnchantments.isHelmet(helmet)
                || EnchantmentHelper.getEnchantmentLevel(ModEnchantments.NIGHT_VISION, helmet) <= 0) {
            return;
        }
        originalGamma = minecraft.gameSettings.gammaSetting;
        minecraft.gameSettings.gammaSetting = Math.max(originalGamma, 1.0F);
        gammaOverridden = true;
    }

    private static void restoreGamma() {
        if (!gammaOverridden) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings != null) {
            minecraft.gameSettings.gammaSetting = originalGamma;
        }
        gammaOverridden = false;
    }
}
