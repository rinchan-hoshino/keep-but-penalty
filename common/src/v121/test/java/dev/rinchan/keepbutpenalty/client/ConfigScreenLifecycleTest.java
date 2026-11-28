package dev.rinchan.keepbutpenalty.client;

import dev.rinchan.keepbutpenalty.config.DurabilityPenaltyMode;
import dev.rinchan.keepbutpenalty.config.KeepButPenaltyConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless black-box lifecycle verification for KeepButPenaltyConfigScreen.
 * Verifies construction, init() in both PERCENT_MAX and FLAT modes, widget creation,
 * and onClose() with zero exceptions.
 */
class ConfigScreenLifecycleTest {
    @AfterEach
    void restoreDefaults() {
        KeepButPenaltyConfig.setForTests(KeepButPenaltyConfig.defaults());
    }

    @Test
    void initializesWidgetsInPercentMaxModeWithoutExceptions() {
        KeepButPenaltyConfig.setForTests(KeepButPenaltyConfig.defaults());
        KeepButPenaltyConfigScreen screen = new KeepButPenaltyConfigScreen(null);
        screen.width = 854;
        screen.height = 480;

        assertDoesNotThrow(() -> screen.init());
        assertEquals(15, screen.children().size(),
                "Expected 12 config controls + 3 action buttons (Reset/Cancel/Save)");
        assertDoesNotThrow(() -> screen.onClose());
    }

    @Test
    void initializesWidgetsInFlatModeWithoutExceptions() {
        KeepButPenaltyConfig flatCfg = KeepButPenaltyConfig.defaults().withScreenValues(
                true, false, true, 0.75D, true,
                DurabilityPenaltyMode.FLAT, 0.10D, 80, false, true,
                true, 6, true
        );
        KeepButPenaltyConfig.setForTests(flatCfg);

        KeepButPenaltyConfigScreen screen = new KeepButPenaltyConfigScreen(null);
        screen.width = 640;
        screen.height = 360;

        assertDoesNotThrow(() -> screen.init());
        assertEquals(15, screen.children().size(),
                "Expected 12 config controls + 3 action buttons in FLAT mode");
        assertDoesNotThrow(() -> screen.onClose());
    }
}
