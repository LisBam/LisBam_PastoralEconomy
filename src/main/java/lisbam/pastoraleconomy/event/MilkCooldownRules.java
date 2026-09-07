package lisbam.pastoraleconomy.event;

/** Pure world-day rules for the shared daily cow milking refresh. */
public final class MilkCooldownRules {
    public static final long TICKS_PER_DAY = 24000L;

    private MilkCooldownRules() {
    }

    public static long getWorldDay(long worldTime) {
        return Math.max(0L, worldTime) / TICKS_PER_DAY;
    }

    /** A cow can be milked once for each current world day. */
    public static boolean isOnCooldown(long lastMilkedDay, long currentWorldDay) {
        return lastMilkedDay == currentWorldDay;
    }

    /** Enabled cooldown owns the transaction on the server and blocks client prediction. */
    public static boolean shouldTakeOwnership(boolean cooldownDisabled) {
        return !cooldownDisabled;
    }

    /** A cooled target is rejected only when the feature is enabled. */
    public static boolean shouldReject(boolean cooldownDisabled, boolean onCooldown) {
        return !cooldownDisabled && onCooldown;
    }

    /** Persist the world day only after an owned server transaction succeeds. */
    public static boolean shouldRecordSuccess(boolean cooldownDisabled, boolean onCooldown,
                                              boolean transactionSucceeded) {
        return !cooldownDisabled && !onCooldown && transactionSucceeded;
    }

    /** Client inventory mutation is always deferred until the server response. */
    public static boolean shouldCancelLocalPrediction(boolean logicalClient) {
        return logicalClient;
    }
}
