package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class PublishedVersionContractTest {
    @Test
    void releaseUsesStandardPublicVersionWithoutPrivateSuffix() throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(findProjectFile("gradle.properties"))) {
            properties.load(reader);
        }
        String version = properties.getProperty("mod_version");
        assertEquals("1.1.0+1.21.1", version);
        assertFalse(version.contains("private"));

        String profiles = Files.readString(findProjectFile("support/mainstream-profiles.json"));
        assertEquals(3, profiles.split("\"minecraft_version\"", -1).length - 1);
        for (String target : new String[] {"1.21.1", "26.1.2", "26.2"}) {
            org.junit.jupiter.api.Assertions.assertTrue(profiles.contains("\"" + target + "\""), target);
        }
        for (String removed : new String[] {"1.21.4", "1.21.5", "1.21.8", "1.21.11"}) {
            assertFalse(profiles.contains("\"" + removed + "\":"), removed);
        }
    }

    private static Path findProjectFile(String relative) throws Exception {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate " + relative);
    }
}
