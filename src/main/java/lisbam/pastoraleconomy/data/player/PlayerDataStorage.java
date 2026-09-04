package lisbam.pastoraleconomy.data.player;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

/** NBT storage is the only direct serialization path for player data. */
final class PlayerDataStorage implements Capability.IStorage<IPlayerData> {
    @Override
    public NBTBase writeNBT(Capability<IPlayerData> capability, IPlayerData instance, EnumFacing side) {
        if (!(instance instanceof PlayerData)) {
            throw new IllegalArgumentException("Unexpected player data capability implementation.");
        }
        return ((PlayerData) instance).writeToNBT();
    }

    @Override
    public void readNBT(Capability<IPlayerData> capability, IPlayerData instance, EnumFacing side, NBTBase nbt) {
        if (!(instance instanceof PlayerData) || !(nbt instanceof NBTTagCompound)) {
            return;
        }
        ((PlayerData) instance).readFromNBT((NBTTagCompound) nbt);
    }
}
