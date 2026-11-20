package dev.rinchan.keepbutpenalty.policy;

import dev.rinchan.keepbutpenalty.config.DurabilityPenaltyMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fixed-seed randomized property fuzzing (>= 1,000 iterations per invariant)
 * covering XP exact inverse math, durability bounds, hunger anti-suicide floor,
 * and consecutive death-streak progressive debuff schedules.
 */
class PenaltyInvariantsPropertyTest {
    private static final int ITERATIONS = 2_000;

    @Test
    void experienceInverseAndRetentionNeverNegativeOrOverflow() {
        Random rng = new Random(0x5EED_0001_2026L);
        for (int i = 0; i < ITERATIONS; i++) {
            int level = rng.nextInt(201); // 0..200 inclusive
            float progress = rng.nextFloat(); // [0.0f, 1.0f)
            double keepRatio = rng.nextDouble(); // [0.0, 1.0)
            if (i % 100 == 0) {
                keepRatio = 0.0D;
            } else if (i % 100 == 1) {
                keepRatio = 1.0D;
            } else if (i % 100 == 2) {
                keepRatio = 0.75D;
            }

            int before = ExperiencePenaltyPolicy.totalExperience(level, progress);
            assertTrue(before >= 0, "totalExperience must be non-negative at iter=" + i);

            // Exact inverse round-trip for any valid total XP
            ExperiencePenaltyPolicy.Result inverse = ExperiencePenaltyPolicy.fromTotalExperience(before);
            assertTrue(inverse.level() >= 0, "inverse level negative at iter=" + i);
            assertTrue(inverse.progress() >= 0.0f && inverse.progress() < 1.0f,
                    "inverse progress out of [0,1) at iter=" + i + ": " + inverse.progress());
            assertEquals(before, inverse.totalExperience(),
                    "inverse totalExperience mismatch at iter=" + i);
            assertEquals(before, ExperiencePenaltyPolicy.totalExperience(inverse.level(), inverse.progress()),
                    "round-trip totalExperience mismatch at iter=" + i);

            // Retention invariants
            ExperiencePenaltyPolicy.Result res = ExperiencePenaltyPolicy.applyRetention(level, progress, keepRatio);
            int expectedRemaining = (int) Math.floor(before * keepRatio);
            assertEquals(expectedRemaining, res.totalExperience(),
                    "remaining XP mismatch at iter=" + i);
            assertEquals(before - expectedRemaining, res.lostExperience(),
                    "lost XP mismatch at iter=" + i);
            assertTrue(res.level() >= 0, "retained level negative at iter=" + i);
            assertTrue(res.progress() >= 0.0f && res.progress() < 1.0f,
                    "retained progress out of [0,1) at iter=" + i + ": " + res.progress());
            assertTrue(res.totalExperience() >= 0 && res.totalExperience() <= before,
                    "retained total out of bounds at iter=" + i);
            assertTrue(res.lostExperience() >= 0 && res.lostExperience() <= before,
                    "lost XP out of bounds at iter=" + i);
            assertEquals(before, res.totalExperience() + res.lostExperience(),
                    "conservation of XP violated at iter=" + i);
            assertEquals(res.totalExperience(),
                    ExperiencePenaltyPolicy.totalExperience(res.level(), res.progress()),
                    "retained round-trip mismatch at iter=" + i);

            // Monotonicity in keepRatio
            double higherRatio = Math.min(1.0D, keepRatio + rng.nextDouble() * (1.0D - keepRatio));
            ExperiencePenaltyPolicy.Result higher = ExperiencePenaltyPolicy.applyRetention(level, progress, higherRatio);
            assertTrue(higher.totalExperience() >= res.totalExperience(),
                    "retention must be monotonic in keepRatio at iter=" + i);
        }
    }

    @Test
    void durabilityAllowBreakFalseAlwaysLeavesAtLeastOneDurability() {
        Random rng = new Random(0x5EED_0002_2026L);
        for (int i = 0; i < ITERATIONS; i++) {
            int maxDamage = 1 + rng.nextInt(10_000); // 1..10000
            int currentDamage = rng.nextInt(maxDamage); // 0..maxDamage-1 (unbroken item)
            DurabilityPenaltyMode mode = rng.nextBoolean()
                    ? DurabilityPenaltyMode.PERCENT_MAX
                    : DurabilityPenaltyMode.FLAT;
            double percent = rng.nextDouble(); // [0.0, 1.0)
            if (i % 50 == 0) percent = 1.0D;
            if (i % 50 == 1) percent = 0.0D;
            int flat = rng.nextInt(20_000);
            boolean isElytra = rng.nextInt(5) == 0;

            DurabilityPenaltyPolicy.Outcome out = DurabilityPenaltyPolicy.evaluate(
                    currentDamage, maxDamage, mode, percent, flat, false, isElytra);

            assertNotEquals(DurabilityPenaltyPolicy.Action.HURT_AND_BREAK, out.action(),
                    "allowBreak=false must never HURT_AND_BREAK at iter=" + i);

            int finalDamage = out.action() == DurabilityPenaltyPolicy.Action.SET_DAMAGE
                    ? out.nextDamage()
                    : currentDamage;
            int remainingDurability = maxDamage - finalDamage;
            assertTrue(remainingDurability >= 1,
                    "remaining durability must be >= 1 when allowBreak=false at iter=" + i
                            + " (max=" + maxDamage + ", cur=" + currentDamage + ", final=" + finalDamage + ")");
            assertTrue(finalDamage >= currentDamage,
                    "damage must never decrease at iter=" + i);
            assertTrue(finalDamage <= maxDamage - 1,
                    "damage must never exceed maxDamage-1 at iter=" + i);

            // Also verify allowBreak=true invariants for the same parameters
            DurabilityPenaltyPolicy.Outcome breakable = DurabilityPenaltyPolicy.evaluate(
                    currentDamage, maxDamage, mode, percent, flat, true, isElytra);
            if (isElytra) {
                assertNotEquals(DurabilityPenaltyPolicy.Action.HURT_AND_BREAK, breakable.action(),
                        "Elytra must never HURT_AND_BREAK even when allowBreak=true at iter=" + i);
            } else if (breakable.action() == DurabilityPenaltyPolicy.Action.HURT_AND_BREAK) {
                assertEquals(maxDamage - 1, breakable.nextDamage(),
                        "HURT_AND_BREAK must stage nextDamage at maxDamage-1 at iter=" + i);
                assertTrue(breakable.breakDamageAmount() >= maxDamage,
                        "HURT_AND_BREAK breakDamageAmount must be >= maxDamage at iter=" + i);
            } else if (breakable.action() == DurabilityPenaltyPolicy.Action.SET_DAMAGE) {
                assertTrue(breakable.nextDamage() > currentDamage && breakable.nextDamage() < maxDamage,
                        "SET_DAMAGE out of range at iter=" + i);
            }
        }
    }

