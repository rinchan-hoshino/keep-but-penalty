package dev.rinchan.keepbutpenalty.policy;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeathStreakAndDebuffTest {
    @Test
    void deathStreakIncrementsResetsAndSaturates() {
        DeathStreakPolicy.State first = DeathStreakPolicy.advance(DeathStreakPolicy.State.empty(), 100L, 12_000L);
        assertEquals(1, first.deaths());

        DeathStreakPolicy.State second = DeathStreakPolicy.advance(first, 500L, 12_000L);
        assertEquals(2, second.deaths());

        DeathStreakPolicy.State reset = DeathStreakPolicy.advance(second, 12_500L, 12_000L);
        assertEquals(1, reset.deaths());

        DeathStreakPolicy.State saturated = DeathStreakPolicy.advance(
                new DeathStreakPolicy.State(Integer.MAX_VALUE, 100L), 200L, 12_000L);
        assertEquals(Integer.MAX_VALUE, saturated.deaths());
    }

    @Test
    void progressiveScheduleSelectsDurationByDeathCount() {
        List<Integer> schedule = List.of(0, 5, 10, 20);
        assertEquals(0, DeathStreakPolicy.durationSeconds(schedule, 1));
        assertEquals(5, DeathStreakPolicy.durationSeconds(schedule, 2));
        assertEquals(10, DeathStreakPolicy.durationSeconds(schedule, 3));
        assertEquals(20, DeathStreakPolicy.durationSeconds(schedule, 4));
        assertEquals(20, DeathStreakPolicy.durationSeconds(schedule, 10));
        assertEquals(-1, DeathStreakPolicy.durationSeconds(List.of(), 1));
    }

    @Test
    void debuffSpecParsesAndRejectsInvalidEntries() {
        RespawnDebuffSpec spec = RespawnDebuffSpec.parse("minecraft:weakness,60,2");
        assertEquals("minecraft:weakness", spec.effectId());
        assertEquals(1200, spec.durationTicks());
        assertEquals(1, spec.amplifier());

        assertFalse(RespawnDebuffSpec.isValid("minecraft:weakness,0,1"));
        assertFalse(RespawnDebuffSpec.isValid("minecraft:weakness,60,0"));
        assertFalse(RespawnDebuffSpec.isValid("not_an_id,60,1"));
    }
}
