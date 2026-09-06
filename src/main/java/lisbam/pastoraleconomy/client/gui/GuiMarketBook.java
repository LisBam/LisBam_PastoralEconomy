package lisbam.pastoraleconomy.client.gui;

import lisbam.pastoraleconomy.client.ClientMarketState;
import lisbam.pastoraleconomy.gui.ContainerMarketBook;
import lisbam.pastoraleconomy.market.MarketCatalog;
import lisbam.pastoraleconomy.market.MarketCommodity;
import lisbam.pastoraleconomy.market.MarketHistoryPoint;
import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import lisbam.pastoraleconomy.market.MarketTrend;
import lisbam.pastoraleconomy.merchant.TradeCatalog;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.RequestMarketHistoryMessage;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Client-only, display-only market book with a bounded historical line chart. */
public final class GuiMarketBook extends GuiContainer {
    private static final int BUTTON_SELL_PAGE = 1;
    private static final int BUTTON_BUY_PAGE = 2;
    private static final int BUTTON_SCROLL_UP = 3;
    private static final int BUTTON_SCROLL_DOWN = 4;
    private static final int SELECTOR_GOOD_BUTTON_OFFSET = 100;
    private static final int SELECTOR_COLUMNS = 3;
    private static final int SNAPSHOT_RETRY_TICKS = 40;
    private static final ResourceLocation VANILLA_PANEL_TEXTURE = new ResourceLocation(
            "minecraft", "textures/gui/demo_background.png");
    private static final List<MarketCommodity> SELL_GOODS = MarketCatalog.getSellHistoryTracked();
    private static final List<MarketCommodity> BUY_GOODS = collectBuyGoods();

    private MarketCommodity selectedCommodity = SELL_GOODS.get(0);
    private int activeRequestId;
    private boolean initialRequestSent;
    private boolean waitingForSnapshot;
    private boolean buyPage;
    private int prefetchIndex;
    private int selectorScrollRow;
    private long nextPrefetchTick;
    private long retrySnapshotAtTick;
    private String buySearchQuery = "";
    private GuiButton sellPageButton;
    private GuiButton buyPageButton;
    private GuiButton scrollUpButton;
    private GuiButton scrollDownButton;
    private GuiTextField searchField;
    private Layout layout;
    private MarketHistorySnapshot graphSnapshot;
    private Layout graphLayout;
    private List<GraphNode> graphNodes = Collections.emptyList();
    private double graphDisplayMinimum;
    private double graphDisplayMaximum;

    public GuiMarketBook() {
        super(new ContainerMarketBook());
    }

    @Override
    public void initGui() {
        // GuiContainer installs this screen's dedicated client container before
        // Forge writes the server window id into EntityPlayer#openContainer.
        super.initGui();
        if (!initialRequestSent) {
            ClientMarketState.beginBookSession();
        }
        buttonList.clear();
        layout = Layout.create(width, height);
        sellPageButton = new GuiButton(BUTTON_SELL_PAGE, layout.panelX + 6, layout.panelY + 20, 62, 14,
                I18n.format("gui.lisbam_pastoral_economy.market_book.sell_page"));
        buyPageButton = new GuiButton(BUTTON_BUY_PAGE, layout.panelX + 70, layout.panelY + 20, 62, 14,
                I18n.format("gui.lisbam_pastoral_economy.market_book.buy_page"));
        buttonList.add(sellPageButton);
        buttonList.add(buyPageButton);
        if (buyPage) {
            searchField = new GuiTextField(0, fontRenderer, layout.searchX, layout.searchY,
                    layout.searchWidth, layout.searchHeight);
            searchField.setMaxStringLength(48);
            searchField.setText(buySearchQuery);
        } else {
            searchField = null;
        }
        scrollUpButton = new GuiButton(BUTTON_SCROLL_UP, layout.cropListX, layout.scrollButtonY,
                layout.scrollButtonWidth, layout.scrollButtonHeight, "↑");
        scrollDownButton = new GuiButton(BUTTON_SCROLL_DOWN,
                layout.cropListX + layout.scrollButtonWidth + 3, layout.scrollButtonY,
                layout.scrollButtonWidth, layout.scrollButtonHeight, "↓");
        buttonList.add(scrollUpButton);
        buttonList.add(scrollDownButton);
        addSelectorButtons();
        if (!initialRequestSent) {
            initialRequestSent = true;
            requestWindow(true);
        } else {
            if (getActiveSnapshot() == null) {
                requestWindow(true);
            } else {
                updateNavigation(getActiveSnapshot());
            }
        }
    }

