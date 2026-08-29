package dev.rinchan.keepbutpenalty;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StablePolicyContractTest {
    @Test
    void defaultsKeepItemsWithoutEnablingAnyPenalty() throws Exception {
        String config = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/KeepButPenaltyConfig.java");
        assertTrue(config.contains("define(\"keepInventory\", true)"));
        assertTrue(config.contains("define(\"enableExperiencePenalty\", false)"));
        assertTrue(config.contains("defineListAllowEmpty(\"respawnDebuffs\", java.util.List.of()"));
        assertTrue(config.contains("define(\"enableDurabilityPenalty\", false)"));
        assertFalse(config.contains("respawnWeaknessLevel"));
        assertFalse(config.contains("respawnMiningFatigueLevel"));
    }

    @Test
    void inventoryRetentionIsADeathScopedProjectionNotAWorldWriteOrInventoryCopy() throws Exception {
        String common = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/KeepButPenalty.java");
        String platform = readSource("neoforge/src/main/java/dev/rinchan/keepbutpenalty/neoforge/KeepButPenaltyNeoForge.java");
        String serverMixin = readSource("neoforge/src/main/java/dev/rinchan/keepbutpenalty/mixin/ServerPlayerMixin.java");
        String build = readSource("neoforge/build.gradle");

        assertFalse(common.contains(".set(true, server)"));
        assertFalse(platform.contains("ServerStartedEvent"));
        assertFalse(platform.contains("PlayerEvent.Clone"));
        assertFalse(platform.contains("LivingDropsEvent"));
        assertTrue(serverMixin.contains("ReentrantFlag.Scope"));
        assertTrue(serverMixin.contains("method = \"die\""));
        assertTrue(serverMixin.contains("method = \"restoreFrom\""));
        assertTrue(serverMixin.contains("dev.rinchan.rinlib.minecraft.KeepInventoryProjection"));
        assertTrue(build.contains("rinlib-neoforge"));
    }

    @Test
    void progressiveDebuffsCountOnlyUncancelledFinalDeathsAndSurviveRespawn() throws Exception {
        String config = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/KeepButPenaltyConfig.java");
        String common = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/KeepButPenalty.java");
        String platform = readSource("neoforge/src/main/java/dev/rinchan/keepbutpenalty/neoforge/KeepButPenaltyNeoForge.java");
        String serverMixin = readSource("neoforge/src/main/java/dev/rinchan/keepbutpenalty/mixin/ServerPlayerMixin.java");
        String persistenceMixin = readSource("support/compat/1.21.1/java/dev/rinchan/keepbutpenalty/mixin/DeathStreakPersistenceMixin.java");

        assertTrue(config.contains("respawnDebuffDurationsSecondsByDeath"));
        assertTrue(config.contains("respawnDebuffStreakResetSeconds"));
        assertTrue(common.contains("DeathStreakPolicy.durationSeconds"));
        assertTrue(platform.contains("EventPriority.LOWEST"));
        assertTrue(platform.contains("recordFinalDeath(player)"));
        assertTrue(persistenceMixin.contains("addAdditionalSaveData"));
        assertTrue(persistenceMixin.contains("readAdditionalSaveData"));
        assertTrue(serverMixin.contains("previous).keepButPenalty$getDeathStreak()"));
    }

    private static String readSource(String relative) throws Exception {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to find " + relative);
    }
}
