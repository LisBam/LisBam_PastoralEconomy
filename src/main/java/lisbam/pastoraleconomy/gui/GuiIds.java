package lisbam.pastoraleconomy.gui;

/**
 * The single allocation point for stable GUI IDs.
 */
public final class GuiIds {
    public static final int MARKET_BOOK = 0;
    public static final int MERCHANT_TRADE = 1;
    public static final int CRAB_TRAP = 2;
    public static final int TRANSPORT_STATION = 3;
    public static final int BACKPACK = 4;

    private GuiIds() {
    }

    public static void validate(int guiId) {
        if (guiId != MARKET_BOOK && guiId != MERCHANT_TRADE && guiId != CRAB_TRAP
                && guiId != TRANSPORT_STATION && guiId != BACKPACK) {
            throw new IllegalArgumentException("Unknown GUI ID: " + guiId);
        }
    }
}
