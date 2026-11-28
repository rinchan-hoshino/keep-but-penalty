package dev.rinchan.keepbutpenalty.config;

import dev.rinchan.keepbutpenalty.policy.DeathStreakPolicy;
import dev.rinchan.keepbutpenalty.policy.DurabilityPenaltyPolicy;
import dev.rinchan.keepbutpenalty.policy.ExperiencePenaltyPolicy;
import dev.rinchan.keepbutpenalty.policy.HungerPenaltyPolicy;
import dev.rinchan.keepbutpenalty.policy.RespawnDebuffSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Zero-dependency TOML configuration for Keep But Penalty 2.0.1.
 * Stored at config/keep_but_penalty.toml; automatically migrates legacy
 * config/keep_but_penalty-common.toml on first startup without data loss.
 */
public final class KeepButPenaltyConfig {
    public static final String FILE_NAME = "keep_but_penalty.toml";
    public static final String LEGACY_FILE_NAME = "keep_but_penalty-common.toml";

    public static final List<String> DEFAULT_EXCLUDED_ITEMS = List.of(
            "#keep_but_penalty:excluded",
            "#tconstruct:modifiable",
            "#tetra:modular_items",
            "#slashblade:swords"
    );

    private static volatile KeepButPenaltyConfig current = defaults();
    private static volatile Path configPath;

    // [death]
    private final boolean keepInventory;
    private final boolean enableExperiencePenalty;
    private final double experienceKeepRatio;
    private final boolean preserveHungerOnDeath;
    private final int minRespawnFoodLevel;
    private final boolean skipOnPvp;
    private final List<String> respawnDebuffs;
    private final List<Integer> respawnDebuffDurationsSecondsByDeath;
    private final int respawnDebuffStreakResetSeconds;
    private final boolean showRespawnSummary;

    // [durability]
    private final boolean enableDurabilityPenalty;
    private final DurabilityPenaltyMode penaltyMode;
    private final double durabilityPercent;
    private final int durabilityFlat;
    private final boolean allowBreak;
    private final boolean damageArmor;
    private final boolean damageMainHand;
    private final boolean damageOffHand;
    private final boolean damageHotbar;
    private final boolean damageInventory;
    private final boolean damageCurios;
    private final boolean damageTrinkets;
    private final boolean damageAccessories;
    private final List<String> excludedItems;
    private final ExclusionMatcher exclusionMatcher;

    public KeepButPenaltyConfig(
            boolean keepInventory,
            boolean enableExperiencePenalty,
            double experienceKeepRatio,
            boolean preserveHungerOnDeath,
            int minRespawnFoodLevel,
            boolean skipOnPvp,
            List<String> respawnDebuffs,
            List<Integer> respawnDebuffDurationsSecondsByDeath,
            int respawnDebuffStreakResetSeconds,
            boolean showRespawnSummary,
            boolean enableDurabilityPenalty,
            DurabilityPenaltyMode penaltyMode,
            double durabilityPercent,
            int durabilityFlat,
            boolean allowBreak,
            boolean damageArmor,
            boolean damageMainHand,
            boolean damageOffHand,
            boolean damageHotbar,
            boolean damageInventory,
            boolean damageCurios,
            boolean damageTrinkets,
            boolean damageAccessories,
            List<String> excludedItems
    ) {
        this.keepInventory = keepInventory;
        this.enableExperiencePenalty = enableExperiencePenalty;
        this.experienceKeepRatio = ExperiencePenaltyPolicy.clampRatio(experienceKeepRatio);
        this.preserveHungerOnDeath = preserveHungerOnDeath;
        this.minRespawnFoodLevel = HungerPenaltyPolicy.clampFoodLevel(minRespawnFoodLevel);
        this.skipOnPvp = skipOnPvp;
        this.respawnDebuffs = sanitizeDebuffs(respawnDebuffs);
        this.respawnDebuffDurationsSecondsByDeath = sanitizeDurations(respawnDebuffDurationsSecondsByDeath);
        this.respawnDebuffStreakResetSeconds = Math.max(1, Math.min(DeathStreakPolicy.MAX_DURATION_SECONDS, respawnDebuffStreakResetSeconds));
        this.showRespawnSummary = showRespawnSummary;

        this.enableDurabilityPenalty = enableDurabilityPenalty;
        this.penaltyMode = penaltyMode == null ? DurabilityPenaltyMode.PERCENT_MAX : penaltyMode;
        this.durabilityPercent = DurabilityPenaltyPolicy.clampPercent(durabilityPercent);
        this.durabilityFlat = Math.max(0, durabilityFlat);
        this.allowBreak = allowBreak;
        this.damageArmor = damageArmor;
        this.damageMainHand = damageMainHand;
        this.damageOffHand = damageOffHand;
        this.damageHotbar = damageHotbar;
        this.damageInventory = damageInventory;
        this.damageCurios = damageCurios;
        this.damageTrinkets = damageTrinkets;
        this.damageAccessories = damageAccessories;
        this.exclusionMatcher = ExclusionMatcher.compile(excludedItems);
        this.excludedItems = this.exclusionMatcher.normalizedEntries();
    }

