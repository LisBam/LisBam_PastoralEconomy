package lisbam.pastoraleconomy.client.gui;

import lisbam.pastoraleconomy.gui.ContainerShippingBox;
import lisbam.pastoraleconomy.tile.TileShippingBox;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

/** Physical-client-only standard chest presentation for the 27 shipping slots. */
public final class GuiShippingBox extends GuiContainer {
    private static final int VANILLA_CONTAINER_TEXT_COLOR = 4210752;
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
            "minecraft", "textures/gui/container/generic_54.png");

    public GuiShippingBox(InventoryPlayer playerInventory, TileShippingBox box) {
        super(new ContainerShippingBox(playerInventory, box));
        xSize = 176;
        ySize = 168;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.lisbam_pastoral_economy.shipping_box"), 8, 6,
                VANILLA_CONTAINER_TEXT_COLOR);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, VANILLA_CONTAINER_TEXT_COLOR);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        int upperInventoryHeight = 3 * 18 + 17;
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, upperInventoryHeight);
        drawTexturedModalRect(guiLeft, guiTop + upperInventoryHeight, 0, 126, xSize, 96);
    }
}
