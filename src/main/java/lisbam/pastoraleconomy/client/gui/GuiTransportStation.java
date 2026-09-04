package lisbam.pastoraleconomy.client.gui;

import lisbam.pastoraleconomy.client.ClientTransportState;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.TransportStationActionMessage;
import lisbam.pastoraleconomy.transport.TransportAction;
import lisbam.pastoraleconomy.transport.TransportNodeView;
import lisbam.pastoraleconomy.transport.TransportStateSnapshot;
import lisbam.pastoraleconomy.transport.VillageTransportCandidate;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private static final int BUTTON_CONFIRM = 10;
    private static final int BUTTON_CANCEL = 11;
    private static final int ROW_HEIGHT = 14;
    private static final ResourceLocation VANILLA_PANEL_TEXTURE = new ResourceLocation(
            "minecraft", "textures/gui/demo_background.png");
    /** Vanilla workbench/furnace inventory-title colour for static window text. */
    private static final int VANILLA_CONTAINER_TEXT_COLOR = 4210752;

    private final BlockPos stationPosition;
    private GuiTextField renameField;
    private int selectedIndex = -1;
    private int scrollOffset;
    private boolean villageLookupRequested;
    private ConfirmationType confirmationType = ConfirmationType.NONE;
    private UUID removalStationId;
    private Layout layout;

    public GuiTransportStation(BlockPos stationPosition) {
        this.stationPosition = stationPosition.toImmutable();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        layout = Layout.create(width, height);
        buttonList.add(new GuiButton(BUTTON_CONNECT, layout.connectX, layout.actionRowOneY, layout.connectWidth, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.connect")));
        buttonList.add(new GuiButton(BUTTON_REFRESH, layout.refreshX, layout.actionRowOneY, layout.refreshWidth, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.refresh")));
        buttonList.add(new GuiButton(BUTTON_TRAVEL, layout.travelX, layout.actionRowOneY, layout.travelWidth, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.travel")));
        buttonList.add(new GuiButton(BUTTON_CONNECT_VILLAGE, layout.contentX, layout.actionRowTwoY,
                layout.contentRight - layout.contentX, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.connect_village")));
        buttonList.add(new GuiButton(BUTTON_PREVIOUS, layout.previousX, layout.listY - 16, 30, 14, "<"));
        buttonList.add(new GuiButton(BUTTON_NEXT, layout.nextX, layout.listY - 16, 30, 14, ">"));
        buttonList.add(new GuiButton(BUTTON_REMOVE, layout.contentX, layout.footerY, 82, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.remove")));
        buttonList.add(new GuiButton(BUTTON_RENAME, layout.renameX, layout.footerY, 74, 18,
                I18n.format("gui.lisbam_pastoral_economy.transport.rename")));
        renameField = new GuiTextField(0, fontRenderer, layout.renameFieldX, layout.footerY + 2,
                layout.renameFieldWidth, 14);
        renameField.setMaxStringLength(96);
        renameField.setTextColor(VANILLA_CONTAINER_TEXT_COLOR);
        renameField.setDisabledTextColour(VANILLA_CONTAINER_TEXT_COLOR);
        buttonList.add(new GuiButton(BUTTON_CONFIRM, layout.dialogConfirmX, layout.dialogButtonY,
                layout.dialogButtonWidth, 18, I18n.format("gui.lisbam_pastoral_economy.transport.confirm")));
        buttonList.add(new GuiButton(BUTTON_CANCEL, layout.dialogCancelX, layout.dialogButtonY,
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
        if (button.id == BUTTON_CANCEL) {
            closeConfirmation();
            updateControls(snapshot);
            return;
        }
        if (snapshot == null || !snapshot.hasCurrentStation()) {
            return;
        }
        if (button.id == BUTTON_CONFIRM) {
            if (!button.enabled) {
                return;
            }
            if (confirmationType == ConfirmationType.CONNECT_VILLAGE) {
                VillageTransportCandidate candidate = snapshot.getNearestVillage();
                if (candidate != null && snapshot.getCoins() >= candidate.getConnectionFee()) {
                    send(TransportAction.CONNECT_VILLAGE, candidate.getVillageId(), "");
                    villageLookupRequested = false;
                    closeConfirmation();
                }
            } else if (confirmationType == ConfirmationType.REMOVE_NODE) {
                TransportNodeView pendingNode = findNode(snapshot.getNodes(), removalStationId);
                if (pendingNode != null && pendingNode.isActive()) {
                    send(TransportAction.REMOVE, pendingNode.getStationId(), "");
                }
                closeConfirmation();
            }
            updateControls(snapshot);
        } else if (button.id == BUTTON_CONNECT) {
            send(TransportAction.CONNECT, snapshot.getCurrentStationId(), "");
        } else if (button.id == BUTTON_REFRESH) {
            send(TransportAction.REFRESH, snapshot.getCurrentStationId(), "");
        } else if (button.id == BUTTON_CONNECT_VILLAGE) {
            if (snapshot.getNearestVillage() != null) {
                confirmationType = ConfirmationType.CONNECT_VILLAGE;
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
                removalStationId = selected.getStationId();
                confirmationType = ConfirmationType.REMOVE_NODE;
                updateControls(snapshot);
            } else if (button.id == BUTTON_RENAME) {
                send(TransportAction.RENAME, selected.getStationId(), renameField.getText());
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (isConfirmationOpen()) {
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
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        if (isConfirmationOpen() || layout == null) {
            return;
        }
        int wheelDelta = Mouse.getEventDWheel();
        if (wheelDelta == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (mouseX < layout.contentX || mouseX >= layout.contentRight || mouseY < layout.listY
                || mouseY >= layout.footerY - 4) {
            return;
        }
        TransportStateSnapshot snapshot = currentSnapshot();
        if (snapshot == null) {
            return;
        }
        int direction = wheelDelta > 0 ? -1 : 1;
        scrollOffset = Math.max(0, Math.min(maxScroll(snapshot.getNodes()), scrollOffset + direction));
        updateControls(snapshot);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (isConfirmationOpen()) {
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
        drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.title"), width / 2,
                layout.panelY + 8);
        TransportStateSnapshot snapshot = currentSnapshot();
        if (snapshot == null) {
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.loading"), width / 2,
                    (layout.panelY + layout.panelBottom) / 2);
        } else {
            drawCurrentStation(snapshot);
            drawNodeList(snapshot);
        }
        if (isConfirmationOpen() && snapshot != null) {
            drawConfirmation(snapshot);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!isConfirmationOpen()) {
            renameField.drawTextBox();
        }
        if (snapshot != null && !isConfirmationOpen()) {
            drawNodeTooltip(snapshot.getNodes(), mouseX, mouseY);
        }
    }

    private void drawCurrentStation(TransportStateSnapshot snapshot) {
        String alias = snapshot.getCurrentAlias().isEmpty()
                ? I18n.format("gui.lisbam_pastoral_economy.transport.unnamed") : snapshot.getCurrentAlias();
        String coins = I18n.format("gui.lisbam_pastoral_economy.transport.coins", format(snapshot.getCoins()));
        String current = I18n.format("gui.lisbam_pastoral_economy.transport.current", alias);
        drawContainerText(fontRenderer.trimStringToWidth(current,
                layout.contentRight - layout.contentX - fontRenderer.getStringWidth(coins) - 10),
                layout.contentX, layout.panelY + 22);
        drawTrimmed(I18n.format("gui.lisbam_pastoral_economy.transport.position",
                Integer.toString(snapshot.getX()), Integer.toString(snapshot.getY()), Integer.toString(snapshot.getZ())),
                layout.contentX, layout.panelY + 34);
        drawContainerText(coins, layout.contentRight - fontRenderer.getStringWidth(coins), layout.panelY + 22);
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
                layout.panelY + 46);
        drawContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.nodes"), layout.contentX,
                layout.listY - 14);
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
            String prefix = index == selectedIndex ? "> " : "  ";
            drawContainerText(fontRenderer.trimStringToWidth(prefix + node.getAlias() + "  " + location,
                    Math.max(1, textWidth)), layout.contentX, y);
            if (!fee.isEmpty()) {
                drawContainerText(fee, layout.contentRight - feeWidth, y);
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
            closeConfirmation();
            setEnabled(BUTTON_CONNECT, false);
            setEnabled(BUTTON_REMOVE, false);
            setEnabled(BUTTON_RENAME, false);
            setEnabled(BUTTON_PREVIOUS, false);
            setEnabled(BUTTON_NEXT, false);
            setEnabled(BUTTON_REFRESH, false);
            setEnabled(BUTTON_CONNECT_VILLAGE, false);
            setEnabled(BUTTON_TRAVEL, false);
            setEnabled(BUTTON_CONFIRM, false);
            setEnabled(BUTTON_CANCEL, false);
            setVisible(BUTTON_CONFIRM, false);
            setVisible(BUTTON_CANCEL, false);
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
        if (confirmationType == ConfirmationType.CONNECT_VILLAGE && !canConnectVillage) {
            closeConfirmation();
        }
        setEnabled(BUTTON_CONNECT_VILLAGE, canConnectVillage);
        TransportNodeView selected = selectedNode(nodes);
        boolean canRemove = selected != null && selected.isActive();
        if (confirmationType == ConfirmationType.REMOVE_NODE) {
            TransportNodeView pendingNode = findNode(nodes, removalStationId);
            if (pendingNode == null || !pendingNode.isActive()) {
                closeConfirmation();
            }
        }
        setEnabled(BUTTON_REMOVE, canRemove);
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
        boolean canConfirmVillage = confirmationType == ConfirmationType.CONNECT_VILLAGE && canConnectVillage
                && snapshot.getCoins() >= snapshot.getNearestVillage().getConnectionFee();
        TransportNodeView removalNode = findNode(nodes, removalStationId);
        boolean canConfirmRemoval = confirmationType == ConfirmationType.REMOVE_NODE
                && removalNode != null && removalNode.isActive();
        setEnabled(BUTTON_CONFIRM, canConfirmVillage || canConfirmRemoval);
        setEnabled(BUTTON_CANCEL, isConfirmationOpen());
        setVisible(BUTTON_CONFIRM, isConfirmationOpen());
        setVisible(BUTTON_CANCEL, isConfirmationOpen());
        setButtonText(BUTTON_CONFIRM, I18n.format(confirmationType == ConfirmationType.REMOVE_NODE
                ? "gui.lisbam_pastoral_economy.transport.remove_confirm"
                : "gui.lisbam_pastoral_economy.transport.confirm"));
        setMainButtonsVisible(!isConfirmationOpen());
    }

    private void requestVillageCandidate(TransportStateSnapshot snapshot) {
        if (!villageLookupRequested && snapshot != null && snapshot.hasCurrentStation()
                && snapshot.currentStationExists() && snapshot.isCurrentActive()
                && snapshot.isCurrentDimensionSupported()) {
            villageLookupRequested = true;
            send(TransportAction.FIND_VILLAGE, snapshot.getCurrentStationId(), "");
        }
    }

    private void drawConfirmation(TransportStateSnapshot snapshot) {
        drawNativePanel(layout.dialogX, layout.dialogY, layout.dialogRight, layout.dialogBottom);
        int centerX = layout.dialogX + layout.dialogWidth / 2;
        if (confirmationType == ConfirmationType.CONNECT_VILLAGE) {
            VillageTransportCandidate candidate = snapshot.getNearestVillage();
            if (candidate == null) {
                return;
            }
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.confirm_village"), centerX,
                    layout.dialogY + 12);
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.confirm_notice"), centerX,
                    layout.dialogY + 30);
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.village_distance",
                    format(candidate.getDistance())), centerX, layout.dialogY + 46);
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.village_fee",
                    format(candidate.getConnectionFee())), centerX, layout.dialogY + 60);
        } else if (confirmationType == ConfirmationType.REMOVE_NODE) {
            TransportNodeView node = findNode(snapshot.getNodes(), removalStationId);
            if (node == null) {
                return;
            }
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.confirm_remove"), centerX,
                    layout.dialogY + 12);
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.remove_target",
                    fontRenderer.trimStringToWidth(node.getAlias(), layout.dialogWidth - 24)), centerX, layout.dialogY + 34);
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.transport.remove_warning"), centerX,
                    layout.dialogY + 56);
        }
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

    private TransportNodeView findNode(List<TransportNodeView> nodes, UUID stationId) {
        if (stationId == null) {
            return null;
        }
        for (TransportNodeView node : nodes) {
            if (stationId.equals(node.getStationId())) {
                return node;
            }
        }
        return null;
    }

    private boolean isConfirmationOpen() {
        return confirmationType != ConfirmationType.NONE;
    }

    private void closeConfirmation() {
        confirmationType = ConfirmationType.NONE;
        removalStationId = null;
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

    private void drawTrimmed(String text, int x, int y) {
        if (!text.isEmpty()) {
            drawContainerText(fontRenderer.trimStringToWidth(text, layout.contentRight - x), x, y);
        }
    }

    /** Matches GuiCrafting/GuiFurnace: direct FontRenderer with 4210752 and no shadow pass. */
    private void drawContainerText(String text, int x, int y) {
        fontRenderer.drawString(text, x, y, VANILLA_CONTAINER_TEXT_COLOR);
    }

    private void drawCenteredContainerText(String text, int centerX, int y) {
        drawContainerText(text, centerX - fontRenderer.getStringWidth(text) / 2, y);
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

    /** All controls fit inside Minecraft's 320x240 scaled screen without oversized empty space. */
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
            // Keep the list heading wholly below the second action row. Its
            // former baseline overlapped the village-connection button.
            this.listY = panelY + 122;
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
            this.dialogWidth = Math.max(180, Math.min(224, contentRight - contentX - 12));
            this.dialogX = (contentX + contentRight - dialogWidth) / 2;
            int dialogHeight = dialogWidth * 166 / 248;
            this.dialogY = panelY + (panelHeight - dialogHeight) / 2;
            this.dialogRight = dialogX + dialogWidth;
            this.dialogBottom = dialogY + dialogHeight;
            this.dialogButtonY = dialogBottom - 26;
            this.dialogButtonWidth = Math.max(40, (dialogWidth - 36) / 2);
            this.dialogConfirmX = dialogX + 12;
            this.dialogCancelX = dialogConfirmX + dialogButtonWidth + 12;
        }

        private static Layout create(int screenWidth, int screenHeight) {
            int availableWidth = Math.max(1, screenWidth - 12);
            int availableHeight = Math.max(1, screenHeight - 12);
            int panelWidth = Math.min(400, availableWidth);
            int preferredHeight = Math.max(206, panelWidth * 166 / 248);
            int panelHeight = Math.min(availableHeight, preferredHeight);
            return new Layout((screenWidth - panelWidth) / 2, (screenHeight - panelHeight) / 2, panelWidth, panelHeight);
        }
    }

    private enum ConfirmationType {
        NONE,
        CONNECT_VILLAGE,
        REMOVE_NODE
    }
}
