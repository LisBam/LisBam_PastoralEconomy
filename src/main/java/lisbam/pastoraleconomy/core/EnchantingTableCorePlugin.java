package lisbam.pastoraleconomy.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/** Loads the narrowly scoped 1.12.2 enchanting-table compatibility transformer. */
@IFMLLoadingPlugin.Name("LisBam Pastoral Economy Enchanting Compatibility")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"lisbam.pastoraleconomy.core"})
public final class EnchantingTableCorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{EnchantingTableTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // No setup data is needed; both transformed methods are matched by descriptor.
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
