package lisbam.pastoraleconomy.network;

import lisbam.pastoraleconomy.network.handler.SyncCoinsMessageHandler;
import lisbam.pastoraleconomy.network.handler.RequestMarketHistoryMessageHandler;
import lisbam.pastoraleconomy.network.handler.RequestMarketTooltipPriceMessageHandler;
import lisbam.pastoraleconomy.network.handler.SyncMarketHistoryMessageHandler;
import lisbam.pastoraleconomy.network.handler.SyncMarketTooltipPriceMessageHandler;
import lisbam.pastoraleconomy.network.handler.SyncShoulderEquipmentMessageHandler;
import lisbam.pastoraleconomy.network.handler.MerchantTradeRequestMessageHandler;
import lisbam.pastoraleconomy.network.handler.SyncMerchantTradeMessageHandler;
import lisbam.pastoraleconomy.network.handler.SyncTransportStateMessageHandler;
import lisbam.pastoraleconomy.network.handler.TransportStationActionMessageHandler;
import lisbam.pastoraleconomy.network.handler.RequestOpenBackpackMessageHandler;
import lisbam.pastoraleconomy.network.handler.EmeraldTradeRequestMessageHandler;
import lisbam.pastoraleconomy.network.message.RequestMarketHistoryMessage;
import lisbam.pastoraleconomy.network.message.RequestMarketTooltipPriceMessage;
import lisbam.pastoraleconomy.network.message.SyncCoinsMessage;
import lisbam.pastoraleconomy.network.message.SyncMarketHistoryMessage;
import lisbam.pastoraleconomy.network.message.SyncMarketTooltipPriceMessage;
import lisbam.pastoraleconomy.network.message.SyncShoulderEquipmentMessage;
import lisbam.pastoraleconomy.network.message.MerchantTradeRequestMessage;
import lisbam.pastoraleconomy.network.message.SyncMerchantTradeMessage;
import lisbam.pastoraleconomy.network.message.SyncTransportStateMessage;
import lisbam.pastoraleconomy.network.message.TransportStationActionMessage;
import lisbam.pastoraleconomy.network.message.RequestOpenBackpackMessage;
import lisbam.pastoraleconomy.network.message.EmeraldTradeRequestMessage;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashSet;
import java.util.Set;

/**
 * The sole packet channel for this mod. Packet IDs are supplied explicitly by
 * the feature that introduces a real packet and cannot be silently reused.
 */
public final class ModNetwork {
    public static final String CHANNEL_NAME = "lb_pastoral";
    /**
     * Stable protocol discriminator allocation. New packets must be appended
     * without renumbering this value.
     */
    public static final int PACKET_SYNC_COINS = 0;
    public static final int PACKET_REQUEST_MARKET_HISTORY = 1;
    public static final int PACKET_SYNC_MARKET_HISTORY = 2;
    public static final int PACKET_REQUEST_MERCHANT_TRADE = 3;
    public static final int PACKET_SYNC_MERCHANT_TRADE = 4;
    public static final int PACKET_TRANSPORT_STATION_ACTION = 5;
    public static final int PACKET_SYNC_TRANSPORT_STATE = 6;
    public static final int PACKET_REQUEST_MARKET_TOOLTIP_PRICE = 7;
    public static final int PACKET_SYNC_MARKET_TOOLTIP_PRICE = 8;
    public static final int PACKET_SYNC_SHOULDER_EQUIPMENT = 9;
    public static final int PACKET_REQUEST_OPEN_BACKPACK = 10;
    public static final int PACKET_REQUEST_EMERALD_TRADE = 11;
    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);

    private static final Set<Integer> REGISTERED_PACKET_IDS = new HashSet<Integer>();
    private static boolean initialized;

    private ModNetwork() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        registerClientbound(
                PACKET_SYNC_COINS,
                SyncCoinsMessageHandler.class,
                SyncCoinsMessage.class
        );
        registerServerbound(
                PACKET_REQUEST_MARKET_HISTORY,
                RequestMarketHistoryMessageHandler.class,
                RequestMarketHistoryMessage.class
        );
        registerClientbound(
                PACKET_SYNC_MARKET_HISTORY,
                SyncMarketHistoryMessageHandler.class,
                SyncMarketHistoryMessage.class
        );
        registerServerbound(PACKET_REQUEST_MERCHANT_TRADE, MerchantTradeRequestMessageHandler.class,
                MerchantTradeRequestMessage.class);
        registerClientbound(PACKET_SYNC_MERCHANT_TRADE, SyncMerchantTradeMessageHandler.class,
                SyncMerchantTradeMessage.class);
        registerServerbound(PACKET_TRANSPORT_STATION_ACTION, TransportStationActionMessageHandler.class,
                TransportStationActionMessage.class);
        registerClientbound(PACKET_SYNC_TRANSPORT_STATE, SyncTransportStateMessageHandler.class,
                SyncTransportStateMessage.class);
        registerServerbound(PACKET_REQUEST_MARKET_TOOLTIP_PRICE, RequestMarketTooltipPriceMessageHandler.class,
                RequestMarketTooltipPriceMessage.class);
        registerClientbound(PACKET_SYNC_MARKET_TOOLTIP_PRICE, SyncMarketTooltipPriceMessageHandler.class,
                SyncMarketTooltipPriceMessage.class);
        registerClientbound(PACKET_SYNC_SHOULDER_EQUIPMENT, SyncShoulderEquipmentMessageHandler.class,
                SyncShoulderEquipmentMessage.class);
        registerServerbound(PACKET_REQUEST_OPEN_BACKPACK, RequestOpenBackpackMessageHandler.class,
                RequestOpenBackpackMessage.class);
        registerServerbound(PACKET_REQUEST_EMERALD_TRADE, EmeraldTradeRequestMessageHandler.class,
                EmeraldTradeRequestMessage.class);
    }

    public static <REQUEST extends IMessage, REPLY extends IMessage> void registerServerbound(
            int packetId,
            Class<? extends IMessageHandler<REQUEST, REPLY>> handler,
            Class<REQUEST> messageClass
    ) {
        register(packetId, handler, messageClass, Side.SERVER);
    }

    public static <REQUEST extends IMessage, REPLY extends IMessage> void registerClientbound(
            int packetId,
            Class<? extends IMessageHandler<REQUEST, REPLY>> handler,
            Class<REQUEST> messageClass
    ) {
        register(packetId, handler, messageClass, Side.CLIENT);
    }

    private static synchronized <REQUEST extends IMessage, REPLY extends IMessage> void register(
            int packetId,
            Class<? extends IMessageHandler<REQUEST, REPLY>> handler,
            Class<REQUEST> messageClass,
            Side receivingSide
    ) {
        if (!initialized) {
            throw new IllegalStateException("ModNetwork must be initialized before packet registration.");
        }
        if (packetId < 0) {
            throw new IllegalArgumentException("Packet IDs must be non-negative.");
        }
        if (!REGISTERED_PACKET_IDS.add(packetId)) {
            throw new IllegalArgumentException("Packet ID " + packetId + " is already registered.");
        }
        CHANNEL.registerMessage(handler, messageClass, packetId, receivingSide);
    }
}
