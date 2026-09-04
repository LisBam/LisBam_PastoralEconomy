package lisbam.pastoraleconomy.block;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.block.Block;

/** Formal blocks introduced by completed gameplay batches. */
public final class ModBlocks {
    public static final BlockVillageStation VILLAGE_STATION = new BlockVillageStation();
    public static final BlockCrabTrap CRAB_TRAP = new BlockCrabTrap();
    public static final BlockTransportStation TRANSPORT_STATION = new BlockTransportStation();

    private ModBlocks() {
    }

    static BlockVillageStation createVillageStation() {
        return new BlockVillageStation();
    }
}
