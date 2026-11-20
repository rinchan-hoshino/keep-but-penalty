package dev.rinchan.keepbutpenalty.config;

import java.util.Locale;

public enum DurabilityPenaltyMode {
    PERCENT_MAX,
    FLAT;

    public static DurabilityPenaltyMode parse(String value, DurabilityPenaltyMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (DurabilityPenaltyMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return fallback;
    }
}
