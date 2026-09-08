package lisbam.pastoraleconomy.client;

/** Deterministic policy checks for the physical-client shoulder-backpack shortcut. */
public final class ShoulderBackpackKeySelfTest {
    private ShoulderBackpackKeySelfTest() {
    }

    public static void main(String[] args) {
        check(ShoulderBackpackKeyHandler.isOpenRequestAllowed(true, true, true),
                "B opens an equipped shoulder backpack from gameplay or the player inventory");
        check(!ShoulderBackpackKeyHandler.isOpenRequestAllowed(false, true, true),
                "an unrelated key cannot open shoulder storage");
        check(!ShoulderBackpackKeyHandler.isOpenRequestAllowed(true, false, true),
                "B cannot interrupt another custom Container");
        check(!ShoulderBackpackKeyHandler.isOpenRequestAllowed(true, true, false),
                "B cannot request storage without an equipped backpack");
        System.out.println("shoulderBackpackKeySelfTest PASS");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
