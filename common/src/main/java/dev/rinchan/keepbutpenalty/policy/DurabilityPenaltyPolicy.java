package dev.rinchan.keepbutpenalty.policy;

import dev.rinchan.keepbutpenalty.config.DurabilityPenaltyMode;

/**
 * Pure durability penalty computation following the vanilla-decoupled breaking protocol.
 */
public final class DurabilityPenaltyPolicy {
    private DurabilityPenaltyPolicy() {}

    public enum Action {
        NONE,
        SET_DAMAGE,
        HURT_AND_BREAK
    }

    public record Outcome(Action action, int nextDamage, int breakDamageAmount) {
        public static Outcome none() {
            return new Outcome(Action.NONE, 0, 0);
        }
    }

    public static int computeLoss(int maxDamage, DurabilityPenaltyMode mode, double percent, int flat) {
        if (maxDamage <= 0) {
            return 0;
        }
        if (mode == DurabilityPenaltyMode.FLAT) {
            return Math.max(0, flat);
        }
        double clampedPercent = clampPercent(percent);
        if (clampedPercent <= 0.0D) {
            return 0;
        }
        return (int) Math.max(1L, Math.round(maxDamage * clampedPercent));
    }

    public static Outcome evaluate(
            int currentDamage,
            int maxDamage,
            DurabilityPenaltyMode mode,
            double percent,
            int flat,
            boolean allowBreak,
            boolean isElytra
    ) {
        if (maxDamage <= 0) {
            return Outcome.none();
        }
        int loss = computeLoss(maxDamage, mode, percent, flat);
        if (loss <= 0) {
            return Outcome.none();
        }

        long target = (long) currentDamage + (long) loss;
        int safeCap = Math.max(0, maxDamage - 1);

        if (isElytra) {
            if (currentDamage >= safeCap) {
                return Outcome.none();
            }
            int next = (int) Math.min(safeCap, target);
            return next > currentDamage ? new Outcome(Action.SET_DAMAGE, next, 0) : Outcome.none();
        }

        if (target < maxDamage) {
            return new Outcome(Action.SET_DAMAGE, (int) target, 0);
        }

        if (!allowBreak) {
            if (currentDamage >= safeCap) {
                return Outcome.none();
            }
            return new Outcome(Action.SET_DAMAGE, safeCap, 0);
        }

        // allowBreak == true and target >= maxDamage:
        // If already at or past maxDamage (e.g., marked broken by an anti-break mod), leave untouched.
        if (currentDamage >= maxDamage) {
            return Outcome.none();
        }

        // Set to maxDamage - 1 first, then invoke vanilla hurtAndBreak with ample deterministic damage
        // so Unbreaking enchantments still cross the break threshold cleanly.
        int breakAmount = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) maxDamage * 16L));
        return new Outcome(Action.HURT_AND_BREAK, safeCap, breakAmount);
    }

    public static double clampPercent(double percent) {
        if (Double.isNaN(percent)) return 0.10D;
        return Math.max(0.0D, Math.min(1.0D, percent));
    }
}