    public static KeepButPenaltyConfig defaults() {
        return new KeepButPenaltyConfig(
                true,
                true,
                0.75D,
                true,
                6,
                false,
                List.of(),
                List.of(),
                600,
                true,
                true,
                DurabilityPenaltyMode.PERCENT_MAX,
                0.10D,
                80,
                false,
                true,
                true,
                true,
                false,
                false,
                true,
                true,
                true,
                DEFAULT_EXCLUDED_ITEMS
        );
    }

    public static KeepButPenaltyConfig get() {
        return current;
    }

    public static void setForTests(KeepButPenaltyConfig config) {
        current = config == null ? defaults() : config;
    }

    public static KeepButPenaltyConfig load(Path configDir) {
        Path primary = configDir.resolve(FILE_NAME);
        Path legacy = configDir.resolve(LEGACY_FILE_NAME);
        configPath = primary;

        KeepButPenaltyConfig loaded;
        if (Files.isRegularFile(primary)) {
            loaded = readFile(primary, false);
        } else if (Files.isRegularFile(legacy)) {
            loaded = readFile(legacy, true);
            writeFile(primary, loaded);
        } else {
            loaded = defaults();
            writeFile(primary, loaded);
        }

        current = loaded;
        return loaded;
    }

    public static void updateAndSave(KeepButPenaltyConfig updated) {
        KeepButPenaltyConfig clean = updated == null ? defaults() : updated;
        current = clean;
        Path path = configPath;
        if (path != null) {
            writeFile(path, clean);
        }
    }

