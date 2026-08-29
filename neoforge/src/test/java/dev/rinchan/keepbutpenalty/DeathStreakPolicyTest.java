package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DeathStreakPolicyTest {
    private static final List<Integer> WMF_SECONDS = List.of(0, 5, 10, 15, 20);

    @Test
    void firstDeathIsFreeAndRepeatedDeathsAdvanceThroughTheConfiguredSchedule() {
        var first = DeathStreakPolicy.advance(DeathStreakPolicy.State.empty(), 1_000L, 12_000L);
        assertEquals(1, first.deaths());
        assertEquals(0, DeathStreakPolicy.durationSeconds(WMF_SECONDS, first.deaths()));
        var second = DeathStreakPolicy.advance(first, 1_100L, 12_000L);
        var third = DeathStreakPolicy.advance(second, 1_200L, 12_000L);
        var fourth = DeathStreakPolicy.advance(third, 1_300L, 12_000L);
        var fifth = DeathStreakPolicy.advance(fourth, 1_400L, 12_000L);
        var sixth = DeathStreakPolicy.advance(fifth, 1_500L, 12_000L);
        assertEquals(5, DeathStreakPolicy.durationSeconds(WMF_SECONDS, second.deaths()));
        assertEquals(10, DeathStreakPolicy.durationSeconds(WMF_SECONDS, third.deaths()));
        assertEquals(15, DeathStreakPolicy.durationSeconds(WMF_SECONDS, fourth.deaths()));
        assertEquals(20, DeathStreakPolicy.durationSeconds(WMF_SECONDS, fifth.deaths()));
        assertEquals(20, DeathStreakPolicy.durationSeconds(WMF_SECONDS, sixth.deaths()));
    }

    @Test
    void tenMinutesWithoutADeathStartsAgainAtTheFreeStage() {
        var previous = new DeathStreakPolicy.State(4, 1_000L);
        assertEquals(1, DeathStreakPolicy.advance(previous, 13_000L, 12_000L).deaths());
        assertEquals(5, DeathStreakPolicy.advance(previous, 12_999L, 12_000L).deaths());
    }

    @Test
    void deathCountSaturatesInsteadOfWrappingAtTheIntegerLimit() {
        var previous = new DeathStreakPolicy.State(Integer.MAX_VALUE, 1_000L);
        assertEquals(Integer.MAX_VALUE, DeathStreakPolicy.advance(previous, 1_001L, 12_000L).deaths());
    }

    @Test
    void emptySchedulePreservesLegacyPerEffectDuration() {
        assertEquals(-1, DeathStreakPolicy.durationSeconds(List.of(), 4));
    }
}
