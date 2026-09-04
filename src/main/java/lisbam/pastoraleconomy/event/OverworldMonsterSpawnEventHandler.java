package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.entity.EnumCreatureType;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Blocks only vanilla/Forge natural monster spawning in the logical-server Overworld. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class OverworldMonsterSpawnEventHandler {
    private OverworldMonsterSpawnEventHandler() {
    }

    @SubscribeEvent
    public static void denyNaturalOverworldMonsters(LivingSpawnEvent.CheckSpawn event) {
        if (!event.getWorld().isRemote
                && event.getWorld().provider.getDimension() == 0
                && !event.isSpawner()
                && event.getEntityLiving().isCreatureType(EnumCreatureType.MONSTER, false)) {
            event.setResult(Event.Result.DENY);
        }
    }
}
