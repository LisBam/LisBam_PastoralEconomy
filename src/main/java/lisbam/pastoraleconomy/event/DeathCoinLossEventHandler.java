package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.data.player.CoinService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Deducts the server-owned coin death penalty before the player Capability is copied to the respawn entity. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class DeathCoinLossEventHandler {
    private DeathCoinLossEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void chargeDeathCoins(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        if (player.world.isRemote) {
            return;
        }
        long lostCoins = DeathCoinLossRules.rollLoss(CoinService.getBalance(player), player.world.rand);
        if (lostCoins <= 0L || !CoinService.trySpend(player, lostCoins)) {
            return;
        }
        player.sendMessage(new TextComponentString("本次死亡失去" + lostCoins + "金币！")
                .setStyle(new Style().setColor(TextFormatting.RED)));
    }
}
