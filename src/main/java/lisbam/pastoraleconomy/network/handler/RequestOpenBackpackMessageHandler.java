package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.equipment.ShoulderEquipmentService;
import lisbam.pastoraleconomy.gui.GuiIds;
import lisbam.pastoraleconomy.item.ItemBackpack;
import lisbam.pastoraleconomy.network.message.RequestOpenBackpackMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Opens storage only while no other server Container is open and the shoulder slot owns a backpack. */
public final class RequestOpenBackpackMessageHandler
        implements IMessageHandler<RequestOpenBackpackMessage, IMessage> {
    @Override
    public IMessage onMessage(RequestOpenBackpackMessage message, MessageContext context) {
        if (!message.isValid() || context.getServerHandler() == null || context.getServerHandler().player == null) {
            return null;
        }
        final EntityPlayerMP player = context.getServerHandler().player;
        player.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (player.connection != null && !player.world.isRemote
                        && player.openContainer == player.inventoryContainer
                        && ItemBackpack.isBackpack(ShoulderEquipmentService.getShoulderStack(player))) {
                    player.openGui(LisBamPastoralEconomy.INSTANCE, GuiIds.BACKPACK, player.world, 0, 0, 0);
                }
            }
        });
        return null;
    }
}
