package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AccessoryKeepContractTest {
    @Test
    void optionalAccessoryAdaptersOnlyOwnDurabilityDamage() throws Exception {
        String common = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/KeepButPenalty.java");
        String curios = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/compat/CuriosCompat.java");
        String accessories = readSource("common/src/main/java/dev/rinchan/keepbutpenalty/compat/AccessoriesCompat.java");

        assertFalse(common.contains("INVENTORY_AFTER_DEATH"));
        assertFalse(common.contains("PERSISTED_INVENTORY_TAG"));
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
