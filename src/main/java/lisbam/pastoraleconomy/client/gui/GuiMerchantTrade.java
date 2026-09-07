package lisbam.pastoraleconomy.client.gui;

import lisbam.pastoraleconomy.client.ClientMerchantTradeState;
import lisbam.pastoraleconomy.client.ClientMerchantTradeViewState;
import lisbam.pastoraleconomy.client.ClientPlayerState;
import lisbam.pastoraleconomy.entity.EntityMerchant;
import lisbam.pastoraleconomy.gui.ContainerMerchantTrade;
import lisbam.pastoraleconomy.market.MarketTrend;
import lisbam.pastoraleconomy.merchant.MerchantTradeOfferView;
import lisbam.pastoraleconomy.merchant.MerchantTradeSnapshot;
import lisbam.pastoraleconomy.merchant.EmeraldTradeAction;
import lisbam.pastoraleconomy.merchant.EmeraldTradeRules;
import lisbam.pastoraleconomy.merchant.TradeCatalog;
import lisbam.pastoraleconomy.merchant.TradeCatalogEntry;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.EmeraldTradeRequestMessage;
import lisbam.pastoraleconomy.network.message.MerchantTradeRequestMessage;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;

/**
 * Compact, responsive 1.12.2 merchant screen. Quantities are display/input
 * state only; MerchantTradeService remains the sole transaction authority.
 */
public final class GuiMerchantTrade extends GuiContainer implements GuiSlider.ISlider {
    private static final int BUTTON_SELL_PAGE = 1;
    private static final int BUTTON_BUY_PAGE = 2;
    private static final int BUTTON_EMERALD_PAGE = 3;
    private static final int BUTTON_CONFIRM_OFFSET = 100;
    private static final int BUTTON_SLIDER_OFFSET = 200;
    private static final int BUTTON_EMERALD_BUY = 300;
    private static final int BUTTON_EMERALD_SELL = 301;
    private static final int MAX_QUANTITY = 4096;
    private static final ResourceLocation VANILLA_PANEL_TEXTURE = new ResourceLocation(
            "minecraft", "textures/gui/demo_background.png");
    private static final ResourceLocation VANILLA_SLOT_TEXTURE = new ResourceLocation(
            "minecraft", "textures/gui/container/generic_54.png");
    private static final int VANILLA_SLOT_U = 7;
    private static final int VANILLA_SLOT_V = 17;
    /** Vanilla workbench/furnace inventory-title colour for static window text. */
    private static final int VANILLA_CONTAINER_TEXT_COLOR = 4210752;

    private final int merchantEntityId;
    private final int[] sellQuantities = new int[MerchantTradeSnapshot.SELL_COUNT];
    private final int[] buyQuantities = new int[MerchantTradeSnapshot.BUY_COUNT];
    private final int[] sellHeldCounts = new int[MerchantTradeSnapshot.SELL_COUNT];
    private int page = ClientMerchantTradeViewState.getPage();
    /** Spot-market buy and sell use distinct controls so their limits never obscure each other. */
    private int emeraldBuyQuantity = 1;
    private int emeraldSellQuantity = 1;
    private int nextRequestId;
    private GuiTextField[] quantityFields = new GuiTextField[0];
    private GuiSlider[] quantitySliders = new GuiSlider[0];
    private Layout layout;

    public GuiMerchantTrade(int merchantEntityId) {
        super(new ContainerMerchantTrade(merchantEntityId));
        this.merchantEntityId = merchantEntityId;
    }

