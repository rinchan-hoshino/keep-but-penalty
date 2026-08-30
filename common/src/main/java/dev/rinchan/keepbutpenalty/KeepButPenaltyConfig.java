package dev.rinchan.keepbutpenalty;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class KeepButPenaltyConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue KEEP_INVENTORY;
    public static final ModConfigSpec.BooleanValue ENABLE_EXPERIENCE_PENALTY;
    public static final ModConfigSpec.DoubleValue EXPERIENCE_KEEP_RATIO;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> RESPAWN_DEBUFFS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> RESPAWN_DEBUFF_DURATIONS_SECONDS_BY_DEATH;
    public static final ModConfigSpec.IntValue RESPAWN_DEBUFF_STREAK_RESET_SECONDS;

    public static final ModConfigSpec.BooleanValue ENABLE_DURABILITY_PENALTY;
    public static final ModConfigSpec.IntValue DURABILITY_LOSS;
    public static final ModConfigSpec.BooleanValue ALLOW_ZERO_DURABILITY;
    public static final ModConfigSpec.BooleanValue DAMAGE_ARMOR;
    public static final ModConfigSpec.BooleanValue DAMAGE_MAIN_HAND;
    public static final ModConfigSpec.BooleanValue DAMAGE_OFF_HAND;
    public static final ModConfigSpec.BooleanValue DAMAGE_CURIOS;
    public static final ModConfigSpec.BooleanValue DAMAGE_ACCESSORIES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("death");
        KEEP_INVENTORY = builder
                .comment("Retain vanilla inventory and supported accessory slots at their death-drop boundaries. The world gamerule is never changed.")
                .define("keepInventory", true);
        ENABLE_EXPERIENCE_PENALTY = builder
                .comment("Reduce retained experience on actual death.")
                .define("enableExperiencePenalty", false);
        EXPERIENCE_KEEP_RATIO = builder
                .comment("Fraction of total experience retained after death.")
                .defineInRange("experienceKeepRatio", 0.333333333D, 0.0D, 1.0D);
        RESPAWN_DEBUFFS = builder
                .comment("Respawn debuffs as effect_id,duration_seconds,level. Empty means no respawn debuffs. The entry duration is used when the progressive schedule below is empty.")
                .defineListAllowEmpty("respawnDebuffs", java.util.List.of(), () -> "minecraft:weakness,60,1", RespawnDebuffSpec::isValid);
        RESPAWN_DEBUFF_DURATIONS_SECONDS_BY_DEATH = builder
                .comment("Optional duration schedule by consecutive final death: first entry for first death, second for second death, and so on. Zero skips all configured debuffs for that death. Deaths beyond the list use its last duration. Empty preserves each respawnDebuffs entry duration.")
                .defineListAllowEmpty("respawnDebuffDurationsSecondsByDeath", java.util.List.of(), () -> 0, DeathStreakPolicy::isValidDuration);
        RESPAWN_DEBUFF_STREAK_RESET_SECONDS = builder
                .comment("Seconds without a final death before the consecutive-death schedule starts over.")
                .defineInRange("respawnDebuffStreakResetSeconds", 600, 1, 86_400);
        builder.pop();

        builder.push("durability");
        ENABLE_DURABILITY_PENALTY = builder
                .comment("Damage configured equipment slots on actual death.")
                .define("enableDurabilityPenalty", false);
        DURABILITY_LOSS = builder
                .comment("Durability removed from each affected damageable stack.")
                .defineInRange("durabilityLoss", 80, 0, Integer.MAX_VALUE);
        ALLOW_ZERO_DURABILITY = builder
                .comment("Allow damage to reach max damage instead of leaving one durability.")
                .define("allowZeroDurability", true);
        DAMAGE_ARMOR = builder.define("damageArmor", true);
        DAMAGE_MAIN_HAND = builder.define("damageMainHand", true);
        DAMAGE_OFF_HAND = builder.define("damageOffHand", true);
        DAMAGE_CURIOS = builder.define("damageCurios", true);
        DAMAGE_ACCESSORIES = builder.define("damageAccessories", true);
        builder.pop();

        SPEC = builder.build();
    }

    private KeepButPenaltyConfig() {
    }
}
