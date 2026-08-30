package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AccessoryKeepContractTest {
    @Test
    void retentionIsOwnedAtEachInventoryDropBoundaryWithoutChangingTheWorldRule() throws Exception {
        String common = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/KeepButPenalty.java");
        String playerMixin = readSource("neoforge/src/main/java/dev/rinchan/keepbutpenalty/mixin/PlayerMixin.java");
        String curios = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/compat/CuriosCompat.java");
        String accessories = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/compat/AccessoriesCompat.java");
        String entrypoint = readSource("neoforge/src/main/java/dev/rinchan/keepbutpenalty/neoforge/KeepButPenaltyNeoForge.java");

        assertFalse(common.contains("INVENTORY_AFTER_DEATH"));
        assertFalse(common.contains("PERSISTED_INVENTORY_TAG"));

        assertTrue(playerMixin.contains("method = \"dropEquipment\""));
        assertTrue(playerMixin.contains("KeepButPenaltyConfig.KEEP_INVENTORY.get()"));
        assertTrue(playerMixin.contains("ci.cancel()"));

        assertTrue(curios.contains("DropRulesEvent"));
        assertTrue(curios.contains("DropRule.ALWAYS_KEEP"));
        assertTrue(curios.contains("EventPriority.LOWEST"));

        assertTrue(accessories.contains("OnDeathCallback"));
        assertTrue(accessories.contains("TriState.FALSE"));
        assertTrue(accessories.contains("OnDropCallback"));
        assertTrue(accessories.contains("DropRule.KEEP"));

        assertTrue(entrypoint.contains("CuriosCompat.registerDropRetention"));
        assertTrue(entrypoint.contains("AccessoriesCompat.registerDropRetention"));
        assertTrue(common.contains("CuriosCompat.damageEquipped"));
        assertTrue(common.contains("AccessoriesCompat.damageEquipped"));
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
