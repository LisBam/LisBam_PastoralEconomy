package lisbam.pastoraleconomy.event;

/** Deterministic boundary checks for the five-minute per-cow timer. */
public final class MilkCooldownSelfTest {
    private MilkCooldownSelfTest() {
    }

    public static void main(String[] args) {
        require(MilkCooldownRules.COOLDOWN_TICKS == 6000L, "five minutes must equal 6000 ticks");
        require(!MilkCooldownRules.isOnCooldown(-1L, 0L), "unset timestamp is ready");
        require(MilkCooldownRules.isOnCooldown(100L, 100L), "same tick is cooling down");
        require(MilkCooldownRules.isOnCooldown(100L, 6099L), "cooldown lasts through tick 6099");
        require(!MilkCooldownRules.isOnCooldown(100L, 6100L), "cooldown expires at 6000 ticks");
        require(!MilkCooldownRules.isOnCooldown(100L, 99L), "time rollback does not lock the cow forever");
        require(MilkCooldownRules.remainingTicks(100L, 1100L) == 5000L, "remaining timer");
        require(MilkCooldownRules.shouldCancelLocalPrediction(true, false),
                "enabled cooldown must suppress the client-side vanilla milk bucket prediction");
        require(!MilkCooldownRules.shouldCancelLocalPrediction(false, false),
                "the server must remain authoritative instead of suppressing a valid first milking");
        require(!MilkCooldownRules.shouldCancelLocalPrediction(true, true),
                "disabling cooldown must preserve vanilla client prediction");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