    /** Only visible selector cells become buttons; the full catalogue is never laid over the chart. */
    private void addSelectorButtons() {
        List<MarketCommodity> goods = visibleGoods();
        int first = selectorScrollRow * SELECTOR_COLUMNS;
        int maximum = Math.min(goods.size(), first + layout.selectorRows * SELECTOR_COLUMNS);
        for (int index = first; index < maximum; index++) {
            int visibleIndex = index - first;
            int column = visibleIndex % SELECTOR_COLUMNS;
            int row = visibleIndex / SELECTOR_COLUMNS;
            int buttonX = layout.cropListX + column * (layout.cropButtonWidth + layout.cropColumnGap);
            int buttonY = layout.cropListY + row * (layout.cropButtonHeight + layout.cropRowGap);
            GuiButton button = new GuiButton(SELECTOR_GOOD_BUTTON_OFFSET + visibleIndex, buttonX, buttonY,
                    layout.cropButtonWidth, layout.cropButtonHeight, "");
            button.enabled = !goods.get(index).getKey().equals(selectedCommodity.getKey());
            buttonList.add(button);
        }
    }

    private void rebuildSelectorButtons() {
        for (int index = buttonList.size() - 1; index >= 0; index--) {
            GuiButton button = buttonList.get(index);
            if (button.id >= SELECTOR_GOOD_BUTTON_OFFSET) {
                buttonList.remove(index);
            }
        }
        addSelectorButtons();
        updateNavigation(getActiveSnapshot());
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        if (button.id == BUTTON_SELL_PAGE || button.id == BUTTON_BUY_PAGE) {
            buyPage = button.id == BUTTON_BUY_PAGE;
            prefetchIndex = 0;
            selectorScrollRow = 0;
            List<MarketCommodity> goods = visibleGoods();
            selectedCommodity = goods.isEmpty() ? SELL_GOODS.get(0) : goods.get(0);
            initGui();
            return;
        }
        if (button.id == BUTTON_SCROLL_UP) {
            scrollSelectors(-1);
            return;
        }
        if (button.id == BUTTON_SCROLL_DOWN) {
            scrollSelectors(1);
            return;
        }
        if (button.id >= SELECTOR_GOOD_BUTTON_OFFSET) {
            int visibleIndex = button.id - SELECTOR_GOOD_BUTTON_OFFSET;
            int selectedIndex = selectorScrollRow * SELECTOR_COLUMNS + visibleIndex;
            List<MarketCommodity> goods = visibleGoods();
            if (selectedIndex >= 0 && selectedIndex < goods.size()) {
                selectCommodity(goods.get(selectedIndex));
            }
            return;
        }
    }

    private void selectCommodity(MarketCommodity commodity) {
        if (commodity == null || commodity.getKey().equals(selectedCommodity.getKey())) {
            return;
        }
        selectedCommodity = commodity;
        if (!useCachedNewestWindow()) {
            requestWindow(true);
        }
        rebuildSelectorButtons();
    }

    private void scrollSelectors(int rowDelta) {
        int maximum = Math.max(0, selectorRowCount(visibleGoods()) - layout.selectorRows);
        int updated = Math.max(0, Math.min(maximum, selectorScrollRow + rowDelta));
        if (updated != selectorScrollRow) {
            selectorScrollRow = updated;
            rebuildSelectorButtons();
        }
    }

    private void requestWindow(boolean forceRequest) {
        activeRequestId = nextRequestId();
        waitingForSnapshot = true;
        retrySnapshotAtTick = currentClientTick() + SNAPSHOT_RETRY_TICKS;
        updateNavigation(null);
        if (forceRequest) {
            ModNetwork.CHANNEL.sendToServer(new RequestMarketHistoryMessage(
                    selectedCommodity.getKey(),
                    -1L,
                    activeRequestId
            ));
        }
    }

