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
    static boolean closeWithExitKey(Minecraft minecraft, int keyCode) {
        if (minecraft != null && minecraft.player != null
                && isExitKey(keyCode, minecraft.gameSettings.keyBindInventory.isActiveAndMatches(keyCode))) {
            // EntityPlayerSP#closeScreen also notifies the matching server
            // Container, preventing an orphaned book, merchant, or station session.
            minecraft.player.closeScreen();
            return true;
        }
        return false;
    }

    static boolean isExitKey(int keyCode, boolean inventoryKeyMatches) {
        return keyCode == Keyboard.KEY_ESCAPE || inventoryKeyMatches;
    }
}
