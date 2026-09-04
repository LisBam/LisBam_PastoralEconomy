package lisbam.pastoraleconomy.client.gui;

import lisbam.pastoraleconomy.gui.ContainerCrabTrap;
import lisbam.pastoraleconomy.tile.TileCrabTrap;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

/** Physical-client-only 1.12.2 inventory GUI; all slots remain Container backed. */
public final class GuiCrabTrap extends GuiContainer {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
            "minecraft", "textures/gui/container/generic_54.png");
    private final TileCrabTrap trap;

    public GuiCrabTrap(InventoryPlayer playerInventory, TileCrabTrap trap) {
        super(new ContainerCrabTrap(playerInventory, trap));
        this.trap = trap;
        xSize = 176;
        // Match GuiChest's four-row generic_54 layout exactly: the two harvest
        // rows occupy the last two rows of the upper inventory and the normal
        // player inventory texture begins immediately beneath them.
        ySize = 186;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.lisbam_pastoral_economy.crab_trap"), 8, 6, 0x404040);
        String status;
        if (trap.hasPendingLoot()) {
            status = I18n.format("gui.lisbam_pastoral_economy.crab_trap.pending");
        } else if (!trap.hasEffectiveWaterEnvironment()) {
            status = I18n.format("gui.lisbam_pastoral_economy.crab_trap.paused");
        } else if (trap.isRoundActive()) {
            status = I18n.format("gui.lisbam_pastoral_economy.crab_trap.waiting");
        } else {
            status = I18n.format("gui.lisbam_pastoral_economy.crab_trap.ready");
        }
        fontRenderer.drawString(status, 98, 24, 0x505050);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        int upperInventoryHeight = 4 * 18 + 17;
        // generic_54 contains four complete rows of slot artwork.  The crab
        // trap owns only two input slots plus two harvest rows, so only reuse
        // its title strip and player inventory portion; draw the actual trap
        // slots below to avoid visual slots that have no Container backing.
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 17);
        drawRect(guiLeft, guiTop + 17, guiLeft + xSize, guiTop + upperInventoryHeight, 0xFFC6C6C6);
        drawRect(guiLeft, guiTop + 17, guiLeft + 1, guiTop + upperInventoryHeight, 0xFF555555);
        drawRect(guiLeft + xSize - 1, guiTop + 17, guiLeft + xSize, guiTop + upperInventoryHeight, 0xFF555555);
        drawRect(guiLeft, guiTop + upperInventoryHeight - 1, guiLeft + xSize, guiTop + upperInventoryHeight, 0xFF555555);
        drawVanillaSlot(guiLeft + 25, guiTop + 18);
        drawVanillaSlot(guiLeft + 61, guiTop + 18);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 9; column++) {
                drawVanillaSlot(guiLeft + 7 + column * 18, guiTop + 53 + row * 18);
            }
        }
        drawTexturedModalRect(guiLeft, guiTop + upperInventoryHeight, 0, 126, xSize, 96);
    }

    private void drawVanillaSlot(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF373737);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF555555);
        drawRect(x + 1, y + 1, x + 17, y + 2, 0xFFFFFFFF);
        drawRect(x + 1, y + 1, x + 2, y + 17, 0xFFFFFFFF);
    }
}