    @Override
    public void initGui() {
        // Prevent Forge's OpenGui packet from assigning this window id to the
        // player's permanent inventoryContainer.
        super.initGui();
        buttonList.clear();
        layout = Layout.create(width, height);
        int tabWidth = (layout.panelWidth - 22) / 3;
        buttonList.add(new GuiButton(BUTTON_SELL_PAGE, layout.panelX + 5, layout.tabY, tabWidth, 14,
                I18n.format("gui.lisbam_pastoral_economy.merchant.sell")));
        buttonList.add(new GuiButton(BUTTON_BUY_PAGE, layout.panelX + 11 + tabWidth, layout.tabY, tabWidth, 14,
                I18n.format("gui.lisbam_pastoral_economy.merchant.buy")));
        buttonList.add(new GuiButton(BUTTON_EMERALD_PAGE, layout.panelX + 17 + tabWidth * 2, layout.tabY, tabWidth, 14,
                I18n.format("gui.lisbam_pastoral_economy.merchant.emerald")));

        int count = pageOfferCount();
        quantityFields = new GuiTextField[count];
        quantitySliders = new GuiSlider[count];
        MerchantTradeSnapshot snapshot = ClientMerchantTradeState.get();
        if (isSellPage()) {
            refreshSellHeldCounts(snapshot);
        }
        for (int index = 0; index < count; index++) {
            int cardX = isEmeraldPage() ? layout.emeraldControlX(index) : layout.cardX(index, isBuyPage());
            int controlsY = isEmeraldPage() ? layout.emeraldControlsY() : layout.controlsY(index, isBuyPage());
            MerchantTradeOfferView view = getView(snapshot, index);
            int quantity = clampQuantity(index, getQuantity(index), view);
            setQuantity(index, quantity);

            int sliderWidth = isEmeraldPage() ? layout.emeraldSliderWidth() : layout.sliderWidth;
            int fieldWidth = isEmeraldPage() ? layout.emeraldFieldWidth() : layout.fieldWidth;
            int sliderX = isEmeraldPage() ? layout.emeraldSliderX(index) : layout.sliderX(cardX);
            int fieldX = isEmeraldPage() ? layout.emeraldFieldX(index) : layout.fieldX(cardX, sliderWidth);
            GuiSlider slider = new GuiSlider(BUTTON_SLIDER_OFFSET + index, sliderX, controlsY,
                    sliderWidth, 12, "", "", 0.0D, 1.0D, 1.0D, false, false, this);
            quantitySliders[index] = slider;
            buttonList.add(slider);

            GuiTextField field = new GuiTextField(index, fontRenderer, fieldX, controlsY, fieldWidth, 12);
            field.setMaxStringLength(10);
            field.setTextColor(0xFFFFFF);
            field.setDisabledTextColour(0xFFFFFF);
            field.setText(Integer.toString(quantity));
            quantityFields[index] = field;

            if (!isEmeraldPage()) {
                buttonList.add(new GuiButton(BUTTON_CONFIRM_OFFSET + index, layout.confirmX(cardX), controlsY,
                        layout.confirmWidth, 12, I18n.format("gui.lisbam_pastoral_economy.merchant.confirm")));
            }
        }
        if (isEmeraldPage()) {
            buttonList.add(new GuiButton(BUTTON_EMERALD_BUY, layout.emeraldControlX(0), layout.emeraldButtonsY(),
                    layout.emeraldColumnWidth(), 14,
                    I18n.format("gui.lisbam_pastoral_economy.merchant.emerald_buy")));
            buttonList.add(new GuiButton(BUTTON_EMERALD_SELL, layout.emeraldControlX(1), layout.emeraldButtonsY(),
                    layout.emeraldColumnWidth(), 14,
                    I18n.format("gui.lisbam_pastoral_economy.merchant.emerald_sell")));
        }
        updateControls(snapshot);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_SELL_PAGE || button.id == BUTTON_BUY_PAGE || button.id == BUTTON_EMERALD_PAGE) {
            page = button.id == BUTTON_SELL_PAGE ? ClientMerchantTradeViewState.PAGE_SELL
                    : button.id == BUTTON_BUY_PAGE ? ClientMerchantTradeViewState.PAGE_BUY
                    : ClientMerchantTradeViewState.PAGE_EMERALD;
            ClientMerchantTradeViewState.setPage(page);
            initGui();
            return;
        }
        if (button.id >= BUTTON_SLIDER_OFFSET && button.id < BUTTON_SLIDER_OFFSET + pageOfferCount()) {
            return;
        }
        if ((button.id == BUTTON_EMERALD_BUY || button.id == BUTTON_EMERALD_SELL) && button.enabled) {
            sendEmeraldTrade(button.id == BUTTON_EMERALD_BUY ? EmeraldTradeAction.BUY : EmeraldTradeAction.SELL);
            return;
        }
        if (button.id < BUTTON_CONFIRM_OFFSET || button.id >= BUTTON_CONFIRM_OFFSET + pageOfferCount()
                || !button.enabled) {
            return;
        }
        MerchantTradeSnapshot snapshot = ClientMerchantTradeState.get();
        if (snapshot == null) {
            return;
        }
        int slot = button.id - BUTTON_CONFIRM_OFFSET;
        MerchantTradeOfferView view = getView(snapshot, slot);
        int quantity = readQuantityField(slot, view);
        if (!isTradable(view, slot)) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new MerchantTradeRequestMessage(snapshot.getMerchantId(), snapshot.getWindowId(),
                snapshot.getWorldDay(), isBuyPage(), slot, quantity, nextRequestId()));
    }

    @Override
    public void updateScreen() {
        for (GuiTextField field : quantityFields) {
            field.updateCursorCounter();
        }
        MerchantTradeSnapshot snapshot = ClientMerchantTradeState.get();
        if (isSellPage()) {
            refreshSellHeldCounts(snapshot);
        }
        updateControls(snapshot);
    }

    /** Trading must continue to tick in a single-player integrated server. */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // This slotless container keeps the existing responsive vanilla-texture layout in drawScreen.
    }

    @Override
    public void onGuiClosed() {
        ClientMerchantTradeState.clear();
        super.onGuiClosed();
    }

    @Override
    public void onChangeSliderValue(GuiSlider slider) {
        int slot = slider.id - BUTTON_SLIDER_OFFSET;
        if (slot < 0 || slot >= quantityFields.length) {
            return;
        }
        MerchantTradeOfferView view = getView(ClientMerchantTradeState.get(), slot);
        setQuantity(slot, clampQuantity(slot, slider.getValueInt(), view));
        if (!quantityFields[slot].isFocused()) {
            quantityFields[slot].setText(Integer.toString(getQuantity(slot)));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (int index = 0; index < quantityFields.length; index++) {
            GuiTextField field = quantityFields[index];
            boolean wasFocused = field.isFocused();
            field.mouseClicked(mouseX, mouseY, mouseButton);
            if (wasFocused && !field.isFocused()) {
                readQuantityField(index, getView(ClientMerchantTradeState.get(), index));
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (ModGuiInput.closeWithExitKey(mc, keyCode, hasFocusedQuantityField())) {
            return;
        }
        for (int index = 0; index < quantityFields.length; index++) {
            GuiTextField field = quantityFields[index];
            if (!field.isFocused()) {
                continue;
            }
            if (Character.isDigit(typedChar) || isQuantityEditingKey(keyCode)) {
                if (field.textboxKeyTyped(typedChar, keyCode)) {
                    readQuantityField(index, getView(ClientMerchantTradeState.get(), index));
                }
                return;
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean hasFocusedQuantityField() {
        for (GuiTextField field : quantityFields) {
            if (field.isFocused()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawNativePanel(layout.panelX, layout.panelY, layout.panelRight, layout.panelBottom);

        MerchantTradeSnapshot snapshot = ClientMerchantTradeState.get();
        String title = getMerchantDisplayName() + " - "
                + I18n.format("gui.lisbam_pastoral_economy.merchant.title");
        drawCenteredContainerText(title, width / 2, layout.panelY + 7);
        long balance = snapshot == null ? ClientPlayerState.getCoins() : snapshot.getBalance();
        long day = snapshot == null ? -1L : snapshot.getWorldDay();
        drawContainerText(I18n.format("gui.lisbam_pastoral_economy.merchant.day", Long.toString(day)),
                layout.panelX + 6, layout.panelY + 20);
        String coins = I18n.format("gui.lisbam_pastoral_economy.merchant.coins", format(balance));
        drawContainerText(coins, layout.panelRight - 6 - fontRenderer.getStringWidth(coins), layout.panelY + 20);

        if (isEmeraldPage()) {
            drawEmeraldMarket(snapshot);
        } else {
            for (int index = 0; index < pageOfferCount(); index++) {
                drawOffer(getView(snapshot, index), index);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (GuiTextField field : quantityFields) {
            field.drawTextBox();
        }
        drawItemTooltip(mouseX, mouseY);
    }

    private String getMerchantDisplayName() {
        if (mc.world != null) {
            Entity entity = mc.world.getEntityByID(merchantEntityId);
            if (entity instanceof EntityMerchant) {
                String name = ((EntityMerchant) entity).getCustomNameTag();
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
            }
        }
        return I18n.format("entity.lisbam_pastoral_economy.merchant.name");
    }

    private void drawOffer(MerchantTradeOfferView view, int index) {
        int x = layout.cardX(index, isBuyPage());
        int y = layout.cardY(index, isBuyPage());
        int cardHeight = layout.cardHeight(isBuyPage());
        if (view == null || !view.isEnabled()) {
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.merchant.future"),
                    x + layout.cardWidth / 2, y + (cardHeight - 8) / 2);
            return;
        }

        TradeCatalogEntry entry = TradeCatalog.get(view.getCatalogKey());
        if (entry == null) {
            return;
        }
        ItemStack stack = createDisplayStack(entry, view.getEnchantmentLevel());
        drawNativeSlot(x + 3, y + 3);
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x + 4, y + 4);
        mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer, stack, x + 4, y + 4, null);

        String name = stack.getDisplayName();
        if (entry.isEnchantment() && view.getEnchantmentLevel() > 0) {
            name = entry.getEnchantmentDefinition().getEnchantment().getTranslatedName(view.getEnchantmentLevel());
        }
        int textX = x + 24;
        drawContainerText(fontRenderer.trimStringToWidth(name, layout.cardWidth - 28), textX, y + 3);
        String trend = view.hasPreviousPrice()
                ? MarketTrend.compare(view.getCurrentPrice(), view.getPreviousPrice()).getSymbol() : "-";
        long total = multiplyForDisplay(view.getCurrentPrice(), getQuantity(index));
        String totalText = I18n.format("gui.lisbam_pastoral_economy.merchant.total", format(total));
        String priceText = format(view.getCurrentPrice()) + " " + trend + "  " + totalText;
        drawContainerText(fontRenderer.trimStringToWidth(priceText, layout.cardWidth - 28), textX, y + 13);
        long basePrice = entry.getBasePriceForLevel(view.getEnchantmentLevel());
        String baseText = I18n.format("gui.lisbam_pastoral_economy.merchant.base_price", format(basePrice));
        drawContainerText(fontRenderer.trimStringToWidth(baseText, layout.cardWidth - 28), textX, y + 23);
        if (cardHeight >= 42) {
            String amount = isBuyPage()
                    ? I18n.format("gui.lisbam_pastoral_economy.merchant.remaining",
                    view.getRemainingItems() < 0
                            ? I18n.format("gui.lisbam_pastoral_economy.merchant.unlimited")
                            : Long.toString(view.getRemainingItems()))
                    : I18n.format("gui.lisbam_pastoral_economy.merchant.available",
                    Integer.toString(getSellHeldCount(index, entry)));
            drawContainerText(fontRenderer.trimStringToWidth(amount, layout.cardWidth - 28), textX, y + 33);
        }
    }

    /** Draws the third tab in the existing merchant panel with distinct buy/sell columns. */
    private void drawEmeraldMarket(MerchantTradeSnapshot snapshot) {
        int y = layout.cardTop;
        if (snapshot == null) {
            drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.merchant.emerald_loading"),
                    width / 2, y + 30);
            return;
        }
        long current = snapshot.getEmeraldCurrentPrice();
        long previous = snapshot.getEmeraldPreviousPrice();
        long delta = current - previous;
        drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.merchant.emerald_pair"), width / 2, y);
        drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.merchant.emerald_current", format(current)),
                width / 2, y + 14);
        drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.merchant.emerald_previous", format(previous)),
                width / 2, y + 26);
        drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.merchant.emerald_change",
                formatSigned(delta), formatPercent(delta, previous)), width / 2, y + 38);
        drawCenteredContainerText(I18n.format("gui.lisbam_pastoral_economy.merchant.emerald_fee",
                Integer.toString(EmeraldTradeRules.FEE_PERCENT)), width / 2, y + 50);
        drawEmeraldTradeSide(snapshot, 0, true, y + 68);
        drawEmeraldTradeSide(snapshot, 1, false, y + 68);
    }

    /** Buy and sell each retain their own quantity, limit, preview, and button column. */
    private void drawEmeraldTradeSide(MerchantTradeSnapshot snapshot, int index, boolean buying, int y) {
        int centerX = layout.emeraldColumnCenter(index);
        int quantity = getQuantity(index);
        long current = snapshot.getEmeraldCurrentPrice();
        long base = EmeraldTradeRules.calculateBaseAmount(current, quantity);
        long total = buying ? EmeraldTradeRules.calculateBuyCost(current, quantity)
                : EmeraldTradeRules.calculateSellIncome(current, quantity);
        long fee = total < 0L || base < 0L ? 0L : buying ? total - base : base - total;
        String sideKey = buying ? "gui.lisbam_pastoral_economy.merchant.emerald_buy"
                : "gui.lisbam_pastoral_economy.merchant.emerald_sell";
        String limitKey = buying ? "gui.lisbam_pastoral_economy.merchant.emerald_buy_limit"
                : "gui.lisbam_pastoral_economy.merchant.emerald_sell_limit";
        String totalKey = buying ? "gui.lisbam_pastoral_economy.merchant.emerald_buy_total"
                : "gui.lisbam_pastoral_economy.merchant.emerald_sell_total";
        drawCenteredContainerText(I18n.format(sideKey), centerX, y);
        drawCenteredContainerText(I18n.format(limitKey, Integer.toString(emeraldMaximum(snapshot, index))), centerX, y + 13);
        drawCenteredContainerText(I18n.format(totalKey, format(total), format(fee)), centerX, y + 26);
    }

    private void sendEmeraldTrade(EmeraldTradeAction action) {
        MerchantTradeSnapshot snapshot = ClientMerchantTradeState.get();
        if (snapshot == null) {
            return;
        }
        int index = action == EmeraldTradeAction.BUY ? 0 : 1;
        int maximum = emeraldMaximum(snapshot, index);
        if (maximum <= 0) {
            updateControls(snapshot);
            return;
        }
        int quantity = Math.max(1, Math.min(maximum, getQuantity(index)));
        setQuantity(index, quantity);
        if (index < quantityFields.length) {
            quantityFields[index].setText(Integer.toString(quantity));
        }
        ModNetwork.CHANNEL.sendToServer(new EmeraldTradeRequestMessage(snapshot.getMerchantId(), snapshot.getWindowId(),
                snapshot.getWorldDay(), action, quantity, nextRequestId()));
    }

    private int emeraldMaximum(MerchantTradeSnapshot snapshot, int index) {
        if (snapshot == null) {
            return 0;
        }
        return index == 0 ? snapshot.getEmeraldMaxBuy() : snapshot.getEmeraldMaxSell();
    }

    private void drawItemTooltip(int mouseX, int mouseY) {
        if (isEmeraldPage()) {
            return;
        }
        MerchantTradeSnapshot snapshot = ClientMerchantTradeState.get();
        for (int index = 0; index < pageOfferCount(); index++) {
            int x = layout.cardX(index, isBuyPage()) + 4;
            int y = layout.cardY(index, isBuyPage()) + 4;
            if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16) {
                continue;
            }
            MerchantTradeOfferView view = getView(snapshot, index);
            TradeCatalogEntry entry = view == null ? null : TradeCatalog.get(view.getCatalogKey());
            if (entry != null && view.isEnabled()) {
                renderToolTip(createDisplayStack(entry, view.getEnchantmentLevel()), mouseX, mouseY);
            }
            return;
        }
    }

    /** The single wool offer is intentionally colour-agnostic, including its visible name. */
    private ItemStack createDisplayStack(TradeCatalogEntry entry, int enchantmentLevel) {
        ItemStack stack = entry.createStack(1, enchantmentLevel);
        if (entry.isAnyWoolColor()) {
            stack.setStackDisplayName(I18n.format("gui.lisbam_pastoral_economy.commodity.wool"));
        }
        return stack;
    }

    private void updateControls(MerchantTradeSnapshot snapshot) {
        // The current page is not an action, and an unavailable offer must use
        // the standard 1.12.2 disabled GuiButton treatment.
        setButtonEnabled(BUTTON_SELL_PAGE, !isSellPage());
        setButtonEnabled(BUTTON_BUY_PAGE, !isBuyPage());
        setButtonEnabled(BUTTON_EMERALD_PAGE, !isEmeraldPage());
        if (isEmeraldPage()) {
            for (int index = 0; index < quantityFields.length; index++) {
                int maximum = emeraldMaximum(snapshot, index);
                int quantity = clampQuantity(index, getQuantity(index), null);
                setQuantity(index, quantity);
                configureSlider(quantitySliders[index], quantity, maximum, maximum > 0);
                if (!quantityFields[index].isFocused()) {
                    quantityFields[index].setText(Integer.toString(quantity));
                }
                quantityFields[index].setEnabled(maximum > 0);
                if (maximum <= 0) {
                    quantityFields[index].setFocused(false);
                }
            }
            setButtonEnabled(BUTTON_EMERALD_BUY, snapshot != null && snapshot.getEmeraldMaxBuy() > 0);
            setButtonEnabled(BUTTON_EMERALD_SELL, snapshot != null && snapshot.getEmeraldMaxSell() > 0);
            return;
        }
        for (int index = 0; index < quantityFields.length; index++) {
            MerchantTradeOfferView view = getView(snapshot, index);
            int quantity = clampQuantity(index, getQuantity(index), view);
            setQuantity(index, quantity);
            boolean tradable = isTradable(view, index);
            configureSlider(quantitySliders[index], quantity, maxQuantity(view, index), tradable);
            if (!quantityFields[index].isFocused()) {
                quantityFields[index].setText(Integer.toString(quantity));
            }
            if (!tradable) {
                quantityFields[index].setFocused(false);
            }
            quantityFields[index].setEnabled(tradable);
            setButtonEnabled(BUTTON_CONFIRM_OFFSET + index, tradable);
        }
    }

    private void configureSlider(GuiSlider slider, int quantity, int maximum, boolean enabled) {
        if (maximum <= 1) {
            slider.minValue = 0.0D;
            slider.maxValue = 1.0D;
            slider.setValue(1.0D);
            slider.enabled = false;
            return;
        }
        slider.minValue = 1.0D;
        slider.maxValue = maximum;
        slider.setValue(quantity);
        slider.enabled = enabled;
    }

    private int readQuantityField(int index, MerchantTradeOfferView view) {
        if (index < 0 || index >= quantityFields.length) {
            return 1;
        }
        String text = quantityFields[index].getText();
        Integer parsed = parseQuantity(text);
        if (parsed != null) {
            setQuantity(index, clampQuantity(index, parsed.intValue(), view));
        }
        int quantity = clampQuantity(index, getQuantity(index), view);
        setQuantity(index, quantity);
        if (!quantityFields[index].isFocused()) {
            quantityFields[index].setText(Integer.toString(quantity));
        }
        return quantity;
    }

    private int clampQuantity(int index, int quantity, MerchantTradeOfferView view) {
        int maximum = maxQuantity(view, index);
        return maximum <= 0 ? 1 : Math.max(1, Math.min(maximum, quantity));
    }

    private boolean isTradable(MerchantTradeOfferView view, int index) {
        if (isEmeraldPage()) {
            return emeraldMaximum(ClientMerchantTradeState.get(), index) > 0;
        }
        if (view == null || !view.isEnabled()) {
            return false;
        }
        return maxQuantity(view, index) > 0;
    }

    private int maxQuantity(MerchantTradeOfferView view, int index) {
        if (isEmeraldPage()) {
            return emeraldMaximum(ClientMerchantTradeState.get(), index);
        }
        if (view == null || !view.isEnabled()) {
            return 0;
        }
        if (isBuyPage()) {
            int stock = view.getRemainingItems();
            int stockLimit = stock < 0 ? MAX_QUANTITY : Math.max(0, Math.min(MAX_QUANTITY, stock));
            MerchantTradeSnapshot snapshot = ClientMerchantTradeState.get();
            long balance = snapshot == null ? ClientPlayerState.getCoins() : snapshot.getBalance();
            long price = view.getCurrentPrice();
            int affordableLimit = price <= 0L ? 0
                    : (int) Math.min((long) MAX_QUANTITY, Math.max(0L, balance / price));
            return Math.min(stockLimit, affordableLimit);
        }
        TradeCatalogEntry entry = TradeCatalog.get(view.getCatalogKey());
        return entry == null ? 0 : Math.max(0, Math.min(MAX_QUANTITY, getSellHeldCount(index, entry)));
    }

    private MerchantTradeOfferView getView(MerchantTradeSnapshot snapshot, int slot) {
        if (snapshot == null) {
            return null;
        }
        if (isBuyPage()) {
            return slot < snapshot.getBuyOffers().size() ? snapshot.getBuyOffers().get(slot) : null;
        }
        return slot < snapshot.getSellOffers().size() ? snapshot.getSellOffers().get(slot) : null;
    }

    private int pageOfferCount() {
        return isEmeraldPage() ? 2 : isBuyPage() ? MerchantTradeSnapshot.BUY_COUNT : MerchantTradeSnapshot.SELL_COUNT;
    }

    private int getQuantity(int index) {
        if (isEmeraldPage()) {
            int quantity = index == 0 ? emeraldBuyQuantity : emeraldSellQuantity;
            return quantity > 0 ? quantity : 1;
        }
        int[] quantities = isBuyPage() ? buyQuantities : sellQuantities;
        return quantities[index] > 0 ? quantities[index] : 1;
    }

    private void setQuantity(int index, int quantity) {
        if (isEmeraldPage()) {
            if (index == 0) {
                emeraldBuyQuantity = quantity;
            } else {
                emeraldSellQuantity = quantity;
            }
            return;
        }
        int[] quantities = isBuyPage() ? buyQuantities : sellQuantities;
        quantities[index] = quantity;
    }

    private void setButtonEnabled(int id, boolean enabled) {
        for (GuiButton button : buttonList) {
            if (button.id == id) {
                button.enabled = enabled;
                return;
            }
        }
    }

    private static boolean isQuantityEditingKey(int keyCode) {
        return keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_LEFT
                || keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_HOME || keyCode == Keyboard.KEY_END;
    }

    private static Integer parseQuantity(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int result = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (!Character.isDigit(character) || result > (Integer.MAX_VALUE - (character - '0')) / 10) {
                return null;
            }
            result = result * 10 + (character - '0');
        }
        return Integer.valueOf(result);
    }

    private int nextRequestId() {
        if (nextRequestId == Integer.MAX_VALUE) {
            nextRequestId = 1;
        } else {
            nextRequestId++;
        }
        return nextRequestId;
    }

    private void refreshSellHeldCounts(MerchantTradeSnapshot snapshot) {
        for (int index = 0; index < sellHeldCounts.length; index++) {
            MerchantTradeOfferView view = snapshot == null || index >= snapshot.getSellOffers().size()
                    ? null : snapshot.getSellOffers().get(index);
            TradeCatalogEntry entry = view == null || !view.isEnabled() ? null : TradeCatalog.get(view.getCatalogKey());
            sellHeldCounts[index] = entry == null ? 0
                    : addAvailableCounts(countHeld(entry), Math.max(0, view.getRemainingItems()));
        }
    }

    private int getSellHeldCount(int index, TradeCatalogEntry entry) {
        return index >= 0 && index < sellHeldCounts.length && entry != null ? sellHeldCounts[index] : 0;
    }

    private int countHeld(TradeCatalogEntry entry) {
        if (mc.player == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : mc.player.inventory.mainInventory) {
            if (entry.matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int addAvailableCounts(int playerCount, int linkedChestCount) {
        return playerCount > Integer.MAX_VALUE - linkedChestCount
                ? Integer.MAX_VALUE : playerCount + linkedChestCount;
    }

    private static long multiplyForDisplay(long price, int quantity) {
        if (price <= 0L || quantity <= 0 || price > Long.MAX_VALUE / quantity) {
            return 0L;
        }
        return price * (long) quantity;
    }

    /** Matches GuiCrafting/GuiFurnace: direct FontRenderer with 4210752 and no shadow pass. */
    private void drawContainerText(String text, int x, int y) {
        fontRenderer.drawString(text, x, y, VANILLA_CONTAINER_TEXT_COLOR);
    }

    private void drawCenteredContainerText(String text, int centerX, int y) {
        drawContainerText(text, centerX - fontRenderer.getStringWidth(text) / 2, y);
    }

    private static String format(long value) {
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

    private static String formatSigned(long value) {
        return (value > 0L ? "+" : value < 0L ? "-" : "") + format(Math.abs(value));
    }

    private static String formatPercent(long delta, long previous) {
        if (previous <= 0L) {
            return "—";
        }
        long hundredths = Math.round(Math.abs(delta) * 10000.0D / previous);
        return (delta > 0L ? "+" : delta < 0L ? "-" : "") + (hundredths / 100L) + "."
                + (hundredths % 100L < 10L ? "0" : "") + (hundredths % 100L) + "%";
    }

    private boolean isSellPage() {
        return page == ClientMerchantTradeViewState.PAGE_SELL;
    }

    private boolean isBuyPage() {
        return page == ClientMerchantTradeViewState.PAGE_BUY;
    }

    private boolean isEmeraldPage() {
        return page == ClientMerchantTradeViewState.PAGE_EMERALD;
    }

    /** Draw the complete vanilla panel in one operation; never assemble it from texture fragments. */
    private void drawNativePanel(int left, int top, int right, int bottom) {
        mc.getTextureManager().bindTexture(VANILLA_PANEL_TEXTURE);
        drawScaledCustomSizeModalRect(left, top, 0.0F, 0.0F, 248, 166,
                right - left, bottom - top, 256.0F, 256.0F);
    }

    /** The 18x18 cell is copied directly from the vanilla chest texture. */
    private void drawNativeSlot(int x, int y) {
        mc.getTextureManager().bindTexture(VANILLA_SLOT_TEXTURE);
        drawTexturedModalRect(x, y, VANILLA_SLOT_U, VANILLA_SLOT_V, 18, 18);
    }

    /** Coordinates are derived from ScaledResolution, including the 320x240 minimum. */
    private static final class Layout {
        private final int panelX;
        private final int panelY;
        private final int panelWidth;
        private final int panelRight;
        private final int panelBottom;
        private final int tabY;
        private final int cardTop;
        private final int cardWidth;
        private final int sliderWidth;
        private final int fieldWidth;
        private final int confirmWidth;

        private Layout(int panelX, int panelY, int panelWidth, int panelHeight, int cardTop, int cardWidth,
                       int sliderWidth, int fieldWidth, int confirmWidth) {
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelWidth = panelWidth;
            this.panelRight = panelX + panelWidth;
            this.panelBottom = panelY + panelHeight;
            this.tabY = panelY + 29;
            this.cardTop = cardTop;
            this.cardWidth = cardWidth;
            this.sliderWidth = sliderWidth;
            this.fieldWidth = fieldWidth;
            this.confirmWidth = confirmWidth;
        }

        private static Layout create(int screenWidth, int screenHeight) {
            int panelWidth = Math.max(1, Math.min(520, screenWidth - 12));
            int panelHeight = Math.max(1, Math.min(300, screenHeight - 12));
            int panelX = (screenWidth - panelWidth) / 2;
            int panelY = (screenHeight - panelHeight) / 2;
            int cardTop = panelY + 47;
            int cardWidth = Math.max(1, (panelWidth - 16) / 2);
            int fieldWidth = 25;
            int confirmWidth = Math.max(38, Math.min(52, cardWidth / 3));
            int sliderWidth = Math.max(20, cardWidth - 24 - fieldWidth - confirmWidth - 12);
            return new Layout(panelX, panelY, panelWidth, panelHeight, cardTop, cardWidth,
                    sliderWidth, fieldWidth, confirmWidth);
        }

        private int cardX(int index, boolean buying) {
            return panelX + 5 + (index / rowsPerColumn(buying)) * (cardWidth + 6);
        }

        private int cardY(int index, boolean buying) {
            return cardTop + (index % rowsPerColumn(buying)) * rowHeight(buying);
        }

        private int controlsY(int index, boolean buying) {
            return cardY(index, buying) + cardHeight(buying) - 13;
        }

        private int cardHeight(boolean buying) {
            return Math.max(32, rowHeight(buying) - 2);
        }

        private int rowHeight(boolean buying) {
            return Math.max(34, (panelBottom - cardTop - 4) / rowsPerColumn(buying));
        }

        private static int rowsPerColumn(boolean buying) {
            // The six sell offers deliberately form a balanced 3 x 2 grid.
            return buying ? 4 : 3;
        }

        private int sliderX(int cardX) {
            return cardX + 24;
        }

        private int fieldX(int cardX, int activeSliderWidth) {
            return sliderX(cardX) + activeSliderWidth + 3;
        }

        private int confirmX(int cardX) {
            return fieldX(cardX, sliderWidth) + fieldWidth + 3;
        }

        private int emeraldControlX(int index) {
            return panelX + 8 + index * (emeraldColumnWidth() + 4);
        }

        private int emeraldColumnWidth() {
            return Math.max(44, (panelWidth - 20) / 2);
        }

        private int emeraldColumnCenter(int index) {
            return emeraldControlX(index) + emeraldColumnWidth() / 2;
        }

        private int emeraldSliderWidth() {
            return Math.max(12, emeraldColumnWidth() - emeraldFieldWidth() - 3);
        }

        private int emeraldSliderX(int index) {
            return emeraldControlX(index);
        }

        private int emeraldFieldX(int index) {
            return emeraldSliderX(index) + emeraldSliderWidth() + 3;
        }

        private int emeraldFieldWidth() {
            return 25;
        }

        private int emeraldControlsY() {
            return panelBottom - 36;
        }

        private int emeraldButtonsY() {
            return panelBottom - 18;
        }

    }
}
