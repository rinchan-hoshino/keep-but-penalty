package dev.rinchan.keepbutpenalty.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class KeepButPenaltyConfigTest {
    @Test
    void defaultsMatchV2DesignSpec() {
        KeepButPenaltyConfig cfg = KeepButPenaltyConfig.defaults();
        assertTrue(cfg.keepInventory());
        assertTrue(cfg.enableExperiencePenalty());
        assertEquals(0.75D, cfg.experienceKeepRatio(), 1e-6);
        assertTrue(cfg.preserveHungerOnDeath());
        assertEquals(6, cfg.minRespawnFoodLevel());
        assertFalse(cfg.skipOnPvp());
        assertTrue(cfg.showRespawnSummary());

        assertTrue(cfg.enableDurabilityPenalty());
        assertEquals(DurabilityPenaltyMode.PERCENT_MAX, cfg.penaltyMode());
        assertEquals(0.10D, cfg.durabilityPercent(), 1e-6);
        assertEquals(80, cfg.durabilityFlat());
        assertFalse(cfg.allowBreak());
        assertTrue(cfg.damageArmor());
        assertTrue(cfg.damageMainHand());
        assertTrue(cfg.damageOffHand());
        assertTrue(cfg.damageInventory());
        assertTrue(cfg.damageCurios());
        assertTrue(cfg.damageTrinkets());
        assertTrue(cfg.damageAccessories());
        assertTrue(cfg.excludedItems().contains("#keep_but_penalty:excluded"));
        assertTrue(cfg.excludedItems().contains("#tconstruct:modifiable"));
        assertTrue(cfg.excludedItems().contains("#tetra:modular_items"));
        assertTrue(cfg.excludedItems().contains("#slashblade:swords"));
    }

    @Test
    void tomlRoundTripPreservesAllFields() {
        KeepButPenaltyConfig original = new KeepButPenaltyConfig(
                true, true, 0.60D, true, 8, true,
                List.of("minecraft:weakness,45,2"),
                List.of(0, 10, 20),
                900,
                false,
                true,
                DurabilityPenaltyMode.FLAT,
                0.15D,
                120,
                false,
                true, false, true, false, false, true, false,
                List.of("#keep_but_penalty:excluded", "minecraft:netherite_sword")
        );
        KeepButPenaltyConfig parsed = KeepButPenaltyConfig.parse(original.toToml(), false);
        assertEquals(original.keepInventory(), parsed.keepInventory());
        assertEquals(original.experienceKeepRatio(), parsed.experienceKeepRatio(), 1e-4);
        assertEquals(original.minRespawnFoodLevel(), parsed.minRespawnFoodLevel());
        assertEquals(original.skipOnPvp(), parsed.skipOnPvp());
        assertEquals(original.respawnDebuffs(), parsed.respawnDebuffs());
        assertEquals(original.respawnDebuffDurationsSecondsByDeath(), parsed.respawnDebuffDurationsSecondsByDeath());
        assertEquals(original.respawnDebuffStreakResetSeconds(), parsed.respawnDebuffStreakResetSeconds());
        assertEquals(original.showRespawnSummary(), parsed.showRespawnSummary());
        assertEquals(original.penaltyMode(), parsed.penaltyMode());
        assertEquals(original.durabilityFlat(), parsed.durabilityFlat());
        assertEquals(original.allowBreak(), parsed.allowBreak());
        assertEquals(original.damageMainHand(), parsed.damageMainHand());
        assertEquals(original.damageInventory(), parsed.damageInventory());
        assertEquals(original.excludedItems(), parsed.excludedItems());
    }

    @Test
    void existingTomlWithoutDamageInventoryDefaultsToTrue() {
        String existing = """
                [durability]
                enableDurabilityPenalty = true
                damageArmor = true
                damageMainHand = true
                damageOffHand = true
                """;
        KeepButPenaltyConfig parsed = KeepButPenaltyConfig.parse(existing, false);
        assertTrue(parsed.damageInventory());
    }

    @Test
    void migratesLegacyCommonTomlWithActiveDurabilityAsFlat(@TempDir Path tempDir) throws Exception {
        String legacy = """
                [death]
                keepInventory = true
                enableExperiencePenalty = true
                experienceKeepRatio = 0.5
                respawnDebuffs = ["minecraft:slowness,30,1"]
                respawnDebuffDurationsSecondsByDeath = [0, 15, 30]
                respawnDebuffStreakResetSeconds = 300

                [durability]
                enableDurabilityPenalty = true
                durabilityLoss = 125
                allowZeroDurability = false
                damageArmor = true
                damageMainHand = false
                damageOffHand = true
                damageCurios = false
                damageAccessories = true
                """;
        Files.writeString(tempDir.resolve(KeepButPenaltyConfig.LEGACY_FILE_NAME), legacy, StandardCharsets.UTF_8);

        KeepButPenaltyConfig migrated = KeepButPenaltyConfig.load(tempDir);
        assertTrue(Files.isRegularFile(tempDir.resolve(KeepButPenaltyConfig.FILE_NAME)));
        assertTrue(migrated.enableExperiencePenalty());
        assertEquals(0.5D, migrated.experienceKeepRatio(), 1e-6);
        assertEquals(List.of("minecraft:slowness,30,1"), migrated.respawnDebuffs());
        assertEquals(List.of(0, 15, 30), migrated.respawnDebuffDurationsSecondsByDeath());
        assertEquals(300, migrated.respawnDebuffStreakResetSeconds());

        assertTrue(migrated.enableDurabilityPenalty());
        assertEquals(DurabilityPenaltyMode.FLAT, migrated.penaltyMode());
        assertEquals(125, migrated.durabilityFlat());
        assertFalse(migrated.allowBreak());
        assertFalse(migrated.damageMainHand());
        assertFalse(migrated.damageCurios());
        assertTrue(migrated.preserveHungerOnDeath());
        assertEquals(6, migrated.minRespawnFoodLevel());
    }

    @Test
    void migratesLegacyCommonTomlWithDisabledDurabilityAsPercentMax(@TempDir Path tempDir) throws Exception {
        String legacy = """
                [durability]
                enableDurabilityPenalty = false
                durabilityLoss = 80
                """;
        Files.writeString(tempDir.resolve(KeepButPenaltyConfig.LEGACY_FILE_NAME), legacy, StandardCharsets.UTF_8);

        KeepButPenaltyConfig migrated = KeepButPenaltyConfig.load(tempDir);
        assertFalse(migrated.enableDurabilityPenalty());
        assertEquals(DurabilityPenaltyMode.PERCENT_MAX, migrated.penaltyMode());
        assertEquals(80, migrated.durabilityFlat());
    }

    @Test
    void exclusionMatcherSupportsItemIdsAndTags() {
        ExclusionMatcher matcher = ExclusionMatcher.compile(List.of(
                "#keep_but_penalty:excluded",
                "minecraft:diamond_sword",
                "INVALID::ID"
        ));
        assertTrue(matcher.matches("minecraft:diamond_sword", tag -> false));
        assertTrue(matcher.matches("minecraft:iron_sword", tag -> "keep_but_penalty:excluded".equals(tag)));
        assertFalse(matcher.matches("minecraft:iron_sword", tag -> false));
        assertEquals(2, matcher.normalizedEntries().size());
    }
}
