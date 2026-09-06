package lisbam.pastoraleconomy.client.gui;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/** Shared 1.12.2 close behaviour for custom GuiScreen views with server Containers. */
final class ModGuiInput {
    private ModGuiInput() {
    }

    /**
     * GuiScreen handles Escape by only removing its client screen.  These
     * views also have an open server Container, so both normal close keys
     * must instead use EntityPlayerSP#closeScreen and send CPacketCloseWindow.
     */
    static boolean closeWithExitKey(Minecraft minecraft, int keyCode, boolean textInputFocused) {
        if (minecraft != null && minecraft.player != null
                && isExitKey(keyCode, minecraft.gameSettings.keyBindInventory.isActiveAndMatches(keyCode),
                textInputFocused)) {
            // EntityPlayerSP#closeScreen also notifies the matching server
            // Container, preventing an orphaned book, merchant, or station session.
            minecraft.player.closeScreen();
            return true;
        }
        return false;
    }

    /**
     * The inventory binding is also an ordinary printable key (E by default).
     * A focused text box owns that key so search/rename input is never treated
     * as a request to close the server Container. Escape remains a close key.
     */
    static boolean isExitKey(int keyCode, boolean inventoryKeyMatches, boolean textInputFocused) {
        return keyCode == Keyboard.KEY_ESCAPE || (!textInputFocused && inventoryKeyMatches);
    }
}
