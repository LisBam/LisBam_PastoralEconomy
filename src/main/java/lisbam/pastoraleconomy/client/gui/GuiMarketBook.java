package lisbam.pastoraleconomy.client.gui;

import lisbam.pastoraleconomy.client.ClientMarketState;
import lisbam.pastoraleconomy.market.MarketCatalog;
import lisbam.pastoraleconomy.market.MarketCommodity;
import lisbam.pastoraleconomy.market.MarketHistoryPoint;
import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import lisbam.pastoraleconomy.market.MarketTrend;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.RequestMarketHistoryMessage;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client-only, display-only market book with a bounded historical line chart. */
public final class GuiMarketBook extends GuiScreen {
    private static final int BUTTON_SELL_PAGE = 1;
    private static final int BUTTON_BUY_PAGE = 2;
    private static final int SELL_GOOD_BUTTON_OFFSET = 100;
    private static final int SELL_GOOD_COLUMNS = 3;
    private static final List<MarketCommodity> SELL_GOODS = MarketCatalog.getSellHistoryTracked();
    private static final List<MarketCommodity> BUY_GOODS = collectBuyGoods();

    private MarketCommodity selectedCommodity = SELL_GOODS.get(0);
    private int activeRequestId;
    private boolean initialRequestSent;
    private boolean waitingForSnapshot;
    private boolean buyPage;
    private int prefetchIndex;
    private long nextPrefetchTick;
    private GuiButton sellPageButton;
    private GuiButton buyPageButton;
    private Layout layout;
    private MarketHistorySnapshot graphSnapshot;
    private Layout graphLayout;
    private List<GraphNode> graphNodes = Collections.emptyList();
    private double graphDisplayMinimum;
    private double graphDisplayMaximum;

