package lisbam.pastoraleconomy.entity;

import java.util.Random;

/** Stable server-side skin indices shared by merchant persistence and client rendering. */
public final class MerchantSkinCatalog {
    /** Index zero belonged to the removed legacy Steve texture and is now invalid. */
    public static final int FIRST_FARMER_SKIN = 1;
    public static final int LAST_FARMER_SKIN = 4;
    public static final int SKIN_COUNT = LAST_FARMER_SKIN - FIRST_FARMER_SKIN + 1;

    private MerchantSkinCatalog() {
    }

    public static boolean isValid(int skinIndex) {
        return skinIndex >= FIRST_FARMER_SKIN && skinIndex <= LAST_FARMER_SKIN;
    }

    public static int normalize(int skinIndex) {
        return isValid(skinIndex) ? skinIndex : FIRST_FARMER_SKIN;
    }

    public static int randomSkin(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random source is required.");
        }
        return FIRST_FARMER_SKIN + random.nextInt(SKIN_COUNT);
    }
}
