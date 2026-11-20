package dev.rinchan.keepbutpenalty.policy;

/**
 * Pure anti-suicide hunger preservation policy.
 * Preserves below-full hunger across death with a configurable floor so players
 * cannot reset hunger by dying while avoiding starvation loops.
 */
public final class HungerPenaltyPolicy {
    public static final int MAX_FOOD_LEVEL = 20;

    private HungerPenaltyPolicy() {}

    public record Result(boolean apply, int foodLevel, float saturationLevel) {
        public static Result none() {
            return new Result(false, MAX_FOOD_LEVEL, 5.0f);
        }
    }

    public static Result computeRespawnHunger(int beforeFood, float beforeSaturation, int minRespawnFoodLevel) {
        int clampedFloor = clampFoodLevel(minRespawnFoodLevel);
        int clampedBefore = clampFoodLevel(beforeFood);
        if (clampedBefore >= MAX_FOOD_LEVEL) {
            return Result.none();
        }
        int newFood = Math.max(clampedFloor, clampedBefore);
        float cleanSaturation = Float.isNaN(beforeSaturation) ? 0.0f : Math.max(0.0f, beforeSaturation);
        float newSaturation = Math.min(cleanSaturation, (float) newFood);
        return new Result(true, newFood, newSaturation);
    }

    public static int clampFoodLevel(int level) {
        return Math.max(0, Math.min(MAX_FOOD_LEVEL, level));
    }
}
