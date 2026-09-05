package lisbam.pastoraleconomy.event;

/** Pure timing rules for the per-cow milking cooldown. */
public final class MilkCooldownRules {
    public static final long COOLDOWN_TICKS = 5L * 60L * 20L;

    private MilkCooldownRules() {
    }

    public static boolean isOnCooldown(long lastMilkedTick, long currentTick) {
        if (lastMilkedTick < 0L || currentTick < lastMilkedTick) {
            return false;
        }
        return currentTick - lastMilkedTick < COOLDOWN_TICKS;
    }

    public static long remainingTicks(long lastMilkedTick, long currentTick) {
        if (!isOnCooldown(lastMilkedTick, currentTick)) {
            return 0L;
        }
        return COOLDOWN_TICKS - (currentTick - lastMilkedTick);
    }

    /** Enabled cooldown owns the transaction on the server and blocks client prediction. */
    public static boolean shouldTakeOwnership(boolean cooldownDisabled) {
        return !cooldownDisabled;
    }

    /** A cooled target is rejected only when the feature is enabled. */
    public static boolean shouldReject(boolean cooldownDisabled, boolean onCooldown) {
        return !cooldownDisabled && onCooldown;
    }

    /** Persist the timestamp only after an owned server transaction succeeds. */
    public static boolean shouldRecordSuccess(boolean cooldownDisabled, boolean onCooldown,
                                              boolean transactionSucceeded) {
        return !cooldownDisabled && !onCooldown && transactionSucceeded;
    }

    /** Client inventory mutation is always deferred until the server response. */
    public static boolean shouldCancelLocalPrediction(boolean logicalClient) {
        return logicalClient;
    }
}
