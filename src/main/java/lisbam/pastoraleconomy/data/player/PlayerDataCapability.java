package lisbam.pastoraleconomy.data.player;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

/** Registration and lookup boundary for the single player-data capability. */
public final class PlayerDataCapability {
    public static final ResourceLocation ID =
            new ResourceLocation(LisBamPastoralEconomy.MODID, "player_data");

    @CapabilityInject(IPlayerData.class)
    public static final Capability<IPlayerData> CAPABILITY = null;

    private static boolean registered;

    private PlayerDataCapability() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        CapabilityManager.INSTANCE.register(IPlayerData.class, new PlayerDataStorage(), PlayerData::new);
        registered = true;
    }

    @Nullable
    public static IPlayerData get(EntityPlayer player) {
        return player.getCapability(CAPABILITY, null);
    }
}
