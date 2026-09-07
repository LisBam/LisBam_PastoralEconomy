package lisbam.pastoraleconomy.tile;

import lisbam.pastoraleconomy.chunkloader.ChunkLoaderService;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Persistent block-instance state for one redstone-controlled chunk loader. */
public final class TileChunkLoader extends TileEntity {
    private static final String KEY_ACTIVE = "active";

    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        if (active != value) {
            active = value;
            markDirty();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            ChunkLoaderService.onChunkLoaderTileLoaded(this);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean(KEY_ACTIVE, active);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        active = compound.getBoolean(KEY_ACTIVE);
    }
}
