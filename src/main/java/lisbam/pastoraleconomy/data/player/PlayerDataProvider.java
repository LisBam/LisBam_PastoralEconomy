package lisbam.pastoraleconomy.data.player;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

/** Capability provider attached to each EntityPlayer, on both physical sides. */
public final class PlayerDataProvider implements ICapabilitySerializable<NBTTagCompound> {
    private final IPlayerData playerData = new PlayerData();

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == PlayerDataCapability.CAPABILITY;
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == PlayerDataCapability.CAPABILITY) {
            return PlayerDataCapability.CAPABILITY.cast(playerData);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return (NBTTagCompound) PlayerDataCapability.CAPABILITY.getStorage().writeNBT(
                PlayerDataCapability.CAPABILITY,
                playerData,
                null
        );
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        PlayerDataCapability.CAPABILITY.getStorage().readNBT(
                PlayerDataCapability.CAPABILITY,
                playerData,
                null,
                nbt
        );
    }
}
