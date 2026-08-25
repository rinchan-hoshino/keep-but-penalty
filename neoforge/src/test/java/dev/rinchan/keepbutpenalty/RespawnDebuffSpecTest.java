package dev.rinchan.keepbutpenalty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespawnDebuffSpecTest {
    @Test
    void parsesEffectDurationAndUserFacingLevel() {
        RespawnDebuffSpec spec = RespawnDebuffSpec.parse("minecraft:weakness,60,1");
        assertEquals("minecraft:weakness", spec.effectId().toString());
        assertEquals(1200, spec.durationTicks());
        assertEquals(0, spec.amplifier());
    }

    @Test
    void validatesTheWholeTuple() {
        assertTrue(RespawnDebuffSpec.isValid("minecraft:mining_fatigue,60,1"));
        assertFalse(RespawnDebuffSpec.isValid("minecraft:weakness,0,1"));
        assertFalse(RespawnDebuffSpec.isValid("minecraft:weakness,60,0"));
        assertFalse(RespawnDebuffSpec.isValid("not an id,60,1"));
        assertFalse(RespawnDebuffSpec.isValid("minecraft:weakness,60"));
        assertThrows(IllegalArgumentException.class, () -> RespawnDebuffSpec.parse("minecraft:weakness,60"));
    }
}
