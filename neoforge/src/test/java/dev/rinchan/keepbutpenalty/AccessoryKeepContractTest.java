package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AccessoryKeepContractTest {
    @Test
    void modPublishesVanillaKeepInventoryWithoutOwningDropOrCloneSemantics() throws Exception {
        String common = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/KeepButPenalty.java");
        String platform = readSource("neoforge/src/main/java/dev/rinchan/keepbutpenalty/neoforge/KeepButPenaltyNeoForge.java");
        String curios = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/compat/CuriosCompat.java");
        String accessories = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/compat/AccessoriesCompat.java");

        assertTrue(common.contains("GameRules.RULE_KEEPINVENTORY"));
        assertTrue(common.contains(".set(true, server)"));
        assertTrue(platform.contains("ServerStartedEvent"));
        assertTrue(platform.contains("publishInventoryRetention"));
        assertFalse(common.contains("INVENTORY_AFTER_DEATH"));
        assertFalse(common.contains("PERSISTED_INVENTORY_TAG"));
        assertFalse(platform.contains("LivingDropsEvent"));
        assertFalse(platform.contains("PlayerEvent.Clone"));
        assertFalse(curios.contains("DropRulesEvent"));
        assertFalse(accessories.contains("OnDropCallback"));
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
