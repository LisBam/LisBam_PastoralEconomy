package lisbam.pastoraleconomy.entity;

/** Stable server-side skin indices shared by merchant persistence and client rendering. */
public final class MerchantSkinCatalog {
    public static final int DEFAULT_STEVE = 0;
    public static final int SKIN_COUNT = 5;

    private MerchantSkinCatalog() {
    }

    public static boolean isValid(int skinIndex) {
        return skinIndex >= DEFAULT_STEVE && skinIndex < SKIN_COUNT;
    }

    public static int normalize(int skinIndex) {
        return isValid(skinIndex) ? skinIndex : DEFAULT_STEVE;
    }
}
