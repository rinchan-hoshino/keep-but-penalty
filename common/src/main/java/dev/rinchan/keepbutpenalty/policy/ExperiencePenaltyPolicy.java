package dev.rinchan.keepbutpenalty.policy;

/**
 * Pure vanilla experience math with exact forward and inverse conversion.
 * Allows writing experienceLevel, experienceProgress, and totalExperience directly
 * without calling giveExperiencePoints (avoiding XP pickup events and Mending side effects).
 */
public final class ExperiencePenaltyPolicy {
    private ExperiencePenaltyPolicy() {}

    public record Result(int level, float progress, int totalExperience, int lostExperience) {}

    public static int totalExperience(int level, float progress) {
        int clampedLevel = Math.max(0, level);
        float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        int bar = xpToNextLevel(clampedLevel);
        int partial = Math.min(bar - 1, (int) Math.round((double) clampedProgress * bar));
        if (partial < 0) partial = 0;
        long total = xpForLevelLong(clampedLevel) + partial;
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static Result applyRetention(int currentLevel, float currentProgress, double keepRatio) {
        double clampedRatio = clampRatio(keepRatio);
        int before = totalExperience(currentLevel, currentProgress);
        if (before <= 0) {
            return new Result(0, 0.0f, 0, 0);
        }
        int remaining = (int) Math.floor(before * clampedRatio);
        if (remaining < 0) remaining = 0;
        if (remaining > before) remaining = before;
        Result resolved = fromTotalExperience(remaining);
        return new Result(resolved.level(), resolved.progress(), resolved.totalExperience(), before - resolved.totalExperience());
    }

    public static Result fromTotalExperience(int totalXp) {
        int clamped = Math.max(0, totalXp);
        if (clamped == 0) {
            return new Result(0, 0.0f, 0, 0);
        }

        int level = estimateLevel(clamped);
        while (level > 0 && xpForLevelLong(level) > clamped) {
            level--;
        }
        while (xpForLevelLong(level + 1) <= clamped) {
            level++;
        }

        long base = xpForLevelLong(level);
        int remainder = (int) (clamped - base);
        int bar = xpToNextLevel(level);
        float progress = bar > 0 ? Math.min(1.0f - Math.ulp(1.0f), (float) remainder / (float) bar) : 0.0f;
        return new Result(level, progress, clamped, 0);
    }

    public static int xpForLevel(int level) {
        return (int) Math.min(Integer.MAX_VALUE, xpForLevelLong(Math.max(0, level)));
    }

    public static long xpForLevelLong(int level) {
        long l = Math.max(0, level);
        if (l <= 16) {
            return l * l + 6L * l;
        }
        if (l <= 31) {
            return (5L * l * l - 81L * l) / 2L + 360L;
        }
        return (9L * l * l - 325L * l) / 2L + 2220L;
    }

    public static int xpToNextLevel(int level) {
        int l = Math.max(0, level);
        if (l >= 30) {
            return 112 + (l - 30) * 9;
        }
        if (l >= 15) {
            return 37 + (l - 15) * 5;
        }
        return 7 + l * 2;
    }

    public static double clampRatio(double ratio) {
        if (Double.isNaN(ratio)) return 0.75D;
        return Math.max(0.0D, Math.min(1.0D, ratio));
    }

    private static int estimateLevel(int totalXp) {
        if (totalXp < 352) {
            // L^2 + 6L - xp = 0 => L = -3 + sqrt(9 + xp)
            return Math.max(0, (int) Math.floor(-3.0D + Math.sqrt(9.0D + totalXp)));
        }
        if (totalXp < 1507) {
            // 2.5L^2 - 40.5L + (360 - xp) = 0 => L = (40.5 + sqrt(1640.25 - 10*(360 - xp))) / 5
            return Math.max(16, (int) Math.floor((40.5D + Math.sqrt(1640.25D - 10.0D * (360.0D - totalXp))) / 5.0D));
        }
        // 4.5L^2 - 162.5L + (2220 - xp) = 0 => L = (162.5 + sqrt(26406.25 - 18*(2220 - xp))) / 9
        return Math.max(31, (int) Math.floor((162.5D + Math.sqrt(26406.25D - 18.0D * (2220.0D - totalXp))) / 9.0D));
    }
}
