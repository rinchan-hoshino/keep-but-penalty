package dev.rinchan.keepbutpenalty;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class KeepButPenaltyConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue keepInventory;
    public static final ModConfigSpec.BooleanValue enableExperiencePenalty;
    public static final ModConfigSpec.DoubleValue experienceKeepRatio;
    public static final ModConfigSpec.BooleanValue enableRespawnDebuffs;
    public static final ModConfigSpec.IntValue respawnDebuffDurationSeconds;
    public static final ModConfigSpec.BooleanValue enableDurabilityPenalty;
    public static final ModConfigSpec.IntValue durabilityLoss;
    public static final ModConfigSpec.BooleanValue allowZeroDurability;
    public static final ModConfigSpec.BooleanValue damageArmor;
    public static final ModConfigSpec.BooleanValue damageMainHand;
    public static final ModConfigSpec.BooleanValue damageOffHand;
    public static final ModConfigSpec.BooleanValue damageCurios;
    public static final ModConfigSpec.BooleanValue damageAccessories;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("death");
        keepInventory = builder
            .comment("Keep player inventory on death while this mod is installed. This does not change the vanilla keepInventory gamerule.")
            .define("keepInventory", true);
        enableExperiencePenalty = builder
            .comment("Reduce player experience after death.")
            .define("enableExperiencePenalty", true);
        experienceKeepRatio = builder
            .comment("Fraction of total vanilla experience kept after death. Default 0.333333333 keeps one third and removes two thirds.")
            .defineInRange("experienceKeepRatio", 0.333333333D, 0D, 1D);
        enableRespawnDebuffs = builder
            .comment("Apply vanilla Weakness I and Mining Fatigue I after a death respawn. Returning from the End does not trigger this penalty.")
            .define("enableRespawnDebuffs", true);
        respawnDebuffDurationSeconds = builder
            .comment("Shared duration in seconds of the vanilla respawn debuffs.")
            .defineInRange("respawnDebuffDurationSeconds", 60, 1, 86400);
        builder.pop();

        builder.push("durability");
        enableDurabilityPenalty = builder
            .comment("Damage equipped items after death.")
            .define("enableDurabilityPenalty", true);
        durabilityLoss = builder
            .comment("Flat durability damage applied to each selected damageable item.")
            .defineInRange("durabilityLoss", 80, 0, Integer.MAX_VALUE);
        allowZeroDurability = builder
            .comment("Allow death damage to push items all the way to max damage. Pair with a no-break mod such as Keep My Sword if you want 0-durability items to remain usable as repair targets.")
            .define("allowZeroDurability", true);
        damageArmor = builder
            .comment("Damage armor slots.")
            .define("damageArmor", true);
        damageMainHand = builder
            .comment("Damage the selected main-hand item.")
            .define("damageMainHand", true);
        damageOffHand = builder
            .comment("Damage the off-hand item.")
            .define("damageOffHand", true);
        damageCurios = builder
            .comment("Damage equipped Curios items when Curios is installed.")
            .define("damageCurios", true);
        damageAccessories = builder
            .comment("Damage equipped Accessories items when Accessories is installed.")
            .define("damageAccessories", true);
        builder.pop();

        SPEC = builder.build();
    }

    private KeepButPenaltyConfig() {
    }
}
