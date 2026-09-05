package lisbam.pastoraleconomy.client.gui;

import org.lwjgl.input.Keyboard;

/** Regression guard for orphaned server Containers behind custom GuiScreen views. */
public final class ModGuiInputSelfTest {
    private ModGuiInputSelfTest() {
    }

    public static void main(String[] args) {
        check(ModGuiInput.isExitKey(Keyboard.KEY_ESCAPE, false), "Escape closes the server Container");
        check(ModGuiInput.isExitKey(42, true), "remapped inventory key closes the server Container");
        check(!ModGuiInput.isExitKey(42, false), "unrelated key keeps the GUI open");
        System.out.println("modGuiInputSelfTest PASS");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException("Failed: " + description);
        }
    }
}
