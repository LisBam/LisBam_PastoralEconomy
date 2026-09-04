package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Physical-client HUD and connection cache lifecycle only. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ClientHudEventHandler {
    private static final int HUD_MARGIN = 4;
    private static final int HUD_COLOR = 0xFFFFFF;

    private ClientHudEventHandler() {
    }

    @SubscribeEvent
    public static void renderCoins(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.hideGUI || !ClientPlayerState.hasValidCoinSync()) {
            return;
        }

        String formattedBalance = formatCoins(ClientPlayerState.getCoins());
        String text = I18n.format("hud.lisbam_pastoral_economy.coins", formattedBalance);
        FontRenderer fontRenderer = minecraft.fontRenderer;
        int x = event.getResolution().getScaledWidth() - HUD_MARGIN - fontRenderer.getStringWidth(text);
        fontRenderer.drawStringWithShadow(text, x, HUD_MARGIN, HUD_COLOR);
    }

    @SubscribeEvent
    public static void clearOnClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        ClientPlayerState.clear();
        ClientMarketState.clear();
        ClientMerchantTradeState.clear();
        ClientTransportState.clear();
    }

    @SubscribeEvent
    public static void clearOnClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientPlayerState.clear();
        ClientMarketState.clear();
        ClientMerchantTradeState.clear();
        ClientTransportState.clear();
    }

    /** Fixed locale-independent formatter used by the display-only HUD. */
    public static String formatCoins(long balance) {
        long safeBalance = balance < 0L ? 0L : balance;
        String digits = Long.toString(safeBalance);
        StringBuilder formatted = new StringBuilder(digits.length() + digits.length() / 3);
        for (int index = 0; index < digits.length(); index++) {
            if (index > 0 && (digits.length() - index) % 3 == 0) {
                formatted.append(',');
            }
            formatted.append(digits.charAt(index));
        }
        return formatted.toString();
    }
}
