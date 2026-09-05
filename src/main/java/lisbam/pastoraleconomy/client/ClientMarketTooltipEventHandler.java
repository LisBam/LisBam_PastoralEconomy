package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.market.MarketCatalog;
import lisbam.pastoraleconomy.market.MarketCommodity;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.RequestMarketTooltipPriceMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Adds the server-authoritative today price to hover tooltips for sellable stacks. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ClientMarketTooltipEventHandler {
    private ClientMarketTooltipEventHandler() {
    }

    @SubscribeEvent
    public static void addSellPrice(ItemTooltipEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || minecraft.player == null || player.world == null
                || !player.getUniqueID().equals(minecraft.player.getUniqueID())) {
            return;
        }
        MarketCommodity commodity = MarketCatalog.findSellCommodity(event.getItemStack());
        if (commodity == null) {
            return;
        }

        long marketDay = player.world.getWorldTime() / PastoralWorldData.MARKET_DAY_TICKS;
        Long price = ClientMarketTooltipState.getPriceForDay(commodity.getKey(), marketDay);
        if (price != null) {
            event.getToolTip().add(I18n.format("tooltip.lisbam_pastoral_economy.market_sell_price",
                    ClientHudEventHandler.formatCoins(price.longValue())));
            return;
        }

        if (ClientMarketTooltipState.shouldRequest(commodity.getKey(), player.world.getTotalWorldTime())) {
            ModNetwork.CHANNEL.sendToServer(new RequestMarketTooltipPriceMessage(commodity.getKey()));
        }
        event.getToolTip().add(I18n.format("tooltip.lisbam_pastoral_economy.market_sell_price_loading"));
    }
}
