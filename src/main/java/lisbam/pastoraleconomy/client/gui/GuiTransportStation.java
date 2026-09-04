package lisbam.pastoraleconomy.client.gui;

import lisbam.pastoraleconomy.client.ClientTransportState;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.TransportStationActionMessage;
import lisbam.pastoraleconomy.transport.TransportAction;
import lisbam.pastoraleconomy.transport.TransportNodeView;
import lisbam.pastoraleconomy.transport.TransportStateSnapshot;
import lisbam.pastoraleconomy.transport.VillageTransportCandidate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Responsive client-only transport view; every mutation remains a C2S request. */
public final class GuiTransportStation extends GuiScreen {
    private static final int BUTTON_CONNECT = 1;
    private static final int BUTTON_REMOVE = 2;
    private static final int BUTTON_RENAME = 3;
    private static final int BUTTON_PREVIOUS = 4;
    private static final int BUTTON_NEXT = 5;
    private static final int BUTTON_REFRESH = 6;
    private static final int BUTTON_CONNECT_VILLAGE = 8;
    private static final int BUTTON_TRAVEL = 9;
    private static final int BUTTON_CONFIRM_VILLAGE = 10;
    private static final int BUTTON_CANCEL_VILLAGE = 11;
    private static final int ROW_HEIGHT = 14;
    private static final ResourceLocation VANILLA_PANEL_TEXTURE = new ResourceLocation(
            "minecraft", "textures/gui/demo_background.png");
    /** Vanilla workbench/furnace inventory-title colour for static window text. */
    private static final int TEXT_COLOR = 0x404040;
    /** Keep actual GuiButton labels at their normal vanilla light colour. */
    private static final int BUTTON_TEXT_COLOR = 0xFFE0E0E0;

    private final BlockPos stationPosition;
    private GuiTextField renameField;
    private int selectedIndex = -1;
    private int scrollOffset;
    private boolean villageLookupRequested;
    private boolean villageConfirmationOpen;
    private Layout layout;

