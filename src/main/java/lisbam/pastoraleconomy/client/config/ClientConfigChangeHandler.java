package lisbam.pastoraleconomy.client.config;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.config.ModSettings;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Saves Mod List edits on the physical client without adding a network path. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ClientConfigChangeHandler {
    private ClientConfigChangeHandler() {
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (LisBamPastoralEconomy.MODID.equals(event.getModID())) {
            ModSettings.save();
        }
    }
}
