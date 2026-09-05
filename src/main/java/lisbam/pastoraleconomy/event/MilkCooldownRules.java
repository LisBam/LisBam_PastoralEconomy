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
}