    public static KeepButPenaltyConfig parse(String content, boolean migratingFromLegacy) {
        Map<String, Object> raw = SimpleToml.parse(content);
        KeepButPenaltyConfig d = defaults();

        boolean keepInv = readBool(raw, "death.keepInventory", "keepInventory", d.keepInventory);
        boolean enableXp = readBool(raw, "death.enableExperiencePenalty", "enableExperiencePenalty", d.enableExperiencePenalty);
        double xpRatio = readDouble(raw, "death.experienceKeepRatio", "experienceKeepRatio", d.experienceKeepRatio);
        boolean preserveHunger = readBool(raw, "death.preserveHungerOnDeath", "preserveHungerOnDeath", d.preserveHungerOnDeath);
        int minFood = readInt(raw, "death.minRespawnFoodLevel", "minRespawnFoodLevel", d.minRespawnFoodLevel);
        boolean skipPvp = readBool(raw, "death.skipOnPvp", "skipOnPvp", d.skipOnPvp);
        List<String> debuffs = readStringList(raw, "death.respawnDebuffs", "respawnDebuffs", d.respawnDebuffs);
        List<Integer> durations = readIntList(
                raw,
                "death.respawnDebuffDurationsSecondsByDeath",
                "respawnDebuffDurationsSecondsByDeath",
                d.respawnDebuffDurationsSecondsByDeath);
        int streakReset = readInt(
                raw,
                "death.respawnDebuffStreakResetSeconds",
                "respawnDebuffStreakResetSeconds",
                d.respawnDebuffStreakResetSeconds);
        boolean showSummary = readBool(raw, "death.showRespawnSummary", "showRespawnSummary", d.showRespawnSummary);

        boolean enableDurability = readBool(raw, "durability.enableDurabilityPenalty", "enableDurabilityPenalty", d.enableDurabilityPenalty);

        String rawMode = readString(raw, "durability.penaltyMode", "penaltyMode", null);
        DurabilityPenaltyMode mode;
        if (rawMode != null) {
            mode = DurabilityPenaltyMode.parse(rawMode, DurabilityPenaltyMode.PERCENT_MAX);
        } else if (migratingFromLegacy && enableDurability) {
            // Preserve legacy fixed-point durability behavior when migrating an active legacy config.
            mode = DurabilityPenaltyMode.FLAT;
        } else {
            mode = DurabilityPenaltyMode.PERCENT_MAX;
        }

        double durPercent = readDouble(raw, "durability.durabilityPercent", "durabilityPercent", d.durabilityPercent);
        int durFlat = readInt(raw, "durability.durabilityFlat", "durabilityFlat",
                readInt(raw, "durability.durabilityLoss", "durabilityLoss", d.durabilityFlat));

        boolean allowBreakVal = hasKey(raw, "durability.allowBreak", "allowBreak")
                ? readBool(raw, "durability.allowBreak", "allowBreak", d.allowBreak)
                : readBool(raw, "durability.allowZeroDurability", "allowZeroDurability", d.allowBreak);

        boolean dmgArmor = readBool(raw, "durability.damageArmor", "damageArmor", d.damageArmor);
        boolean dmgMain = readBool(raw, "durability.damageMainHand", "damageMainHand", d.damageMainHand);
        boolean dmgOff = readBool(raw, "durability.damageOffHand", "damageOffHand", d.damageOffHand);
        boolean dmgHotbar = readBool(raw, "durability.damageHotbar", "damageHotbar", d.damageHotbar);
        boolean dmgInventory = readBool(raw, "durability.damageInventory", "damageInventory", d.damageInventory);
        boolean dmgCurios = readBool(raw, "durability.damageCurios", "damageCurios", d.damageCurios);
        boolean dmgTrinkets = readBool(raw, "durability.damageTrinkets", "damageTrinkets", d.damageTrinkets);
        boolean dmgAccessories = readBool(raw, "durability.damageAccessories", "damageAccessories", d.damageAccessories);
        List<String> excluded = readStringList(raw, "durability.excludedItems", "excludedItems", d.excludedItems);

        return new KeepButPenaltyConfig(
                keepInv,
                enableXp,
                xpRatio,
                preserveHunger,
                minFood,
                skipPvp,
                debuffs,
                durations,
                streakReset,
                showSummary,
                enableDurability,
                mode,
                durPercent,
                durFlat,
                allowBreakVal,
                dmgArmor,
                dmgMain,
                dmgOff,
                dmgHotbar,
                dmgInventory,
                dmgCurios,
                dmgTrinkets,
                dmgAccessories,
                excluded
        );
    }

    public String toToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Keep But Penalty 2.0.1 Configuration\n");
        sb.append("# Changes saved in-game take effect immediately.\n\n");

        sb.append("[death]\n");
        sb.append("# Project keepInventory=true during player death/respawn without changing the world gamerule.\n");
        sb.append("keepInventory = ").append(keepInventory).append('\n');
        sb.append("# Apply experience penalty on death using exact vanilla level/progress math.\n");
        sb.append("enableExperiencePenalty = ").append(enableExperiencePenalty).append('\n');
        sb.append("# Fraction of total experience kept after death (0.0 to 1.0, default 0.75 = keep 75%, lose 25%).\n");
        sb.append("experienceKeepRatio = ").append(formatDouble(experienceKeepRatio)).append('\n');
        sb.append("# Preserve below-full hunger across death to prevent suicide hunger resets.\n");
        sb.append("preserveHungerOnDeath = ").append(preserveHungerOnDeath).append('\n');
        sb.append("# Minimum food level restored on respawn when preserveHungerOnDeath is enabled (0 to 20).\n");
        sb.append("minRespawnFoodLevel = ").append(minRespawnFoodLevel).append('\n');
        sb.append("# Skip penalties when killed by another player (inventory is still kept if keepInventory = true).\n");
        sb.append("skipOnPvp = ").append(skipOnPvp).append('\n');
        sb.append("# Respawn debuffs as \"effect_id,duration_seconds,level\" (e.g. \"minecraft:weakness,60,1\").\n");
        sb.append("respawnDebuffs = ").append(SimpleToml.formatStringArray(respawnDebuffs)).append('\n');
        sb.append("# Optional progressive debuff durations (seconds) by consecutive death; 0 skips that death.\n");
        sb.append("respawnDebuffDurationsSecondsByDeath = ").append(SimpleToml.formatIntArray(respawnDebuffDurationsSecondsByDeath)).append('\n');
        sb.append("# Seconds without dying before the consecutive-death streak resets (1 to 86400).\n");
        sb.append("respawnDebuffStreakResetSeconds = ").append(respawnDebuffStreakResetSeconds).append('\n');
        sb.append("# Show a concise Actionbar summary of death penalties after respawning.\n");
        sb.append("showRespawnSummary = ").append(showRespawnSummary).append("\n\n");

