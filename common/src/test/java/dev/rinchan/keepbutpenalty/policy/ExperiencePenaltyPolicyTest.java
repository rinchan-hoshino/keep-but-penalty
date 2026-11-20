package dev.rinchan.keepbutpenalty.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExperiencePenaltyPolicyTest {
    @Test
    void roundTripExactAcrossLevelsZeroToFiveHundred() {
        for (int level = 0; level <= 500; level++) {
            int bar = ExperiencePenaltyPolicy.xpToNextLevel(level);
            for (int offset : new int[]{0, 1, bar / 2, bar - 1}) {
                int expectedTotal = ExperiencePenaltyPolicy.xpForLevel(level) + offset;
                ExperiencePenaltyPolicy.Result res = ExperiencePenaltyPolicy.fromTotalExperience(expectedTotal);
                assertEquals(level, res.level(), "level mismatch at total=" + expectedTotal);
                assertEquals(expectedTotal, res.totalExperience(), "total mismatch at level=" + level);
                assertTrue(res.progress() >= 0.0f && res.progress() < 1.0f,
                        "progress out of range: " + res.progress());
                assertEquals(expectedTotal,
                        ExperiencePenaltyPolicy.totalExperience(res.level(), res.progress()),
                        "round-trip total mismatch at level=" + level + " offset=" + offset);
            }
        }
    }

    @Test
    void defaultSeventyFivePercentRetention() {
        // Level 30 exact = 1395 XP; 75% = floor(1046.25) = 1046 XP; lost = 349 XP
        assertEquals(1395, ExperiencePenaltyPolicy.xpForLevel(30));
        ExperiencePenaltyPolicy.Result res = ExperiencePenaltyPolicy.applyRetention(30, 0.0f, 0.75D);
        assertEquals(1046, res.totalExperience());
        assertEquals(349, res.lostExperience());
        assertEquals(1046, ExperiencePenaltyPolicy.totalExperience(res.level(), res.progress()));
    }

    @Test
    void zeroAndFullRetentionBoundaries() {
        ExperiencePenaltyPolicy.Result zero = ExperiencePenaltyPolicy.applyRetention(40, 0.5f, 0.0D);
        assertEquals(0, zero.level());
        assertEquals(0.0f, zero.progress());
        assertEquals(0, zero.totalExperience());
        assertTrue(zero.lostExperience() > 0);

        int before = ExperiencePenaltyPolicy.totalExperience(40, 0.5f);
        ExperiencePenaltyPolicy.Result full = ExperiencePenaltyPolicy.applyRetention(40, 0.5f, 1.0D);
        assertEquals(before, full.totalExperience());
        assertEquals(0, full.lostExperience());
    }

    @Test
    void zeroXpPlayerLosesNothing() {
        ExperiencePenaltyPolicy.Result res = ExperiencePenaltyPolicy.applyRetention(0, 0.0f, 0.75D);
        assertEquals(0, res.level());
        assertEquals(0.0f, res.progress());
        assertEquals(0, res.totalExperience());
        assertEquals(0, res.lostExperience());
    }
}
