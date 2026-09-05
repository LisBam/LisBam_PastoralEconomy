package lisbam.pastoraleconomy.proxy;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.config.ModSettings;
import lisbam.pastoraleconomy.data.player.PlayerDataCapability;
import lisbam.pastoraleconomy.entity.ModEntities;
import lisbam.pastoraleconomy.gui.ModGuiHandler;
import lisbam.pastoraleconomy.item.ItemGoldenBoneMeal;
import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import lisbam.pastoraleconomy.merchant.MerchantTradeSnapshot;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.transport.TransportStateSnapshot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        ModSettings.initialize(event.getSuggestedConfigurationFile());
        PlayerDataCapability.register();
        ModEntities.register();
        ModNetwork.initialize();
        ItemGoldenBoneMeal.registerCompatibility();
        FMLLog.info("Player data capability, merchant entities, and common network infrastructure are ready.");
    }

    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(
                LisBamPastoralEconomy.INSTANCE,
                new ModGuiHandler()
        );
        FMLLog.info("Common GUI handler is registered.");
    }

    /**
     * S2C packet entry point. The physical dedicated-server proxy deliberately
     * does nothing; the client proxy supplies the client-only implementation.
     */
    public void handleCoinsSync(long balance) {
        // Client-only work is isolated in ClientProxy.
    }

    /** Dedicated servers deliberately never construct a client GuiScreen. */
    public Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    /** ClientProxy schedules this display-only snapshot onto the client thread. */
    public void handleMarketHistorySync(MarketHistorySnapshot snapshot) {
        // Client-only work is isolated in ClientProxy.
    }

    /** ClientProxy schedules the display-only inventory-tooltip price onto the client thread. */
    public void handleMarketTooltipPriceSync(String commodityKey, long marketDay, long price) {
        // Client-only work is isolated in ClientProxy.
    }

    public void handleMerchantTradeSync(MerchantTradeSnapshot snapshot) {
        // Client-only work is isolated in ClientProxy.
    }

    public void handleTransportStateSync(TransportStateSnapshot snapshot) {
        // Client-only work is isolated in ClientProxy.
    }
}
