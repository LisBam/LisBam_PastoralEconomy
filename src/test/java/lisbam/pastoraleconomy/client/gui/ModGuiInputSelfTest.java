package lisbam.pastoraleconomy.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Keyboard;

/** Regression guard for orphaned server Containers behind custom GuiScreen views. */
public final class ModGuiInputSelfTest {
    private ModGuiInputSelfTest() {
    }

    public static void main(String[] args) {
        check(ModGuiInput.isExitKey(Keyboard.KEY_ESCAPE, false), "Escape closes the server Container");
        check(ModGuiInput.isExitKey(42, true), "remapped inventory key closes the server Container");
        check(!ModGuiInput.isExitKey(42, false), "unrelated key keeps the GUI open");
        check(GuiContainer.class.isAssignableFrom(GuiMarketBook.class),
                "market book installs a dedicated client Container");
        check(GuiContainer.class.isAssignableFrom(GuiMerchantTrade.class),
                "merchant screen installs a dedicated client Container");
        check(GuiContainer.class.isAssignableFrom(GuiTransportStation.class),
                "transport screen installs a dedicated client Container");
        System.out.println("modGuiInputSelfTest PASS");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException("Failed: " + description);
        }
    }
}