    private int nextRequestId() {
        return ClientMarketState.nextRequestId();
    }

    @Override
    public void updateScreen() {
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
        MarketHistorySnapshot snapshot = getActiveSnapshot();
        if (snapshot != null) {
            waitingForSnapshot = false;
        } else if (waitingForSnapshot && currentClientTick() >= retrySnapshotAtTick) {
            // The server intentionally coalesces bursty display requests. A
            // dropped/coalesced request must therefore never leave this book
            // permanently on its loading message.
            requestWindow(true);
        }
        updateNavigation(snapshot);
        if (!waitingForSnapshot && mc.world != null && mc.world.getTotalWorldTime() >= nextPrefetchTick) {
            List<MarketCommodity> goods = pageGoods();
            while (prefetchIndex < goods.size()
                    && (goods.get(prefetchIndex).getKey().equals(selectedCommodity.getKey())
                    || ClientMarketState.getLatestSnapshot(goods.get(prefetchIndex).getKey()) != null)) {
                prefetchIndex++;
            }
            if (prefetchIndex < goods.size()) {
                MarketCommodity commodity = goods.get(prefetchIndex++);
                ModNetwork.CHANNEL.sendToServer(new RequestMarketHistoryMessage(commodity.getKey(), -1L,
                        ClientMarketState.nextRequestId()));
                nextPrefetchTick = mc.world.getTotalWorldTime() + 4L;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (ModGuiInput.closeWithExitKey(mc, keyCode, searchField != null && searchField.isFocused())) {
            return;
        }
        if (searchField != null && searchField.isFocused()) {
            String previous = searchField.getText();
            if (searchField.textboxKeyTyped(typedChar, keyCode)
                    && !previous.equals(searchField.getText())) {
                buySearchQuery = searchField.getText();
                onSearchChanged();
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (mouseX >= layout.cropListX && mouseX < layout.cropListX + layout.cropAreaWidth
                && mouseY >= layout.cropListY && mouseY < layout.scrollButtonY) {
            scrollSelectors(wheel > 0 ? -1 : 1);
        }
    }

    private void onSearchChanged() {
        selectorScrollRow = 0;
        List<MarketCommodity> goods = visibleGoods();
        if (!goods.isEmpty() && !containsCommodity(goods, selectedCommodity)) {
            selectCommodity(goods.get(0));
        } else {
            rebuildSelectorButtons();
        }
    }

    @Override
    public void onGuiClosed() {
        // A book session owns its display cache. Closing it releases all page
        // snapshots so the next open is a fresh, bounded server request.
        ClientMarketState.endBookSession();
        graphSnapshot = null;
        graphNodes = Collections.emptyList();
        super.onGuiClosed();
    }

    /** Market requests must continue to reach the integrated server while open. */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // This slotless container keeps the existing responsive vanilla-texture layout in drawScreen.
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawNativePanel(layout.panelX, layout.panelY, layout.panelRight, layout.panelBottom);
        drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.market_book.title"),
                width / 2, layout.panelY + 8);

        MarketHistorySnapshot snapshot = getActiveSnapshot();
        drawMarketHeader(snapshot);
        List<GraphNode> graphNodes = drawHistoryGraph(snapshot);
        drawPageRange(snapshot);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (searchField != null) {
            searchField.drawTextBox();
        }
        drawSelectors();
        drawGraphTooltip(graphNodes, mouseX, mouseY);
        drawItemTooltip(mouseX, mouseY);
    }

    private void drawMarketHeader(MarketHistorySnapshot snapshot) {
        if (snapshot == null) {
            drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.loading"),
                    layout.detailsX, layout.detailsY + 22, 0xFFE8D6A4);
            return;
        }

        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.world_day",
                Long.toString(snapshot.getCurrentMarketDay())), layout.detailsX, layout.detailsY, 0xFFF2E4B7);
        ItemStack icon = createCommodityDisplayStack(selectedCommodity);
        mc.getRenderItem().renderItemAndEffectIntoGUI(icon, layout.detailsX, layout.detailsY + 14);
        mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer, icon, layout.detailsX, layout.detailsY + 14, null);
        int textX = layout.detailsX + 20;
        drawString(fontRenderer, icon.getDisplayName(), textX, layout.detailsY + 15, 0xFFFFFFFF);
        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.current_price",
                formatCoins(snapshot.getCurrentPrice())), textX, layout.detailsY + 27, 0xFFBFE6A8);
        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.base_price",
                formatCoins(selectedCommodity.getBasePrice())), textX, layout.detailsY + 39, 0xFFF2E4B7);
        Long previous = snapshot.getPreviousPrice();
        String previousText = previous == null ? "—" : formatCoins(previous.longValue());
        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.previous_price", previousText),
                textX, layout.detailsY + 51, 0xFFD9D9D9);
        MarketTrend trend = previous == null ? MarketTrend.FLAT
                : MarketTrend.compare(snapshot.getCurrentPrice(), previous.longValue());
        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.trend",
                trend.getSymbol(), getTrendName(trend)), textX, layout.detailsY + 63, getTrendColor(trend));
    }

    private List<GraphNode> drawHistoryGraph(MarketHistorySnapshot snapshot) {
        int graphX = layout.graphX;
        int graphY = layout.graphY;
        int graphRight = layout.graphRight;
        int graphBottom = layout.graphBottom;
        drawRect(graphX - 1, graphY - 1, graphRight + 1, graphBottom + 1, 0xFF8C7C58);
        drawRect(graphX, graphY, graphRight, graphBottom, 0xB0101610);
        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.price_axis"),
                graphX, graphY - 10, 0xFFBDB49D);
        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.day_axis"),
                graphRight - fontRenderer.getStringWidth(I18n.format("gui.lisbam_pastoral_economy.market_book.day_axis")),
                graphBottom + 3, 0xFFBDB49D);

        if (snapshot == null || waitingForSnapshot) {
            drawCenteredString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.loading"),
                    (graphX + graphRight) / 2, (graphY + graphBottom) / 2 - 4, 0xFFE8D6A4);
            return Collections.emptyList();
        }
        List<MarketHistoryPoint> points = snapshot.getPoints();
        if (points.isEmpty()) {
            drawCenteredString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.no_history"),
                    (graphX + graphRight) / 2, (graphY + graphBottom) / 2 - 4, 0xFFE8D6A4);
            return Collections.emptyList();
        }

        if (snapshot != graphSnapshot || layout != graphLayout) {
            rebuildGraphCache(snapshot);
        }

        drawString(fontRenderer, formatCoins((long) graphDisplayMaximum), graphX + 2, graphY + 2, 0xFFBDB49D);
        drawString(fontRenderer, formatCoins((long) graphDisplayMinimum), graphX + 2, graphBottom - 10, 0xFFBDB49D);

        GlStateManager.disableTexture2D();
        GlStateManager.color(0.58F, 0.86F, 0.42F, 1.0F);
        GL11.glLineWidth(2.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (GraphNode node : graphNodes) {
            GL11.glVertex2f(node.x, node.y);
        }
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        for (GraphNode node : graphNodes) {
            drawRect(node.x - 2, node.y - 2, node.x + 3, node.y + 3, 0xFFEAF6C8);
        }

        drawString(fontRenderer, Long.toString(points.get(0).getWorldDay()), graphX, graphBottom + 12, 0xFFBDB49D);
        String latestDay = Long.toString(points.get(points.size() - 1).getWorldDay());
        drawString(fontRenderer, latestDay, graphRight - fontRenderer.getStringWidth(latestDay), graphBottom + 12, 0xFFBDB49D);
        return graphNodes;
    }

    /** Rebuilds graph geometry only when a new server snapshot or layout arrives. */
    private void rebuildGraphCache(MarketHistorySnapshot snapshot) {
        List<MarketHistoryPoint> points = snapshot.getPoints();
        double minPrice = points.get(0).getPrice();
        double maxPrice = minPrice;
        for (MarketHistoryPoint point : points) {
            minPrice = Math.min(minPrice, point.getPrice());
            maxPrice = Math.max(maxPrice, point.getPrice());
        }
        double range = maxPrice - minPrice;
        double padding = range <= 0.0D ? Math.max(1.0D, maxPrice * 0.10D) : Math.max(1.0D, range * 0.10D);
        graphDisplayMinimum = Math.max(0.0D, minPrice - padding);
        graphDisplayMaximum = maxPrice + padding;
        double displayRange = graphDisplayMaximum - graphDisplayMinimum;
        if (displayRange <= 0.0D) {
            displayRange = 1.0D;
        }

        int graphWidth = layout.graphRight - layout.graphX;
        int graphHeight = layout.graphBottom - layout.graphY;
        List<GraphNode> rebuilt = new ArrayList<GraphNode>(points.size());
        for (int index = 0; index < points.size(); index++) {
            MarketHistoryPoint point = points.get(index);
            int nodeX = points.size() == 1 ? (layout.graphX + layout.graphRight) / 2
                    : layout.graphX + Math.round((float) index * (float) graphWidth / (float) (points.size() - 1));
            int nodeY = layout.graphY + Math.round((float) ((graphDisplayMaximum - point.getPrice()) / displayRange)
                    * (float) graphHeight);
            nodeY = Math.max(layout.graphY, Math.min(layout.graphBottom, nodeY));
            rebuilt.add(new GraphNode(point, nodeX, nodeY));
        }
        graphSnapshot = snapshot;
        graphLayout = layout;
        graphNodes = Collections.unmodifiableList(rebuilt);
    }

    private void drawPageRange(MarketHistorySnapshot snapshot) {
        String pageRange = "—";
        if (snapshot != null && !snapshot.getPoints().isEmpty()) {
            List<MarketHistoryPoint> points = snapshot.getPoints();
            pageRange = points.get(0).getWorldDay() + " - " + points.get(points.size() - 1).getWorldDay();
        }
        drawCenteredString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.range", pageRange),
                (layout.graphX + layout.graphRight) / 2, layout.pageButtonY + 6, 0xFFE8D6A4);
    }

    private void drawSelectors() {
        if (searchField != null) {
            drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.search"),
                    layout.cropListX, layout.searchY - 10, 4210752);
        }
        List<MarketCommodity> goods = visibleGoods();
        int first = selectorScrollRow * SELECTOR_COLUMNS;
        int maximum = Math.min(goods.size(), first + layout.selectorRows * SELECTOR_COLUMNS);
        for (int index = first; index < maximum; index++) {
            MarketCommodity commodity = goods.get(index);
            int visibleIndex = index - first;
            int column = visibleIndex % SELECTOR_COLUMNS;
            int row = visibleIndex / SELECTOR_COLUMNS;
            int x = layout.cropListX + column * (layout.cropButtonWidth + layout.cropColumnGap);
            int y = layout.cropListY + row * (layout.cropButtonHeight + layout.cropRowGap);
            ItemStack icon = createCommodityDisplayStack(commodity);
            mc.getRenderItem().renderItemAndEffectIntoGUI(icon, x + (layout.cropButtonWidth - 16) / 2, y + 2);
        }
    }

    private void drawGraphTooltip(List<GraphNode> nodes, int mouseX, int mouseY) {
        for (GraphNode node : nodes) {
            int deltaX = mouseX - node.x;
            int deltaY = mouseY - node.y;
            if (deltaX * deltaX + deltaY * deltaY <= 25) {
                MarketHistoryPoint point = node.point;
                List<String> tooltip = new ArrayList<String>();
                tooltip.add(I18n.format("gui.lisbam_pastoral_economy.market_book.tooltip_day",
                        Long.toString(point.getWorldDay())));
                tooltip.add(I18n.format("gui.lisbam_pastoral_economy.market_book.tooltip_price",
                        formatCoins(point.getPrice())));
                if (point.hasPreviousPoint()) {
                    long delta = point.getDelta();
                    MarketTrend trend = point.getTrend();
                    tooltip.add(I18n.format("gui.lisbam_pastoral_economy.market_book.tooltip_delta",
                            formatDelta(delta), trend.getSymbol()));
                    tooltip.add(I18n.format("gui.lisbam_pastoral_economy.market_book.tooltip_trend",
                            getTrendName(trend)));
                } else {
                    tooltip.add(I18n.format("gui.lisbam_pastoral_economy.market_book.tooltip_delta_none"));
                    tooltip.add(I18n.format("gui.lisbam_pastoral_economy.market_book.tooltip_trend",
                            getTrendName(MarketTrend.FLAT)));
                }
                drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    /** Gives every rendered merchant-sell icon the same native tooltip as an inventory stack. */
    private void drawItemTooltip(int mouseX, int mouseY) {
        if (mouseX >= layout.detailsX && mouseX < layout.detailsX + 16
                && mouseY >= layout.detailsY + 14 && mouseY < layout.detailsY + 30) {
            renderToolTip(createCommodityDisplayStack(selectedCommodity), mouseX, mouseY);
            return;
        }
        List<MarketCommodity> goods = visibleGoods();
        int first = selectorScrollRow * SELECTOR_COLUMNS;
        int maximum = Math.min(goods.size(), first + layout.selectorRows * SELECTOR_COLUMNS);
        for (int index = first; index < maximum; index++) {
            int visibleIndex = index - first;
            int column = visibleIndex % SELECTOR_COLUMNS;
            int row = visibleIndex / SELECTOR_COLUMNS;
            int x = layout.cropListX + column * (layout.cropButtonWidth + layout.cropColumnGap)
                    + (layout.cropButtonWidth - 16) / 2;
            int y = layout.cropListY + row * (layout.cropButtonHeight + layout.cropRowGap) + 2;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                MarketCommodity commodity = goods.get(index);
                renderToolTip(createCommodityDisplayStack(commodity), mouseX, mouseY);
                return;
            }
        }
    }

    /** Keeps the generic wool listing readable without changing its item/meta market identity. */
    private ItemStack createCommodityDisplayStack(MarketCommodity commodity) {
        ItemStack stack = TradeCatalog.createEnchantedBookStackForMarketKey(commodity.getKey());
        if (stack == null) {
            stack = new ItemStack(commodity.getItem(), 1, commodity.getMetadata());
        }
        if (commodity.isAnyWoolColor()) {
            stack.setStackDisplayName(I18n.format("gui.lisbam_pastoral_economy.commodity.wool"));
        }
        return stack;
    }

    private MarketHistorySnapshot getActiveSnapshot() {
        MarketHistorySnapshot exact = ClientMarketState.getSnapshot(selectedCommodity.getKey(), -1L, activeRequestId);
        return exact == null ? ClientMarketState.getLatestSnapshot(selectedCommodity.getKey()) : exact;
    }

    /** All merchant-sell newest windows arrive with the opening request id. */
    private boolean useCachedNewestWindow() {
        MarketHistorySnapshot snapshot = ClientMarketState.getLatestSnapshot(selectedCommodity.getKey());
        if (snapshot == null) {
            return false;
        }
        waitingForSnapshot = false;
        updateNavigation(snapshot);
        return true;
    }

    private void updateNavigation(MarketHistorySnapshot snapshot) {
        if (sellPageButton != null) sellPageButton.enabled = buyPage;
        if (buyPageButton != null) buyPageButton.enabled = !buyPage;
        int maximumScrollRow = Math.max(0, selectorRowCount(visibleGoods()) - layout.selectorRows);
        if (scrollUpButton != null) scrollUpButton.enabled = selectorScrollRow > 0;
        if (scrollDownButton != null) scrollDownButton.enabled = selectorScrollRow < maximumScrollRow;
        for (GuiButton button : buttonList) {
            if (button.id >= SELECTOR_GOOD_BUTTON_OFFSET) {
                int visibleIndex = button.id - SELECTOR_GOOD_BUTTON_OFFSET;
                int selectedIndex = selectorScrollRow * SELECTOR_COLUMNS + visibleIndex;
                List<MarketCommodity> goods = visibleGoods();
                button.enabled = selectedIndex >= 0 && selectedIndex < goods.size()
                        && !goods.get(selectedIndex).getKey().equals(selectedCommodity.getKey());
            }
        }
    }

    private List<MarketCommodity> pageGoods() {
        return buyPage ? BUY_GOODS : SELL_GOODS;
    }

    private List<MarketCommodity> visibleGoods() {
        if (!buyPage || buySearchQuery.trim().isEmpty()) {
            return pageGoods();
        }
        String query = buySearchQuery.trim().toLowerCase(Locale.ROOT);
        List<MarketCommodity> matched = new ArrayList<MarketCommodity>();
        for (MarketCommodity commodity : BUY_GOODS) {
            ItemStack stack = createCommodityDisplayStack(commodity);
            String displayName = stack.getDisplayName().toLowerCase(Locale.ROOT);
            if (PinyinSearch.matches(displayName, query)
                    || commodity.getKey().toLowerCase(Locale.ROOT).contains(query)) {
                matched.add(commodity);
            }
        }
        return matched;
    }

    private static int selectorRowCount(List<MarketCommodity> goods) {
        return (goods.size() + SELECTOR_COLUMNS - 1) / SELECTOR_COLUMNS;
    }

    private static boolean containsCommodity(List<MarketCommodity> goods, MarketCommodity commodity) {
        if (commodity == null) {
            return false;
        }
        for (MarketCommodity candidate : goods) {
            if (candidate.getKey().equals(commodity.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static List<MarketCommodity> collectBuyGoods() {
        List<MarketCommodity> result = new ArrayList<MarketCommodity>();
        for (MarketCommodity commodity : MarketCatalog.getHistoryTracked()) {
            if (commodity.getKey().contains(":buy/")) {
                result.add(commodity);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private long currentClientTick() {
        return mc.world == null ? 0L : mc.world.getTotalWorldTime();
    }

    /** Uses one complete vanilla panel texture instead of hand-drawing a container background. */
    private void drawNativePanel(int left, int top, int right, int bottom) {
        mc.getTextureManager().bindTexture(VANILLA_PANEL_TEXTURE);
        drawScaledCustomSizeModalRect(left, top, 0.0F, 0.0F, 248, 166,
                right - left, bottom - top, 256.0F, 256.0F);
    }

    private void drawCenteredContainerText(String text, int centerX, int y) {
        fontRenderer.drawString(text, centerX - fontRenderer.getStringWidth(text) / 2, y, 4210752);
    }


    private static String formatCoins(long value) {
        String digits = Long.toString(Math.max(0L, value));
        StringBuilder formatted = new StringBuilder(digits.length() + digits.length() / 3);
        for (int index = 0; index < digits.length(); index++) {
            if (index > 0 && (digits.length() - index) % 3 == 0) {
                formatted.append(',');
            }
            formatted.append(digits.charAt(index));
        }
        return formatted.toString();
    }

    private static String formatDelta(long delta) {
        return delta > 0L ? "+" + formatCoins(delta) : Long.toString(delta);
    }

    private static String getTrendName(MarketTrend trend) {
        if (trend == MarketTrend.UP) {
            return I18n.format("gui.lisbam_pastoral_economy.market_book.trend_up");
        }
        if (trend == MarketTrend.DOWN) {
            return I18n.format("gui.lisbam_pastoral_economy.market_book.trend_down");
        }
        return I18n.format("gui.lisbam_pastoral_economy.market_book.trend_flat");
    }

    private static int getTrendColor(MarketTrend trend) {
        if (trend == MarketTrend.UP) {
            return 0xFF8EEB71;
        }
        if (trend == MarketTrend.DOWN) {
            return 0xFFE87878;
        }
        return 0xFFE8D6A4;
    }

    private static final class GraphNode {
        private final MarketHistoryPoint point;
        private final int x;
        private final int y;

        private GraphNode(MarketHistoryPoint point, int x, int y) {
            this.point = point;
            this.x = x;
            this.y = y;
        }
    }

    /** Layout derives every panel coordinate from the actual scaled GUI size. */
    private static final class Layout {
        private final int panelX;
        private final int panelY;
        private final int panelRight;
        private final int panelBottom;
        private final int cropAreaWidth;
        private final int cropListX;
        private final int cropListY;
        private final int cropButtonWidth;
        private final int cropButtonHeight;
        private final int cropColumnGap;
        private final int cropRowGap;
        private final int selectorRows;
        private final int searchX;
        private final int searchY;
        private final int searchWidth;
        private final int searchHeight;
        private final int scrollButtonY;
        private final int scrollButtonWidth;
        private final int scrollButtonHeight;
        private final int detailsX;
        private final int detailsY;
        private final int graphX;
        private final int graphY;
        private final int graphRight;
        private final int graphBottom;
        private final int pageButtonY;
        private final int pageButtonWidth;
        private final int pageButtonHeight;

        private Layout(int panelX, int panelY, int panelRight, int panelBottom, int cropAreaWidth,
                       int cropListX, int cropListY, int cropButtonWidth, int cropButtonHeight,
                       int cropColumnGap, int cropRowGap, int selectorRows, int searchX, int searchY,
                       int searchWidth, int searchHeight, int scrollButtonY, int scrollButtonWidth,
                       int scrollButtonHeight, int detailsX, int detailsY,
                       int graphX, int graphY, int graphRight, int graphBottom,
                       int pageButtonY,
                       int pageButtonWidth, int pageButtonHeight) {
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelRight = panelRight;
            this.panelBottom = panelBottom;
            this.cropAreaWidth = cropAreaWidth;
            this.cropListX = cropListX;
            this.cropListY = cropListY;
            this.cropButtonWidth = cropButtonWidth;
            this.cropButtonHeight = cropButtonHeight;
            this.cropColumnGap = cropColumnGap;
            this.cropRowGap = cropRowGap;
            this.selectorRows = selectorRows;
            this.searchX = searchX;
            this.searchY = searchY;
            this.searchWidth = searchWidth;
            this.searchHeight = searchHeight;
            this.scrollButtonY = scrollButtonY;
            this.scrollButtonWidth = scrollButtonWidth;
            this.scrollButtonHeight = scrollButtonHeight;
            this.detailsX = detailsX;
            this.detailsY = detailsY;
            this.graphX = graphX;
            this.graphY = graphY;
            this.graphRight = graphRight;
            this.graphBottom = graphBottom;
            this.pageButtonY = pageButtonY;
            this.pageButtonWidth = pageButtonWidth;
            this.pageButtonHeight = pageButtonHeight;
        }

        private static Layout create(int screenWidth, int screenHeight) {
            int panelWidth = Math.max(1, Math.min(440, screenWidth - 16));
            int panelHeight = Math.max(1, Math.min(280, screenHeight - 16));
            int panelX = (screenWidth - panelWidth) / 2;
            int panelY = (screenHeight - panelHeight) / 2;
            int panelRight = panelX + panelWidth;
            int panelBottom = panelY + panelHeight;
            int cropAreaWidth = Math.min(144, Math.max(72, panelWidth / 3));
            int cropButtonWidth = Math.max(20, (cropAreaWidth - 2 * 5) / SELECTOR_COLUMNS);
            int cropButtonHeight = 20;
            int cropListX = panelX + 7;
            int searchX = cropListX;
            int searchY = panelY + 46;
            int searchWidth = cropAreaWidth;
            int searchHeight = 12;
            int cropListY = panelY + 63;
            int scrollButtonHeight = 12;
            int scrollButtonY = panelBottom - scrollButtonHeight - 4;
            int selectorRows = Math.max(1, (scrollButtonY - cropListY - 2) / (cropButtonHeight + 2));
            int scrollButtonWidth = Math.max(12, (cropAreaWidth - 3) / 2);
            int detailsX = Math.min(cropListX + cropAreaWidth + 8, panelRight - 10);
            int detailsY = panelY + 27;
            int pageButtonHeight = Math.max(12, Math.min(20, panelHeight / 12));
            int pageButtonY = panelBottom - pageButtonHeight - 6;
            int graphX = detailsX;
            // Leave a readable gap between the vertical axis label (coins/unit)
            // and the header's today's-trend line.
            int graphY = detailsY + 90;
            int graphRight = Math.max(graphX + 1, panelRight - 8);
            int graphBottom = Math.max(graphY + 1, pageButtonY - 18);
            int pageButtonWidth = Math.max(12, Math.min(58, Math.max(12, (graphRight - graphX) / 4)));
            return new Layout(panelX, panelY, panelRight, panelBottom, cropAreaWidth,
                    cropListX, cropListY, cropButtonWidth, cropButtonHeight, 5, 2, selectorRows,
                    searchX, searchY, searchWidth, searchHeight, scrollButtonY, scrollButtonWidth,
                    scrollButtonHeight, detailsX, detailsY, graphX, graphY, graphRight, graphBottom,
                    pageButtonY, pageButtonWidth, pageButtonHeight);
        }
    }
}
