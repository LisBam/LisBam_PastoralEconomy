package lisbam.pastoraleconomy.client.gui;

import lisbam.pastoraleconomy.gui.ContainerCrabTrap;
import lisbam.pastoraleconomy.tile.TileCrabTrap;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

/** Physical-client-only 1.12.2 inventory GUI; all slots remain Container backed. */
public final class GuiCrabTrap extends GuiContainer {
    private static final int VANILLA_CONTAINER_TEXT_COLOR = 4210752;
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
            "minecraft", "textures/gui/container/generic_54.png");
    private static final int VANILLA_SLOT_U = 7;
    private static final int VANILLA_SLOT_V = 17;
    private static final int VANILLA_BACKGROUND_U = 90;
    private static final int VANILLA_BACKGROUND_V = 10;
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
        fontRenderer.drawString(I18n.format("container.lisbam_pastoral_economy.crab_trap"), 8, 6,
                VANILLA_CONTAINER_TEXT_COLOR);
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
        fontRenderer.drawString(status, 98, 24, VANILLA_CONTAINER_TEXT_COLOR);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, VANILLA_CONTAINER_TEXT_COLOR);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        int upperInventoryHeight = 4 * 18 + 17;
        // generic_54 contains four complete rows of slot artwork. The crab
        // trap owns only two input slots plus two harvest rows, so only reuse
        // its title strip and player inventory portion; copy the real vanilla
        // slot artwork for every Container-backed trap slot in between.
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 17);
        drawScaledCustomSizeModalRect(guiLeft, guiTop + 17, VANILLA_BACKGROUND_U, VANILLA_BACKGROUND_V,
                1, 1, xSize, upperInventoryHeight - 17, 256.0F, 256.0F);
        drawNativeSlot(guiLeft + 25, guiTop + 18);
        drawNativeSlot(guiLeft + 61, guiTop + 18);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 9; column++) {
                drawNativeSlot(guiLeft + 7 + column * 18, guiTop + 53 + row * 18);
            }
        }
        drawTexturedModalRect(guiLeft, guiTop + upperInventoryHeight, 0, 126, xSize, 96);
    }

    private void drawNativeSlot(int x, int y) {
        drawTexturedModalRect(x, y, VANILLA_SLOT_U, VANILLA_SLOT_V, 18, 18);
    }
}
