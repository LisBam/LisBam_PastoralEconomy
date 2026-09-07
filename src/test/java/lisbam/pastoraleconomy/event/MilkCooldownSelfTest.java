package lisbam.pastoraleconomy.event;

/** Deterministic boundary checks for the once-per-world-day cow refresh. */
public final class MilkCooldownSelfTest {
    private MilkCooldownSelfTest() {
    }

    public static void main(String[] args) {
        require(MilkCooldownRules.TICKS_PER_DAY == 24000L, "a Minecraft day must equal 24000 ticks");
        require(MilkCooldownRules.getWorldDay(0L) == 0L, "world starts on day zero");
        require(MilkCooldownRules.getWorldDay(23999L) == 0L, "day zero includes tick 23999");
        require(MilkCooldownRules.getWorldDay(24000L) == 1L, "day one starts at tick 24000");
        require(MilkCooldownRules.getWorldDay(-1L) == 0L, "negative time cannot produce a negative day");
        require(!MilkCooldownRules.isOnCooldown(-1L, 4L), "an unmilked cow is ready");
        require(MilkCooldownRules.isOnCooldown(4L, 4L), "a cow is unavailable for the rest of its milk day");
        require(!MilkCooldownRules.isOnCooldown(4L, 5L), "all cows refresh together on the next day");
        require(MilkCooldownRules.shouldTakeOwnership(false),
                "enabled cooldown must use the server-owned milk transaction");
        require(!MilkCooldownRules.shouldTakeOwnership(true),
                "disabling cooldown must preserve the unmodified vanilla transaction");
        require(MilkCooldownRules.shouldReject(false, true),
                "a cooled cow must be rejected without producing milk");
        require(!MilkCooldownRules.shouldReject(true, true),
                "the bypass setting must allow the vanilla transaction");
        require(MilkCooldownRules.shouldRecordSuccess(false, false, true),
                "a successful owned transaction records the cow milk day");
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