    public GuiTransportStation(BlockPos stationPosition) {
        this.stationPosition = stationPosition.toImmutable();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        layout = Layout.create(width, height);
        buttonList.add(new LightTextButton(BUTTON_CONNECT, layout.connectX, layout.actionRowOneY, layout.connectWidth, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.connect")));
        buttonList.add(new LightTextButton(BUTTON_REFRESH, layout.refreshX, layout.actionRowOneY, layout.refreshWidth, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.refresh")));
        buttonList.add(new LightTextButton(BUTTON_TRAVEL, layout.travelX, layout.actionRowOneY, layout.travelWidth, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.travel")));
        buttonList.add(new LightTextButton(BUTTON_CONNECT_VILLAGE, layout.contentX, layout.actionRowTwoY,
                layout.contentRight - layout.contentX, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.connect_village")));
        buttonList.add(new LightTextButton(BUTTON_PREVIOUS, layout.previousX, layout.listY - 16, 30, 14, "<"));
        buttonList.add(new LightTextButton(BUTTON_NEXT, layout.nextX, layout.listY - 16, 30, 14, ">"));
        buttonList.add(new LightTextButton(BUTTON_REMOVE, layout.contentX, layout.footerY, 82, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.remove")));
        buttonList.add(new LightTextButton(BUTTON_RENAME, layout.renameX, layout.footerY, 74, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.rename")));
        renameField = new GuiTextField(0, fontRenderer, layout.renameFieldX, layout.footerY + 2,
                layout.renameFieldWidth, 14);
        renameField.setMaxStringLength(96);
        renameField.setTextColor(TEXT_COLOR);
        renameField.setDisabledTextColour(TEXT_COLOR);
        buttonList.add(new LightTextButton(BUTTON_CONFIRM_VILLAGE, layout.dialogConfirmX, layout.dialogButtonY,
                layout.dialogButtonWidth, 18, I18n.format("gui.lisbam_pastoral_economy.transport.confirm")));
        buttonList.add(new LightTextButton(BUTTON_CANCEL_VILLAGE, layout.dialogCancelX, layout.dialogButtonY,
                layout.dialogButtonWidth, 18, I18n.format("gui.lisbam_pastoral_economy.transport.cancel")));
        updateControls(currentSnapshot());
        requestVillageCandidate(currentSnapshot());
    }

    @Override
    public void updateScreen() {
        renameField.updateCursorCounter();
        TransportStateSnapshot snapshot = currentSnapshot();
        updateControls(snapshot);
        requestVillageCandidate(snapshot);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        TransportStateSnapshot snapshot = currentSnapshot();
        if (snapshot == null || !snapshot.hasCurrentStation()) {
            return;
        }
        if (button.id == BUTTON_CANCEL_VILLAGE) {
            villageConfirmationOpen = false;
            updateControls(snapshot);
        } else if (button.id == BUTTON_CONFIRM_VILLAGE) {
            VillageTransportCandidate candidate = snapshot.getNearestVillage();
            if (button.enabled && candidate != null && snapshot.getCoins() >= candidate.getConnectionFee()) {
                send(TransportAction.CONNECT_VILLAGE, candidate.getVillageId(), "");
                villageConfirmationOpen = false;
                villageLookupRequested = false;
                updateControls(snapshot);
            }
        } else if (button.id == BUTTON_CONNECT) {
            send(TransportAction.CONNECT, snapshot.getCurrentStationId(), "");
        } else if (button.id == BUTTON_REFRESH) {
            send(TransportAction.REFRESH, snapshot.getCurrentStationId(), "");
        } else if (button.id == BUTTON_CONNECT_VILLAGE) {
            if (snapshot.getNearestVillage() != null) {
                villageConfirmationOpen = true;
                updateControls(snapshot);
            }
        } else if (button.id == BUTTON_TRAVEL) {
            TransportNodeView selected = selectedNode(snapshot.getNodes());
            if (selected != null && selected.hasTravelFee() && snapshot.getCoins() >= selected.getTravelFee()) {
                send(TransportAction.TRAVEL, selected.getStationId(), "");
            }
        } else if (button.id == BUTTON_PREVIOUS) {
            scrollOffset = Math.max(0, scrollOffset - visibleRows());
        } else if (button.id == BUTTON_NEXT) {
            scrollOffset = Math.min(maxScroll(snapshot.getNodes()), scrollOffset + visibleRows());
        } else {
            TransportNodeView selected = selectedNode(snapshot.getNodes());
            if (selected == null) {
                return;
            }
            if (button.id == BUTTON_REMOVE) {
                send(TransportAction.REMOVE, selected.getStationId(), "");
            } else if (button.id == BUTTON_RENAME) {
                send(TransportAction.RENAME, selected.getStationId(), renameField.getText());
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (villageConfirmationOpen) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        renameField.mouseClicked(mouseX, mouseY, mouseButton);
        TransportStateSnapshot snapshot = currentSnapshot();
        if (snapshot == null || mouseX < layout.contentX || mouseX >= layout.contentRight
                || mouseY < layout.listY || mouseY >= layout.footerY - 4) {
            return;
        }
        int row = (mouseY - layout.listY) / ROW_HEIGHT;
        if (row >= 0 && row < visibleRows()) {
            int index = scrollOffset + row;
            if (index < snapshot.getNodes().size()) {
                selectedIndex = index;
                renameField.setText(snapshot.getNodes().get(index).getAlias());
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (villageConfirmationOpen) {
            return;
        }
        if (!renameField.textboxKeyTyped(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawNativePanel(layout.panelX, layout.panelY, layout.panelRight, layout.panelBottom);
        drawCenteredString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.transport.title"), width / 2,
                layout.panelY + 8, TEXT_COLOR);
        TransportStateSnapshot snapshot = currentSnapshot();
        if (snapshot == null) {
            drawCenteredString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.transport.loading"), width / 2,
                    (layout.panelY + layout.panelBottom) / 2, TEXT_COLOR);
        } else {
            drawCurrentStation(snapshot);
            drawNodeList(snapshot);
        }
        if (villageConfirmationOpen && snapshot != null) {
            drawVillageConfirmation(snapshot);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!villageConfirmationOpen) {
            renameField.drawTextBox();
        }
        if (snapshot != null && !villageConfirmationOpen) {
            drawNodeTooltip(snapshot.getNodes(), mouseX, mouseY);
        }
    }

    private void drawCurrentStation(TransportStateSnapshot snapshot) {
        String alias = snapshot.getCurrentAlias().isEmpty()
                ? I18n.format("gui.lisbam_pastoral_economy.transport.unnamed") : snapshot.getCurrentAlias();
        String coins = I18n.format("gui.lisbam_pastoral_economy.transport.coins", format(snapshot.getCoins()));
        String current = I18n.format("gui.lisbam_pastoral_economy.transport.current", alias);
        drawString(fontRenderer, fontRenderer.trimStringToWidth(current,
                layout.contentRight - layout.contentX - fontRenderer.getStringWidth(coins) - 10),
                layout.contentX, layout.panelY + 22, TEXT_COLOR);
        drawTrimmed(I18n.format("gui.lisbam_pastoral_economy.transport.position",
                Integer.toString(snapshot.getX()), Integer.toString(snapshot.getY()), Integer.toString(snapshot.getZ())),
                layout.contentX, layout.panelY + 34, TEXT_COLOR);
        drawString(fontRenderer, coins, layout.contentRight - fontRenderer.getStringWidth(coins), layout.panelY + 22,
                TEXT_COLOR);
        String status;
        if (!snapshot.currentStationExists()) {
            status = I18n.format("gui.lisbam_pastoral_economy.transport.invalid");
        } else if (!snapshot.isCurrentDimensionSupported()) {
            status = I18n.format("gui.lisbam_pastoral_economy.transport.dimension_unsupported");
        } else if (snapshot.isCurrentActive()) {
            status = I18n.format("gui.lisbam_pastoral_economy.transport.active");
        } else if (snapshot.isStarterFree()) {
            status = I18n.format("gui.lisbam_pastoral_economy.transport.free");
        } else {
            status = I18n.format("gui.lisbam_pastoral_economy.transport.fee", format(snapshot.getConnectionFee()));
        }
        drawTrimmed(I18n.format("gui.lisbam_pastoral_economy.transport.status", status), layout.contentX,
                layout.panelY + 46, TEXT_COLOR);
        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.transport.nodes"), layout.contentX,
                layout.listY - 14, TEXT_COLOR);
    }

    private void drawNodeList(TransportStateSnapshot snapshot) {
        List<TransportNodeView> nodes = snapshot.getNodes();
        for (int row = 0; row < visibleRows(); row++) {
            int index = scrollOffset + row;
            if (index >= nodes.size()) {
                break;
            }
            TransportNodeView node = nodes.get(index);
            int y = layout.listY + row * ROW_HEIGHT;
            String location = node.getX() + "," + node.getY() + "," + node.getZ();
            String fee = node.hasTravelFee()
                    ? I18n.format("gui.lisbam_pastoral_economy.transport.travel_fee", format(node.getTravelFee())) : "";
            int feeWidth = fee.isEmpty() ? 0 : fontRenderer.getStringWidth(fee);
            int textWidth = layout.contentRight - layout.contentX - feeWidth - (feeWidth == 0 ? 0 : 6);
            int color = index == selectedIndex ? 0xFF55AA55 : TEXT_COLOR;
            drawString(fontRenderer, fontRenderer.trimStringToWidth(node.getAlias() + "  " + location,
                    Math.max(1, textWidth)), layout.contentX, y, color);
            if (!fee.isEmpty()) {
                drawString(fontRenderer, fee, layout.contentRight - feeWidth, y, color);
            }
        }
    }

    private void drawNodeTooltip(List<TransportNodeView> nodes, int mouseX, int mouseY) {
        if (mouseX < layout.contentX || mouseX >= layout.contentRight || mouseY < layout.listY
                || mouseY >= layout.footerY - 4) {
            return;
        }
        int row = (mouseY - layout.listY) / ROW_HEIGHT;
        int index = scrollOffset + row;
        if (row < 0 || row >= visibleRows() || index >= nodes.size()) {
            return;
        }
        TransportNodeView node = nodes.get(index);
        List<String> tooltip = new ArrayList<String>();
        tooltip.add(node.getAlias());
        tooltip.add(node.getX() + ", " + node.getY() + ", " + node.getZ());
        tooltip.add(node.hasTravelFee()
                ? I18n.format("gui.lisbam_pastoral_economy.transport.travel_fee", format(node.getTravelFee()))
                : I18n.format("gui.lisbam_pastoral_economy.transport.travel_unavailable"));
        drawHoveringText(tooltip, mouseX, mouseY);
    }

    private void updateControls(TransportStateSnapshot snapshot) {
        if (snapshot == null) {
            villageConfirmationOpen = false;
            setEnabled(BUTTON_CONNECT, false);
            setEnabled(BUTTON_REMOVE, false);
            setEnabled(BUTTON_RENAME, false);
            setEnabled(BUTTON_PREVIOUS, false);
            setEnabled(BUTTON_NEXT, false);
            setEnabled(BUTTON_REFRESH, false);
            setEnabled(BUTTON_CONNECT_VILLAGE, false);
            setEnabled(BUTTON_TRAVEL, false);
            setEnabled(BUTTON_CONFIRM_VILLAGE, false);
            setEnabled(BUTTON_CANCEL_VILLAGE, false);
            setVisible(BUTTON_CONFIRM_VILLAGE, false);
            setVisible(BUTTON_CANCEL_VILLAGE, false);
            setMainButtonsVisible(true);
            setButtonText(BUTTON_TRAVEL, I18n.format("gui.lisbam_pastoral_economy.transport.travel"));
            renameField.setEnabled(false);
            return;
        }
        List<TransportNodeView> nodes = snapshot.getNodes();
        if (selectedIndex >= nodes.size()) {
            selectedIndex = nodes.isEmpty() ? -1 : nodes.size() - 1;
        }
        scrollOffset = Math.min(scrollOffset, maxScroll(nodes));
        setEnabled(BUTTON_CONNECT, snapshot.currentStationExists() && snapshot.isCurrentDimensionSupported()
                && !snapshot.isCurrentActive());
        setEnabled(BUTTON_REFRESH, snapshot.currentStationExists());
        boolean canConnectVillage = snapshot.getNearestVillage() != null && snapshot.currentStationExists()
                && snapshot.isCurrentActive() && snapshot.isCurrentDimensionSupported();
        if (villageConfirmationOpen && !canConnectVillage) {
            villageConfirmationOpen = false;
        }
        setEnabled(BUTTON_CONNECT_VILLAGE, canConnectVillage);
        TransportNodeView selected = selectedNode(nodes);
        setEnabled(BUTTON_REMOVE, selected != null && selected.isActive());
        setEnabled(BUTTON_RENAME, selected != null);
        boolean canTravel = selected != null && selected.hasTravelFee() && snapshot.getCoins() >= selected.getTravelFee();
        setEnabled(BUTTON_TRAVEL, canTravel);
        String travelLabel = I18n.format("gui.lisbam_pastoral_economy.transport.travel");
        if (selected != null && selected.hasTravelFee()) {
            travelLabel += " (" + format(selected.getTravelFee()) + ")";
        }
        setButtonText(BUTTON_TRAVEL, travelLabel);
        setEnabled(BUTTON_PREVIOUS, scrollOffset > 0);
        setEnabled(BUTTON_NEXT, scrollOffset < maxScroll(nodes));
        renameField.setEnabled(selected != null);
        setEnabled(BUTTON_CONFIRM_VILLAGE, villageConfirmationOpen && canConnectVillage
                && snapshot.getCoins() >= snapshot.getNearestVillage().getConnectionFee());
        setEnabled(BUTTON_CANCEL_VILLAGE, villageConfirmationOpen);
        setVisible(BUTTON_CONFIRM_VILLAGE, villageConfirmationOpen);
        setVisible(BUTTON_CANCEL_VILLAGE, villageConfirmationOpen);
        setMainButtonsVisible(!villageConfirmationOpen);
    }

    private void requestVillageCandidate(TransportStateSnapshot snapshot) {
        if (!villageLookupRequested && snapshot != null && snapshot.hasCurrentStation()
                && snapshot.currentStationExists() && snapshot.isCurrentActive()
                && snapshot.isCurrentDimensionSupported()) {
            villageLookupRequested = true;
            send(TransportAction.FIND_VILLAGE, snapshot.getCurrentStationId(), "");
        }
    }

    private void drawVillageConfirmation(TransportStateSnapshot snapshot) {
        VillageTransportCandidate candidate = snapshot.getNearestVillage();
        if (candidate == null) {
            return;
        }
        drawNativePanel(layout.dialogX, layout.dialogY, layout.dialogRight, layout.dialogBottom);
        drawCenteredString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.transport.confirm_village"),
                layout.dialogX + layout.dialogWidth / 2, layout.dialogY + 8, TEXT_COLOR);
        drawCenteredString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.transport.village_distance",
                format(candidate.getDistance())), layout.dialogX + layout.dialogWidth / 2, layout.dialogY + 24, TEXT_COLOR);
        drawCenteredString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.transport.village_fee",
                format(candidate.getConnectionFee())), layout.dialogX + layout.dialogWidth / 2, layout.dialogY + 36, TEXT_COLOR);
    }

    private void setMainButtonsVisible(boolean visible) {
        setVisible(BUTTON_CONNECT, visible);
        setVisible(BUTTON_REMOVE, visible);
        setVisible(BUTTON_RENAME, visible);
        setVisible(BUTTON_PREVIOUS, visible);
        setVisible(BUTTON_NEXT, visible);
        setVisible(BUTTON_REFRESH, visible);
        setVisible(BUTTON_CONNECT_VILLAGE, visible);
        setVisible(BUTTON_TRAVEL, visible);
    }

    private void send(TransportAction action, java.util.UUID stationId, String alias) {
        ModNetwork.CHANNEL.sendToServer(new TransportStationActionMessage(action, stationId, alias));
    }

    private void setEnabled(int id, boolean enabled) {
        for (GuiButton button : buttonList) {
            if (button.id == id) {
                button.enabled = enabled;
            }
        }
    }

    private void setVisible(int id, boolean visible) {
        for (GuiButton button : buttonList) {
            if (button.id == id) {
                button.visible = visible;
            }
        }
    }

    private void setButtonText(int id, String text) {
        for (GuiButton button : buttonList) {
            if (button.id == id) {
                button.displayString = text;
            }
        }
    }

    private TransportNodeView selectedNode(List<TransportNodeView> nodes) {
        return selectedIndex >= 0 && selectedIndex < nodes.size() ? nodes.get(selectedIndex) : null;
    }

    private int visibleRows() {
        return Math.max(2, Math.max(0, (layout.footerY - layout.listY - 4) / ROW_HEIGHT));
    }

    private int maxScroll(List<TransportNodeView> nodes) {
        return Math.max(0, nodes.size() - visibleRows());
    }

    private TransportStateSnapshot currentSnapshot() {
        TransportStateSnapshot snapshot = ClientTransportState.get();
        return snapshot != null && snapshot.getX() == stationPosition.getX()
                && snapshot.getY() == stationPosition.getY() && snapshot.getZ() == stationPosition.getZ() ? snapshot : null;
    }

    private void drawTrimmed(String text, int x, int y, int color) {
        if (!text.isEmpty()) {
            drawString(fontRenderer, fontRenderer.trimStringToWidth(text, layout.contentRight - x), x, y, color);
        }
    }

    private static String format(long value) {
        return String.format(java.util.Locale.ROOT, "%,d", value);
    }

    /** Draw the complete vanilla panel in one operation; never assemble it from texture fragments. */
    private void drawNativePanel(int left, int top, int right, int bottom) {
        mc.getTextureManager().bindTexture(VANILLA_PANEL_TEXTURE);
        drawScaledCustomSizeModalRect(left, top, 0.0F, 0.0F, 248, 166,
                right - left, bottom - top, 256.0F, 256.0F);
    }

    /** Retains vanilla button behaviour while drawing labels in the standard light button colour. */
    private static final class LightTextButton extends GuiButton {
        private LightTextButton(int buttonId, int x, int y, int width, int height, String text) {
            super(buttonId, x, y, width, height, text);
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
            super.drawButton(minecraft, mouseX, mouseY, partialTicks);
            if (visible) {
                drawCenteredString(minecraft.fontRenderer, displayString, x + width / 2, y + (height - 8) / 2,
                        BUTTON_TEXT_COLOR);
            }
        }
    }

    /** All action controls fit inside even Minecraft's 320x240 scaled screen. */
    private static final class Layout {
        private final int panelX;
        private final int panelY;
        private final int panelRight;
        private final int panelBottom;
        private final int contentX;
        private final int contentRight;
        private final int actionRowOneY;
        private final int actionRowTwoY;
        private final int listY;
        private final int footerY;
        private final int connectX;
        private final int connectWidth;
        private final int refreshX;
        private final int refreshWidth;
        private final int travelX;
        private final int travelWidth;
        private final int previousX;
        private final int nextX;
        private final int renameFieldX;
        private final int renameFieldWidth;
        private final int renameX;
        private final int dialogX;
        private final int dialogY;
        private final int dialogWidth;
        private final int dialogRight;
        private final int dialogBottom;
        private final int dialogButtonY;
        private final int dialogButtonWidth;
        private final int dialogConfirmX;
        private final int dialogCancelX;

        private Layout(int panelX, int panelY, int panelWidth, int panelHeight) {
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelRight = panelX + panelWidth;
            this.panelBottom = panelY + panelHeight;
            this.contentX = panelX + 6;
            this.contentRight = panelRight - 6;
            this.actionRowOneY = panelY + 62;
            this.actionRowTwoY = panelY + 84;
            this.listY = panelY + 112;
            this.footerY = panelBottom - 22;
            this.refreshWidth = 64;
            this.travelWidth = 90;
            this.connectWidth = Math.max(72, contentRight - contentX - refreshWidth - travelWidth - 8);
            this.connectX = contentX;
            this.refreshX = connectX + connectWidth + 4;
            this.travelX = contentRight - travelWidth;
            this.previousX = contentRight - 64;
            this.nextX = contentRight - 30;
            this.renameX = contentRight - 74;
            this.renameFieldX = contentX + 88;
            this.renameFieldWidth = Math.max(20, renameX - renameFieldX - 4);
            this.dialogWidth = Math.max(180, Math.min(240, contentRight - contentX - 16));
            this.dialogX = (contentX + contentRight - dialogWidth) / 2;
            this.dialogY = panelY + 66;
            this.dialogRight = dialogX + dialogWidth;
            this.dialogBottom = dialogY + 76;
            this.dialogButtonY = dialogY + 54;
            this.dialogButtonWidth = Math.max(40, (dialogWidth - 36) / 2);
            this.dialogConfirmX = dialogX + 12;
            this.dialogCancelX = dialogConfirmX + dialogButtonWidth + 12;
        }

        private static Layout create(int screenWidth, int screenHeight) {
            int panelWidth = Math.max(1, Math.min(520, screenWidth - 12));
            int panelHeight = Math.max(1, Math.min(300, screenHeight - 12));
            return new Layout((screenWidth - panelWidth) / 2, (screenHeight - panelHeight) / 2, panelWidth, panelHeight);
        }
    }
}
