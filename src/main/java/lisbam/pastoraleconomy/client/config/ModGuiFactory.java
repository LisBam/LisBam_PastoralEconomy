package lisbam.pastoraleconomy.client.config;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.config.ModSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

import java.util.Collections;
import java.util.Set;

/** Physical-client Forge 1.12.2 Mod List configuration entry point. */
public final class ModGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {
        // The shared settings are initialized during common preInit.
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfig(
                parentScreen,
                new ConfigElement(ModSettings.getConfiguration().getCategory(ModSettings.MARKET_CATEGORY))
                        .getChildElements(),
                LisBamPastoralEconomy.MODID,
                false,
                false,
                I18n.format("config." + LisBamPastoralEconomy.MODID + ".title")
        );
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }
}
