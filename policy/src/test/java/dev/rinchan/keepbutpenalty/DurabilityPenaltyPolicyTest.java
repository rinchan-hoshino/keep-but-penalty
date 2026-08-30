package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DurabilityPenaltyPolicyTest {
    @Test
    void matchesALongReferenceAcrossDamageAndLossBoundaries() {
        int[] damages = {0, 1, 10, 999, 1_000, Integer.MAX_VALUE};
        int[] maximums = {0, 1, 2, 1_000, Integer.MAX_VALUE};
        int[] losses = {0, 1, 80, 1_000, Integer.MAX_VALUE};

        for (int damage : damages) {
            for (int maximum : maximums) {
                for (int loss : losses) {
                    for (boolean allowZeroDurability : new boolean[] {false, true}) {
                        int actual = DurabilityPenaltyPolicy.nextDamage(
                            damage, maximum, loss, allowZeroDurability
                        );
                        int limit = maximum <= 0
                            ? 0
                            : allowZeroDurability ? maximum : maximum - 1;
                        int expected = (int) Math.min(
                            (long) limit,
                            (long) Math.max(0, damage) + Math.max(0, loss)
                        );
                        assertEquals(expected, actual, () ->
                            "damage=" + damage + ", max=" + maximum + ", loss=" + loss
                                + ", allowZero=" + allowZeroDurability
                        );
                        assertTrue(actual >= 0 && actual <= limit);
                    }
                }
            }
        }
    }

    @Test
    void hugeLossSaturatesInsteadOfWrappingNegative() {
        assertEquals(999, DurabilityPenaltyPolicy.nextDamage(10, 1_000, Integer.MAX_VALUE, false));
        assertEquals(1_000, DurabilityPenaltyPolicy.nextDamage(10, 1_000, Integer.MAX_VALUE, true));
    }
}