        sb.append("[durability]\n");
        sb.append("# Apply durability penalty to equipped items on death.\n");
        sb.append("enableDurabilityPenalty = ").append(enableDurabilityPenalty).append('\n');
        sb.append("# Durability loss mode: \"PERCENT_MAX\" (% of max durability) or \"FLAT\" (fixed points).\n");
        sb.append("penaltyMode = ").append(SimpleToml.escapeString(penaltyMode.name())).append('\n');
        sb.append("# Fraction of max durability removed in PERCENT_MAX mode (0.0 to 1.0, default 0.10 = 10%).\n");
        sb.append("durabilityPercent = ").append(formatDouble(durabilityPercent)).append('\n');
        sb.append("# Fixed durability points removed in FLAT mode.\n");
        sb.append("durabilityFlat = ").append(durabilityFlat).append('\n');
        sb.append("# Allow items reaching max damage to trigger vanilla hurtAndBreak (false clamps at 1 durability left).\n");
        sb.append("allowBreak = ").append(allowBreak).append('\n');
        sb.append("damageArmor = ").append(damageArmor).append('\n');
        sb.append("damageMainHand = ").append(damageMainHand).append('\n');
        sb.append("damageOffHand = ").append(damageOffHand).append('\n');
        sb.append("# Apply durability penalty to damageable items in hotbar slots (0-8).\n");
        sb.append("damageHotbar = ").append(damageHotbar).append('\n');
        sb.append("# Apply durability penalty to damageable items in main inventory slots (9-35, excluding hotbar).\n");
        sb.append("damageInventory = ").append(damageInventory).append('\n');
        sb.append("damageCurios = ").append(damageCurios).append('\n');
        sb.append("damageTrinkets = ").append(damageTrinkets).append('\n');
        sb.append("damageAccessories = ").append(damageAccessories).append('\n');
        sb.append("# Item IDs (\"minecraft:elytra\") or item tags (\"#keep_but_penalty:excluded\") exempt from durability loss.\n");
        sb.append("excludedItems = ").append(SimpleToml.formatStringArray(excludedItems)).append('\n');
        return sb.toString();
    }

    private static KeepButPenaltyConfig readFile(Path path, boolean migratingFromLegacy) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return parse(content, migratingFromLegacy);
        } catch (IOException e) {
            return defaults();
        }
    }

    private static void writeFile(Path path, KeepButPenaltyConfig cfg) {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(path, cfg.toToml(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static boolean hasKey(Map<String, Object> raw, String qualified, String fallback) {
        return raw.containsKey(qualified) || raw.containsKey(fallback);
    }

    private static Object getRaw(Map<String, Object> raw, String qualified, String fallback) {
        return raw.containsKey(qualified) ? raw.get(qualified) : raw.get(fallback);
    }

    private static boolean readBool(Map<String, Object> raw, String qualified, String fallback, boolean def) {
        Object val = getRaw(raw, qualified, fallback);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) {
            if ("true".equalsIgnoreCase(s)) return true;
            if ("false".equalsIgnoreCase(s)) return false;
        }
        return def;
    }

    private static int readInt(Map<String, Object> raw, String qualified, String fallback, int def) {
        Object val = getRaw(raw, qualified, fallback);
        if (val instanceof Number n) return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, n.longValue()));
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static double readDouble(Map<String, Object> raw, String qualified, String fallback, double def) {
        Object val = getRaw(raw, qualified, fallback);
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static String readString(Map<String, Object> raw, String qualified, String fallback, String def) {
        Object val = getRaw(raw, qualified, fallback);
        return val instanceof String s ? s : def;
    }

    private static List<String> readStringList(Map<String, Object> raw, String qualified, String fallback, List<String> def) {
        Object val = getRaw(raw, qualified, fallback);
        if (!(val instanceof List<?> list)) return def;
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                String text = item.toString().trim();
                if (!text.isEmpty()) out.add(text);
            }
        }
        return out;
    }

    private static List<Integer> readIntList(Map<String, Object> raw, String qualified, String fallback, List<Integer> def) {
        Object val = getRaw(raw, qualified, fallback);
        if (!(val instanceof List<?> list)) return def;
        List<Integer> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number n) {
                out.add(n.intValue());
            } else if (item instanceof String s) {
                try {
                    out.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        return out;
    }

    private static List<String> sanitizeDebuffs(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        List<String> valid = new ArrayList<>();
        for (String entry : raw) {
            if (RespawnDebuffSpec.isValid(entry)) {
                valid.add(entry.trim());
            }
        }
        return Collections.unmodifiableList(valid);
    }

    private static List<Integer> sanitizeDurations(List<Integer> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        List<Integer> valid = new ArrayList<>();
        for (Integer seconds : raw) {
            if (seconds != null && seconds >= 0 && seconds <= DeathStreakPolicy.MAX_DURATION_SECONDS) {
                valid.add(seconds);
            }
        }
        return Collections.unmodifiableList(valid);
    }

    private static String formatDouble(double value) {
        String formatted = String.format(Locale.ROOT, "%.4f", value);
        while (formatted.contains(".") && formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        if (formatted.endsWith(".")) {
            formatted = formatted + "0";
        }
        return formatted;
    }

    public boolean keepInventory() { return keepInventory; }
    public boolean enableExperiencePenalty() { return enableExperiencePenalty; }
    public double experienceKeepRatio() { return experienceKeepRatio; }
    public boolean preserveHungerOnDeath() { return preserveHungerOnDeath; }
    public int minRespawnFoodLevel() { return minRespawnFoodLevel; }
    public boolean skipOnPvp() { return skipOnPvp; }
    public List<String> respawnDebuffs() { return respawnDebuffs; }
    public List<Integer> respawnDebuffDurationsSecondsByDeath() { return respawnDebuffDurationsSecondsByDeath; }
    public int respawnDebuffStreakResetSeconds() { return respawnDebuffStreakResetSeconds; }
    public boolean showRespawnSummary() { return showRespawnSummary; }

    public boolean enableDurabilityPenalty() { return enableDurabilityPenalty; }
    public DurabilityPenaltyMode penaltyMode() { return penaltyMode; }
    public double durabilityPercent() { return durabilityPercent; }
    public int durabilityFlat() { return durabilityFlat; }
    public boolean allowBreak() { return allowBreak; }
    public boolean damageArmor() { return damageArmor; }
    public boolean damageMainHand() { return damageMainHand; }
    public boolean damageOffHand() { return damageOffHand; }
    public boolean damageHotbar() { return damageHotbar; }
    public boolean damageInventory() { return damageInventory; }
    public boolean damageCurios() { return damageCurios; }
    public boolean damageTrinkets() { return damageTrinkets; }
    public boolean damageAccessories() { return damageAccessories; }
    public List<String> excludedItems() { return excludedItems; }
    public ExclusionMatcher exclusionMatcher() { return exclusionMatcher; }

    public KeepButPenaltyConfig withScreenValues(
            boolean keepInventory,
            boolean skipOnPvp,
            boolean enableExperiencePenalty,
            double experienceKeepRatio,
            boolean enableDurabilityPenalty,
            DurabilityPenaltyMode penaltyMode,
            double durabilityPercent,
            int durabilityFlat,
            boolean allowBreak,
            boolean damageHotbar,
            boolean damageInventory,
            boolean preserveHungerOnDeath,
            int minRespawnFoodLevel,
            boolean showRespawnSummary
    ) {
        return new KeepButPenaltyConfig(
                keepInventory,
                enableExperiencePenalty,
                experienceKeepRatio,
                preserveHungerOnDeath,
                minRespawnFoodLevel,
                skipOnPvp,
                this.respawnDebuffs,
                this.respawnDebuffDurationsSecondsByDeath,
                this.respawnDebuffStreakResetSeconds,
                showRespawnSummary,
                enableDurabilityPenalty,
                penaltyMode,
                durabilityPercent,
                durabilityFlat,
                allowBreak,
                this.damageArmor,
                this.damageMainHand,
                this.damageOffHand,
                damageHotbar,
                damageInventory,
                this.damageCurios,
                this.damageTrinkets,
                this.damageAccessories,
                this.excludedItems
        );
    }
}
