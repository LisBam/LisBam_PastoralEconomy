package lisbam.pastoraleconomy.config;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

/**
 * Common configuration whose market value is only read by the authoritative
 * logical server. Client configuration screens never recalculate a snapshot.
 */
public final class ModSettings {
    public static final String MARKET_CATEGORY = "market";
    private static final String KEY_MORE_STABLE_MARKET_VOLATILITY = "moreStableMarketVolatility";
    private static final boolean DEFAULT_MORE_STABLE_MARKET_VOLATILITY = false;

    private static Configuration configuration;
    private static Property moreStableMarketVolatility;

    private ModSettings() {
    }

    public static synchronized void initialize(File configurationFile) {
        configuration = new Configuration(configurationFile);
        configuration.getCategory(MARKET_CATEGORY)
                .setLanguageKey("config." + LisBamPastoralEconomy.MODID + ".market");
        moreStableMarketVolatility = configuration.get(
                MARKET_CATEGORY,
                KEY_MORE_STABLE_MARKET_VOLATILITY,
                DEFAULT_MORE_STABLE_MARKET_VOLATILITY,
                "Server-authoritative. False uses the chained daily market; true restores the legacy independent "
                        + "triangular daily prices. Changing this never recalculates an existing market day."
        ).setLanguageKey("config." + LisBamPastoralEconomy.MODID + ".market.more_stable_market_volatility");
        save();
    }

    /**
     * True intentionally restores the pre-maintenance price formula. The
     * default false path advances from the snapshot saved for the prior day.
     */
    public static synchronized boolean isMoreStableMarketVolatility() {
        return moreStableMarketVolatility != null
                && moreStableMarketVolatility.getBoolean(DEFAULT_MORE_STABLE_MARKET_VOLATILITY);
    }

    public static synchronized Configuration getConfiguration() {
        if (configuration == null) {
            throw new IllegalStateException("Mod settings have not been initialized.");
        }
        return configuration;
    }

    public static synchronized void save() {
        if (configuration != null && configuration.hasChanged()) {
            configuration.save();
        }
    }
}
