package dev.rinchan.keepbutpenalty;

/** Pure durability boundary used by the death penalty pipeline. */
public final class DurabilityPenaltyPolicy {
    private DurabilityPenaltyPolicy() {}

    public static int nextDamage(int currentDamage, int maximumDamage, int loss, boolean allowZeroDurability) {
        if (maximumDamage <= 0) {
            return 0;
        }
        int limit = allowZeroDurability ? maximumDamage : maximumDamage - 1;
        long requested = (long) Math.max(0, currentDamage) + Math.max(0, loss);
        return (int) Math.min((long) limit, requested);
    }
}
