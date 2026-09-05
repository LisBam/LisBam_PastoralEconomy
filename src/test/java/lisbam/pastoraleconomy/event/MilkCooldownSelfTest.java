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
        require(MilkCooldownRules.shouldTakeOwnership(false),
                "enabled cooldown must use the server-owned milk transaction");
        require(!MilkCooldownRules.shouldTakeOwnership(true),
                "disabling cooldown must preserve the unmodified vanilla transaction");
        require(MilkCooldownRules.shouldReject(false, true),
                "a cooled cow must be rejected without producing milk");
        require(!MilkCooldownRules.shouldReject(true, true),
                "the bypass setting must allow the vanilla transaction");
        require(MilkCooldownRules.shouldRecordSuccess(false, false, true),
                "a successful owned transaction records the cow timestamp");
        require(!MilkCooldownRules.shouldRecordSuccess(false, false, false),
                "a failed transaction must not record a cooldown timestamp");
        require(!MilkCooldownRules.shouldRecordSuccess(false, true, true),
                "a rejected cooled transaction must not record another timestamp");
        require(MilkCooldownRules.shouldCancelLocalPrediction(true),
                "the client must never predict a milk bucket before the server responds");
        require(!MilkCooldownRules.shouldCancelLocalPrediction(false),
                "server interaction must remain able to complete a real transaction");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
