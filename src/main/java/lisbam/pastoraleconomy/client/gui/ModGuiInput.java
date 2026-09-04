package lisbam.pastoraleconomy.client.gui;

import net.minecraft.client.Minecraft;

/** Shared 1.12.2 inventory-key closing behaviour for custom GuiScreen views. */
final class ModGuiInput {
    private ModGuiInput() {
    }

    static boolean closeWithInventoryKey(Minecraft minecraft, int keyCode) {
        if (minecraft != null && minecraft.player != null
                && minecraft.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            // EntityPlayerSP#closeScreen also notifies the matching server
            // Container, preventing an orphaned merchant/station session.
            minecraft.player.closeScreen();
            return true;
        }
        return false;
    }
}
