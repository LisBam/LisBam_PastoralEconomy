package lisbam.pastoraleconomy.equipment;

/** Fixed, deterministic balance values for the shoulder Feather Wings. */
public final class FeatherWingsRules {
    public static final int FLIGHT_TICKS_PER_DURABILITY = 20;
    public static final int MAX_DAMAGE = 500;
    public static final int DAMAGE_REPAIRED_PER_FEATHER = MAX_DAMAGE / 100;
    public static final int MAX_TRACKED_FLIGHT_DISTANCE_CM = 1000;

    private FeatherWingsRules() {
    }

    public static int getFeathersRequired(int currentDamage, int availableFeathers) {
        if (currentDamage <= 0 || availableFeathers <= 0) {
            return 0;
        }
        int required = (currentDamage + DAMAGE_REPAIRED_PER_FEATHER - 1) / DAMAGE_REPAIRED_PER_FEATHER;
        return Math.min(required, availableFeathers);
    }

    public static int getRepairedDamage(int currentDamage, int feathers) {
        if (currentDamage <= 0 || feathers <= 0) {
            return 0;
        }
        return Math.min(currentDamage, feathers * DAMAGE_REPAIRED_PER_FEATHER);
    }

    /** One material-repair operation follows vanilla's minimum one-level cost. */
    public static int getAnvilExperienceCost(int currentRepairCost, boolean renamed) {
        int base = Math.max(1, currentRepairCost + 1);
        return renamed && base < Integer.MAX_VALUE ? base + 1 : base;
    }

    /** Mirrors vanilla's output prior-work progression without overflowing. */
    public static int getNextRepairCost(int currentRepairCost) {
        if (currentRepairCost <= 0) {
            return 1;
        }
        return currentRepairCost > (Integer.MAX_VALUE - 1) / 2
                ? Integer.MAX_VALUE : currentRepairCost * 2 + 1;
    }

    /** Two times vanilla walking exhaustion: 0.02 exhaustion for each block flown. */
    public static float getFlightExhaustion(int travelledCentimeters) {
        return travelledCentimeters <= 0 || travelledCentimeters > MAX_TRACKED_FLIGHT_DISTANCE_CM
                ? 0.0F : travelledCentimeters * 0.0002F;
    }
}
