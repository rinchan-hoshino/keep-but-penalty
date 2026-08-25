package dev.rinchan.keepbutpenalty;

import java.util.regex.Pattern;

public record RespawnDebuffSpec(String effectId, int durationTicks, int amplifier) {
    private static final Pattern EFFECT_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private static final int MAX_DURATION_SECONDS = 86_400;
    private static final int MAX_LEVEL = 256;

    public static RespawnDebuffSpec parse(String value) {
        String[] fields = value.split(",", -1);
        if (fields.length != 3) {
            throw invalid(value);
        }
        String effectId = fields[0].trim();
        int durationSeconds = parseBounded(fields[1], 1, MAX_DURATION_SECONDS, value);
        int level = parseBounded(fields[2], 1, MAX_LEVEL, value);
        if (!EFFECT_ID.matcher(effectId).matches()) {
            throw invalid(value);
        }
        return new RespawnDebuffSpec(effectId, durationSeconds * 20, level - 1);
    }

    public static boolean isValid(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        try {
            parse(text);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static int parseBounded(String field, int minimum, int maximum, String original) {
        try {
            int value = Integer.parseInt(field.trim());
            if (value >= minimum && value <= maximum) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        throw invalid(original);
    }

    private static IllegalArgumentException invalid(String value) {
        return new IllegalArgumentException("Invalid respawn debuff '" + value + "'; expected effect_id,duration_seconds,level");
    }
}
