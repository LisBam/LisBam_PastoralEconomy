package lisbam.pastoraleconomy.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/** Loads the narrowly scoped 1.12.2 enchanting and shoulder-equipment transformers. */
@IFMLLoadingPlugin.Name("LisBam Pastoral Economy Enchanting Compatibility")
@IFMLLoadingPlugin.MCVersion("1.12.2")
// None of the mod's own classes are transformer targets. Keeping the complete
// namespace out of the global transformer chain also prevents unrelated
// coremods from returning a null byte array for a lazily loaded gameplay class.
@IFMLLoadingPlugin.TransformerExclusions({"lisbam.pastoraleconomy"})
public final class EnchantingTableCorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{EnchantingTableTransformer.class.getName(), ShoulderEquipmentTransformer.class.getName()};
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
