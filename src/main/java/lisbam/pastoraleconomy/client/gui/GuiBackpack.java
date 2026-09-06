package lisbam.pastoraleconomy.client.gui;

import lisbam.pastoraleconomy.gui.ContainerBackpack;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/** Three-row vanilla-texture backpack page with native Container slots and controls. */
public final class GuiBackpack extends GuiContainer {
    private static final ResourceLocation CHEST_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private static final int PREVIOUS_PAGE = 9001;
    private static final int NEXT_PAGE = 9002;

    private final ContainerBackpack backpackContainer;
    private GuiButton previousButton;
    private GuiButton nextButton;

    public GuiBackpack(InventoryPlayer playerInventory) {
        this(new ContainerBackpack(playerInventory));
    }

    private GuiBackpack(ContainerBackpack container) {
        super(container);
        this.backpackContainer = container;
        this.xSize = 176;
        this.ySize = 168;
    }

    @Override
    public void initGui() {
        super.initGui();
        previousButton = addButton(new GuiButton(PREVIOUS_PAGE, guiLeft + 128, guiTop + 4, 20, 12, "<"));
        nextButton = addButton(new GuiButton(NEXT_PAGE, guiLeft + 150, guiTop + 4, 20, 12, ">"));
        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int requested = backpackContainer.getPage();
        if (button.id == PREVIOUS_PAGE) {
            requested--;
        } else if (button.id == NEXT_PAGE) {
            requested++;
        } else {
            super.actionPerformed(button);
            return;
        }
        if (backpackContainer.setPage(requested)) {
            mc.playerController.sendEnchantPacket(backpackContainer.windowId, requested);
        }
        updateButtons();
    }

    private void updateButtons() {
        if (previousButton == null || nextButton == null) {
            return;
        }
        int page = backpackContainer.getPage();
        int count = backpackContainer.getPageCount();
        previousButton.visible = count > 1;
        nextButton.visible = count > 1;
        previousButton.enabled = page > 0;
        nextButton.enabled = page + 1 < count;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!ModGuiInput.closeWithExitKey(mc, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(backpackContainer.getBackpackName(), 8, 6, 0x404040);
        if (backpackContainer.getPageCount() > 1) {
            String page = (backpackContainer.getPage() + 1) + "/" + backpackContainer.getPageCount();
            fontRenderer.drawString(page, 112 - fontRenderer.getStringWidth(page) / 2, 6, 0x404040);
        }
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("container.inventory"), 8, 74, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(CHEST_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 71);
        drawTexturedModalRect(guiLeft, guiTop + 71, 0, 126, xSize, 96);
    }
}
