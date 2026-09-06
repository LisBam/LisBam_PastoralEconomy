package lisbam.pastoraleconomy.proxy;

import lisbam.pastoraleconomy.client.ClientCoinSyncExecutor;
import lisbam.pastoraleconomy.client.ClientMarketSyncExecutor;
import lisbam.pastoraleconomy.client.ClientMarketTooltipSyncExecutor;
import lisbam.pastoraleconomy.client.ClientTransportStateSyncExecutor;
import lisbam.pastoraleconomy.client.ClientShoulderEquipmentSyncExecutor;
import lisbam.pastoraleconomy.client.gui.GuiCrabTrap;
import lisbam.pastoraleconomy.client.gui.GuiMarketBook;
import lisbam.pastoraleconomy.client.gui.GuiTransportStation;
import lisbam.pastoraleconomy.gui.GuiIds;
import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import lisbam.pastoraleconomy.merchant.MerchantTradeSnapshot;
import lisbam.pastoraleconomy.client.ClientMerchantTradeSyncExecutor;
import lisbam.pastoraleconomy.client.RenderMerchant;
import lisbam.pastoraleconomy.client.gui.GuiMerchantTrade;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import lisbam.pastoraleconomy.entity.EntityMerchant;
import lisbam.pastoraleconomy.tile.TileCrabTrap;
import lisbam.pastoraleconomy.transport.TransportStateSnapshot;

public final class ClientProxy extends CommonProxy {
    @Override
    public void preInit(net.minecraftforge.fml.common.event.FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityMerchant.class, new IRenderFactory<EntityMerchant>() {
            @Override
            public net.minecraft.client.renderer.entity.Render<? super EntityMerchant> createRenderFor(
                    net.minecraft.client.renderer.entity.RenderManager manager) {
                return new RenderMerchant(manager);
            }
        });
    }
    @Override
    public void handleCoinsSync(long balance) {
        ClientCoinSyncExecutor.acceptServerBalance(balance);
    }

    @Override
    public Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        if (guiId == GuiIds.MARKET_BOOK) {
            return new GuiMarketBook();
        }
        if (guiId == GuiIds.MERCHANT_TRADE) {
            return new GuiMerchantTrade(x);
        }
        if (guiId == GuiIds.CRAB_TRAP
                && world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCrabTrap) {
            return new GuiCrabTrap(player.inventory,
                    (TileCrabTrap) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)));
        }
        if (guiId == GuiIds.TRANSPORT_STATION) {
            return new GuiTransportStation(new net.minecraft.util.math.BlockPos(x, y, z));
        }
        return null;
    }

    @Override
    public void handleMarketHistorySync(MarketHistorySnapshot snapshot) {
        ClientMarketSyncExecutor.acceptServerSnapshot(snapshot);
    }

    @Override
    public void handleMarketTooltipPriceSync(String commodityKey, long marketDay, long price) {
        ClientMarketTooltipSyncExecutor.acceptServerPrice(commodityKey, marketDay, price);
    }

    @Override
    public void handleMerchantTradeSync(MerchantTradeSnapshot snapshot) {
        ClientMerchantTradeSyncExecutor.acceptServerSnapshot(snapshot);
    }

    @Override
    public void handleTransportStateSync(TransportStateSnapshot snapshot) {
        ClientTransportStateSyncExecutor.acceptServerSnapshot(snapshot);
    }

    @Override
    public void handleShoulderEquipmentSync(int entityId, ItemStack stack) {
        ClientShoulderEquipmentSyncExecutor.acceptServerSnapshot(entityId, stack);
    }
}
