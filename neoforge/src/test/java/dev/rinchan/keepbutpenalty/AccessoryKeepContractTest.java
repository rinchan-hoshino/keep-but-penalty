package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AccessoryKeepContractTest {
    @Test
    void keepInventoryUsesEachAccessorySystemsNativeKeepRule() throws IOException {
        String curios = readSource(
            "common/src/main/java/dev/rinchan/keepbutpenalty/compat/CuriosCompat.java"
        );
        assertTrue(curios.contains("event.addOverride(stack -> true, ICurio.DropRule.ALWAYS_KEEP)"));

        String accessories = readSource(
            "common/src/main/java/dev/rinchan/keepbutpenalty/compat/AccessoriesCompat.java"
        );
        assertTrue(accessories.contains("OnDropCallback.EVENT.register"));
        assertTrue(accessories.contains("return DropRule.KEEP"));

        String lifecycle = readSource(
            "common/src/main/java/dev/rinchan/keepbutpenalty/KeepButPenalty.java"
        );
        assertFalse(lifecycle.contains("ACCESSORIES_AFTER_DEATH"));
        assertTrue(lifecycle.contains("PERSISTED_INVENTORY_TAG"));
        assertTrue(lifecycle.contains("persistInventory(player)"));
        assertTrue(lifecycle.contains("restorePersistedInventory(newPlayer)"));
    }

    private static String readSource(String relative) throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate " + relative);
    }
}
