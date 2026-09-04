package lisbam.pastoraleconomy;

import lisbam.pastoraleconomy.proxy.CommonProxy;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = LisBamPastoralEconomy.MODID,
        name = LisBamPastoralEconomy.NAME,
        version = LisBamPastoralEconomy.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        useMetadata = true,
        guiFactory = "lisbam.pastoraleconomy.client.config.ModGuiFactory"
)
public final class LisBamPastoralEconomy {
    public static final String MODID = "lisbam_pastoral_economy";
    public static final String NAME = "LisBam_PastoralEconomy";
    public static final String VERSION = "1.0";
    @Mod.Instance(MODID)
    public static LisBamPastoralEconomy INSTANCE;

    @SidedProxy(
            clientSide = "lisbam.pastoraleconomy.proxy.ClientProxy",
            serverSide = "lisbam.pastoraleconomy.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FMLLog.info("Loading %s %s for Minecraft 1.12.2.", NAME, VERSION);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
