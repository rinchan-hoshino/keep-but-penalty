package dev.rinchan.keepbutpenalty.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HungerPenaltyPolicyTest {
    @Test
    void preservesHungerBelowFullWithFloorAndSaturationClamp() {
        HungerPenaltyPolicy.Result low = HungerPenaltyPolicy.computeRespawnHunger(2, 1.5f, 6);
        assertTrue(low.apply());
        assertEquals(6, low.foodLevel());
        assertEquals(1.5f, low.saturationLevel());

        HungerPenaltyPolicy.Result mid = HungerPenaltyPolicy.computeRespawnHunger(12, 18.0f, 6);
        assertTrue(mid.apply());
        assertEquals(12, mid.foodLevel());
        assertEquals(12.0f, mid.saturationLevel());
    }

    @Test
    void fullHungerDoesNotTriggerOverride() {
        HungerPenaltyPolicy.Result full = HungerPenaltyPolicy.computeRespawnHunger(20, 5.0f, 6);
        assertFalse(full.apply());
    }

    @Test
    void zeroFloorAllowsStarvationPreservationWhenConfigured() {
        HungerPenaltyPolicy.Result res = HungerPenaltyPolicy.computeRespawnHunger(0, 0.0f, 0);
        assertTrue(res.apply());
        assertEquals(0, res.foodLevel());
        assertEquals(0.0f, res.saturationLevel());
    }
}