    @Test
    void hungerAntiSuicideFloorAndSaturationBoundsHold() {
        Random rng = new Random(0x5EED_0003_2026L);
        for (int i = 0; i < ITERATIONS; i++) {
            int beforeFood = rng.nextInt(31) - 5; // -5..25
            float beforeSat = (i % 77 == 0) ? Float.NaN : (rng.nextFloat() * 35.0f - 5.0f);
            int minFloor = rng.nextInt(31) - 5; // -5..25

            int clampedBefore = HungerPenaltyPolicy.clampFoodLevel(beforeFood);
            int clampedFloor = HungerPenaltyPolicy.clampFoodLevel(minFloor);

            HungerPenaltyPolicy.Result res = HungerPenaltyPolicy.computeRespawnHunger(beforeFood, beforeSat, minFloor);
            if (clampedBefore >= HungerPenaltyPolicy.MAX_FOOD_LEVEL) {
                assertFalse(res.apply(), "full hunger must not override respawn at iter=" + i);
            } else {
                assertTrue(res.apply(), "below-full hunger must apply preservation at iter=" + i);
                int expectedFood = Math.max(clampedFloor, clampedBefore);
                assertEquals(expectedFood, res.foodLevel(), "foodLevel mismatch at iter=" + i);
                assertTrue(res.foodLevel() >= clampedFloor && res.foodLevel() <= HungerPenaltyPolicy.MAX_FOOD_LEVEL,
                        "foodLevel out of [floor, 20] at iter=" + i);
                assertFalse(Float.isNaN(res.saturationLevel()), "saturation must never be NaN at iter=" + i);
                assertTrue(res.saturationLevel() >= 0.0f && res.saturationLevel() <= res.foodLevel(),
                        "saturation out of [0, foodLevel] at iter=" + i);
            }
        }
    }

    @Test
    void deathStreakAndProgressiveScheduleAreMonotonicAndBounded() {
        Random rng = new Random(0x5EED_0004_2026L);
        for (int i = 0; i < ITERATIONS; i++) {
            int scheduleLen = 1 + rng.nextInt(8);
            List<Integer> schedule = new ArrayList<>(scheduleLen);
            int currentSec = rng.nextInt(30);
            for (int j = 0; j < scheduleLen; j++) {
                currentSec = Math.min(DeathStreakPolicy.MAX_DURATION_SECONDS, currentSec + rng.nextInt(60));
                schedule.add(currentSec);
            }

            long resetTicks = 20L * (1 + rng.nextInt(1200));
            long gameTime = rng.nextInt(10_000);
            DeathStreakPolicy.State state = DeathStreakPolicy.State.empty();

            int prevDuration = -1;
            int prevDeaths = 0;
            for (int step = 0; step < 10; step++) {
                boolean forceReset = rng.nextInt(4) == 0;
                long delta = forceReset
                        ? resetTicks + rng.nextInt(1000)
                        : rng.nextInt((int) Math.min(Integer.MAX_VALUE, resetTicks));
                gameTime += delta;

                state = DeathStreakPolicy.advance(state, gameTime, resetTicks);
                assertTrue(state.deaths() >= 1, "streak deaths must be >= 1 at iter=" + i);
                assertEquals(gameTime, state.lastDeathGameTime(), "lastDeathGameTime must match gameTime");

                int duration = DeathStreakPolicy.durationSeconds(schedule, state.deaths());
                assertTrue(duration >= schedule.get(0) && duration <= schedule.get(scheduleLen - 1),
                        "duration out of schedule bounds at iter=" + i);

                if (!forceReset && prevDeaths > 0) {
                    assertEquals(prevDeaths + 1, state.deaths(),
                            "streak must increment within reset window at iter=" + i);
                    assertTrue(duration >= prevDuration,
                            "non-decreasing schedule must be monotonic across consecutive deaths at iter=" + i);
                } else {
                    assertEquals(1, state.deaths(),
                            "streak must reset to 1 on first death or after reset window at iter=" + i);
                }
                prevDeaths = state.deaths();
                prevDuration = duration;
            }
        }
    }
}