    @Override
    public void initGui() {
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
        addSellGoodButtons();
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

    private void addSellGoodButtons() {
        List<MarketCommodity> goods = currentGoods();
        for (int index = 0; index < goods.size(); index++) {
            int column = index % SELL_GOOD_COLUMNS;
            int row = index / SELL_GOOD_COLUMNS;
            int buttonX = layout.cropListX + column * (layout.cropButtonWidth + layout.cropColumnGap);
            int buttonY = layout.cropListY + row * (layout.cropButtonHeight + layout.cropRowGap);
            buttonList.add(new GuiButton(SELL_GOOD_BUTTON_OFFSET + index, buttonX, buttonY,
                    layout.cropButtonWidth, layout.cropButtonHeight, ""));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        if (button.id == BUTTON_SELL_PAGE || button.id == BUTTON_BUY_PAGE) {
            buyPage = button.id == BUTTON_BUY_PAGE;
            prefetchIndex = 0;
            List<MarketCommodity> goods = currentGoods();
            selectedCommodity = goods.isEmpty() ? SELL_GOODS.get(0) : goods.get(0);
            initGui();
            return;
        }
        if (button.id >= SELL_GOOD_BUTTON_OFFSET && button.id < SELL_GOOD_BUTTON_OFFSET + currentGoods().size()) {
            selectedCommodity = currentGoods().get(button.id - SELL_GOOD_BUTTON_OFFSET);
            if (useCachedNewestWindow()) {
                return;
            }
            requestWindow(true);
            return;
        }
    }

    private void requestWindow(boolean forceRequest) {
        activeRequestId = nextRequestId();
        waitingForSnapshot = true;
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
        MarketHistorySnapshot snapshot = getActiveSnapshot();
        if (snapshot != null) {
            waitingForSnapshot = false;
        }
        updateNavigation(snapshot);
        if (!waitingForSnapshot && mc.world != null && mc.world.getTotalWorldTime() >= nextPrefetchTick) {
            List<MarketCommodity> goods = currentGoods();
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
        if (ModGuiInput.closeWithInventoryKey(mc, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
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

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(layout.panelX, layout.panelY, layout.panelRight, layout.panelBottom, 0xE01D251E);
        drawRect(layout.panelX + 1, layout.panelY + 1, layout.panelRight - 1, layout.panelBottom - 1, 0xE8303B31);
        drawCenteredString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.title"),
                width / 2, layout.panelY + 8, 0xFFF2E4B7);

        MarketHistorySnapshot snapshot = getActiveSnapshot();
        drawMarketHeader(snapshot);
        List<GraphNode> graphNodes = drawHistoryGraph(snapshot);
        drawPageRange(snapshot);
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawCropSelectors();
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
        Long previous = snapshot.getPreviousPrice();
        String previousText = previous == null ? "—" : formatCoins(previous.longValue());
        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.previous_price", previousText),
                textX, layout.detailsY + 39, 0xFFD9D9D9);
        MarketTrend trend = previous == null ? MarketTrend.FLAT
                : MarketTrend.compare(snapshot.getCurrentPrice(), previous.longValue());
        drawString(fontRenderer, I18n.format("gui.lisbam_pastoral_economy.market_book.trend",
                trend.getSymbol(), getTrendName(trend)), textX, layout.detailsY + 51, getTrendColor(trend));
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

    private void drawCropSelectors() {
        List<MarketCommodity> goods = currentGoods();
        for (int index = 0; index < goods.size(); index++) {
            MarketCommodity commodity = goods.get(index);
            int column = index % SELL_GOOD_COLUMNS;
            int row = index / SELL_GOOD_COLUMNS;
            int x = layout.cropListX + column * (layout.cropButtonWidth + layout.cropColumnGap);
            int y = layout.cropListY + row * (layout.cropButtonHeight + layout.cropRowGap);
            if (commodity.getKey().equals(selectedCommodity.getKey())) {
                drawRect(x, y, x + layout.cropButtonWidth, y + layout.cropButtonHeight, 0x556DA24A);
            }
            ItemStack icon = createCommodityDisplayStack(commodity);
            mc.getRenderItem().renderItemAndEffectIntoGUI(icon, x + 2, y + 1);
            String name = icon.getDisplayName();
            int maximumTextWidth = Math.max(0, layout.cropButtonWidth - 20);
            if (fontRenderer.getStringWidth(name) > maximumTextWidth) {
                name = fontRenderer.trimStringToWidth(name, maximumTextWidth);
            }
            drawString(fontRenderer, name, x + 19, y + (layout.cropButtonHeight - 8) / 2, 0xFFFFFFFF);
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
        List<MarketCommodity> goods = currentGoods();
        for (int index = 0; index < goods.size(); index++) {
            int column = index % SELL_GOOD_COLUMNS;
            int row = index / SELL_GOOD_COLUMNS;
            int x = layout.cropListX + column * (layout.cropButtonWidth + layout.cropColumnGap) + 2;
            int y = layout.cropListY + row * (layout.cropButtonHeight + layout.cropRowGap) + 1;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                MarketCommodity commodity = goods.get(index);
                renderToolTip(createCommodityDisplayStack(commodity), mouseX, mouseY);
                return;
            }
        }
    }

    /** Keeps the generic wool listing readable without changing its item/meta market identity. */
    private ItemStack createCommodityDisplayStack(MarketCommodity commodity) {
        ItemStack stack = new ItemStack(commodity.getItem(), 1, commodity.getMetadata());
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
        for (GuiButton button : buttonList) {
            if (button.id >= SELL_GOOD_BUTTON_OFFSET && button.id < SELL_GOOD_BUTTON_OFFSET + currentGoods().size()) {
                button.enabled = !waitingForSnapshot;
            }
        }
    }

    private List<MarketCommodity> currentGoods() {
        return buyPage ? BUY_GOODS : SELL_GOODS;
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
        private final int cropListX;
        private final int cropListY;
        private final int cropButtonWidth;
        private final int cropButtonHeight;
        private final int cropColumnGap;
        private final int cropRowGap;
        private final int detailsX;
        private final int detailsY;
        private final int graphX;
        private final int graphY;
        private final int graphRight;
        private final int graphBottom;
        private final int pageButtonY;
        private final int pageButtonWidth;
        private final int pageButtonHeight;

        private Layout(int panelX, int panelY, int panelRight, int panelBottom,
                       int cropListX, int cropListY, int cropButtonWidth, int cropButtonHeight,
                       int cropColumnGap, int cropRowGap, int detailsX, int detailsY,
                       int graphX, int graphY, int graphRight, int graphBottom,
                       int pageButtonY,
                       int pageButtonWidth, int pageButtonHeight) {
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelRight = panelRight;
            this.panelBottom = panelBottom;
            this.cropListX = cropListX;
            this.cropListY = cropListY;
            this.cropButtonWidth = cropButtonWidth;
            this.cropButtonHeight = cropButtonHeight;
            this.cropColumnGap = cropColumnGap;
            this.cropRowGap = cropRowGap;
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
            int cropAreaWidth = Math.min(144, Math.max(40, panelWidth / 3));
            int cropButtonWidth = Math.max(12, (cropAreaWidth - 2 * 5) / SELL_GOOD_COLUMNS);
            int cropButtonHeight = Math.max(12, Math.min(16, Math.max(12, (panelHeight - 64) / 8)));
            int cropListX = panelX + 7;
            int cropListY = panelY + 28;
            int detailsX = Math.min(cropListX + cropAreaWidth + 8, panelRight - 10);
            int detailsY = panelY + 27;
            int pageButtonHeight = Math.max(12, Math.min(20, panelHeight / 12));
            int pageButtonY = panelBottom - pageButtonHeight - 6;
            int graphX = detailsX;
            // Leave a readable gap between the vertical axis label (coins/unit)
            // and the header's today's-trend line.
            int graphY = detailsY + 78;
            int graphRight = Math.max(graphX + 1, panelRight - 8);
            int graphBottom = Math.max(graphY + 1, pageButtonY - 18);
            int pageButtonWidth = Math.max(12, Math.min(58, Math.max(12, (graphRight - graphX) / 4)));
            return new Layout(panelX, panelY, panelRight, panelBottom,
                    cropListX, cropListY, cropButtonWidth, cropButtonHeight, 5, 2,
                    detailsX, detailsY, graphX, graphY, graphRight, graphBottom,
                    pageButtonY, pageButtonWidth, pageButtonHeight);
        }
    }
}
