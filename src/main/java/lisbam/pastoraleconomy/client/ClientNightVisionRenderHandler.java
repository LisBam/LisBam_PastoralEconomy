package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Client-only Night Vision implementation. It keeps a short-lived local
 * render window and raises gamma while the enchanted helmet is worn; no
 * vanilla status-effect instance or server-side effect is created.
 */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ClientNightVisionRenderHandler {
    private static final long DURATION_TICKS = 20L * 10L;
    private static final long RENEWAL_TICKS = 20L * 5L;
    private static final float NIGHT_VISION_GAMMA = 16.0F;

    private static boolean active;
    private static float originalGamma;
    private static long effectUntilTick;
    private static long nextRenewalTick;

    private ClientNightVisionRenderHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null || !hasNightVisionHelmet(player)) {
            restore(minecraft);
            return;
        }

        long currentTick = player.ticksExisted;
        if (!active) {
            active = true;
            originalGamma = minecraft.gameSettings.gammaSetting;
            effectUntilTick = currentTick + DURATION_TICKS;
            nextRenewalTick = currentTick + RENEWAL_TICKS;
        } else if (currentTick >= nextRenewalTick) {
            effectUntilTick = currentTick + DURATION_TICKS;
            nextRenewalTick = currentTick + RENEWAL_TICKS;
        }

        if (currentTick < effectUntilTick) {
            minecraft.gameSettings.gammaSetting = Math.max(originalGamma, NIGHT_VISION_GAMMA);
        } else {
            restore(minecraft);
        }
    }

    @SubscribeEvent
    public static void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        restore(Minecraft.getMinecraft());
    }

    /** Called by client cache lifecycle code when a connection is replaced. */
    public static void clear() {
        restore(Minecraft.getMinecraft());
    }

    private static boolean hasNightVisionHelmet(EntityPlayer player) {
        ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        return ModEnchantments.isHelmet(helmet)
                && EnchantmentHelper.getEnchantmentLevel(ModEnchantments.NIGHT_VISION, helmet) > 0;
    }

    private static void restore(Minecraft minecraft) {
        if (active) {
            minecraft.gameSettings.gammaSetting = originalGamma;
        }
        active = false;
        effectUntilTick = 0L;
        nextRenewalTick = 0L;
    }
}
