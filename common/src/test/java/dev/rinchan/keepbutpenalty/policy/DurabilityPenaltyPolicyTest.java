package dev.rinchan.keepbutpenalty.policy;

import dev.rinchan.keepbutpenalty.config.DurabilityPenaltyMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DurabilityPenaltyPolicyTest {
    @Test
    void percentModeComputesRoundedLossWithMinOneFloor() {
        assertEquals(156, DurabilityPenaltyPolicy.computeLoss(1561, DurabilityPenaltyMode.PERCENT_MAX, 0.10D, 80));
        assertEquals(1, DurabilityPenaltyPolicy.computeLoss(5, DurabilityPenaltyMode.PERCENT_MAX, 0.01D, 80));
        assertEquals(0, DurabilityPenaltyPolicy.computeLoss(500, DurabilityPenaltyMode.PERCENT_MAX, 0.0D, 80));
    }

    @Test
    void flatModeUsesFixedPoints() {
        assertEquals(80, DurabilityPenaltyPolicy.computeLoss(1561, DurabilityPenaltyMode.FLAT, 0.10D, 80));
        assertEquals(0, DurabilityPenaltyPolicy.computeLoss(1561, DurabilityPenaltyMode.FLAT, 0.10D, 0));
    }

    @Test
    void deterministicSetDamageBelowMaxDamage() {
        DurabilityPenaltyPolicy.Outcome out = DurabilityPenaltyPolicy.evaluate(
                100, 1561, DurabilityPenaltyMode.PERCENT_MAX, 0.10D, 80, true, false);
        assertEquals(DurabilityPenaltyPolicy.Action.SET_DAMAGE, out.action());
        assertEquals(256, out.nextDamage());
    }

    @Test
    void elytraAlwaysClampsAtMaxDamageMinusOne() {
        DurabilityPenaltyPolicy.Outcome out = DurabilityPenaltyPolicy.evaluate(
                420, 432, DurabilityPenaltyMode.PERCENT_MAX, 0.10D, 80, true, true);
        assertEquals(DurabilityPenaltyPolicy.Action.SET_DAMAGE, out.action());
        assertEquals(431, out.nextDamage());

        DurabilityPenaltyPolicy.Outcome alreadyBrokenWing = DurabilityPenaltyPolicy.evaluate(
                431, 432, DurabilityPenaltyMode.PERCENT_MAX, 0.10D, 80, true, true);
        assertEquals(DurabilityPenaltyPolicy.Action.NONE, alreadyBrokenWing.action());
    }

    @Test
    void allowBreakFalseClampsAtOneDurabilityRemaining() {
        DurabilityPenaltyPolicy.Outcome out = DurabilityPenaltyPolicy.evaluate(
                240, 250, DurabilityPenaltyMode.FLAT, 0.10D, 80, false, false);
        assertEquals(DurabilityPenaltyPolicy.Action.SET_DAMAGE, out.action());
        assertEquals(249, out.nextDamage());

        DurabilityPenaltyPolicy.Outcome alreadyAtFloor = DurabilityPenaltyPolicy.evaluate(
                249, 250, DurabilityPenaltyMode.FLAT, 0.10D, 80, false, false);
        assertEquals(DurabilityPenaltyPolicy.Action.NONE, alreadyAtFloor.action());
    }

    @Test
    void allowBreakTrueTriggersStandardHurtAndBreakAndSkipsAlreadyBroken() {
        DurabilityPenaltyPolicy.Outcome out = DurabilityPenaltyPolicy.evaluate(
                240, 250, DurabilityPenaltyMode.FLAT, 0.10D, 80, true, false);
        assertEquals(DurabilityPenaltyPolicy.Action.HURT_AND_BREAK, out.action());
        assertEquals(249, out.nextDamage());
        assertEquals(250 * 16, out.breakDamageAmount());

        // Already at or above maxDamage (e.g. broken state from an anti-break mod) -> skip cleanly
        DurabilityPenaltyPolicy.Outcome alreadyBroken = DurabilityPenaltyPolicy.evaluate(
                250, 250, DurabilityPenaltyMode.FLAT, 0.10D, 80, true, false);
        assertEquals(DurabilityPenaltyPolicy.Action.NONE, alreadyBroken.action());
    }

    @Test
    void handlesExtremeFlatLossWithoutIntegerOverflow() {
        DurabilityPenaltyPolicy.Outcome out = DurabilityPenaltyPolicy.evaluate(
                100, 1561, DurabilityPenaltyMode.FLAT, 0.10D, Integer.MAX_VALUE, true, false);
        assertEquals(DurabilityPenaltyPolicy.Action.HURT_AND_BREAK, out.action());
        assertEquals(1560, out.nextDamage());
    }
}
