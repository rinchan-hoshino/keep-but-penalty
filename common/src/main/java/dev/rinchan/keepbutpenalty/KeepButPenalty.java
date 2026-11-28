package dev.rinchan.keepbutpenalty;

import dev.rinchan.keepbutpenalty.compat.AccessoryCompat;
import dev.rinchan.keepbutpenalty.config.DurabilityPenaltyMode;
import dev.rinchan.keepbutpenalty.config.KeepButPenaltyConfig;
import dev.rinchan.keepbutpenalty.policy.DeathStreakPolicy;
import dev.rinchan.keepbutpenalty.policy.DurabilityPenaltyPolicy;
import dev.rinchan.keepbutpenalty.policy.ExperiencePenaltyPolicy;
import dev.rinchan.keepbutpenalty.policy.HungerPenaltyPolicy;
import dev.rinchan.keepbutpenalty.policy.RespawnDebuffSpec;
import dev.rinchan.keepbutpenalty.state.PendingRespawnState;
import dev.rinchan.keepbutpenalty.state.PlayerPenaltyAccess;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class KeepButPenalty {
    public static final String MOD_ID = "keep_but_penalty";
    private static final Set<String> UNKNOWN_DEBUFFS = ConcurrentHashMap.newKeySet();

    private KeepButPenalty() {}

    public static void init(Path configDir) {
        KeepButPenaltyConfig.load(configDir);
    }

    public static void onPlayerDied(ServerPlayer player, DamageSource source) {
        KeepButPenaltyConfig config = KeepButPenaltyConfig.get();
        PlayerPenaltyAccess access = (PlayerPenaltyAccess) player;

        if (config.skipOnPvp() && isPvpDeath(player, source)) {
            access.keepButPenalty$setPendingRespawn(PendingRespawnState.empty());
            return;
        }

        // 1. Record consecutive death streak
        long resetTicks = config.respawnDebuffStreakResetSeconds() * 20L;
        access.keepButPenalty$setDeathStreak(DeathStreakPolicy.advance(
                access.keepButPenalty$getDeathStreak(),
                player.level().getGameTime(),
                resetTicks
        ));

        // 2. Snapshot pre-death hunger before respawn resets it
        boolean captureHunger = config.preserveHungerOnDeath();
        int beforeFood = captureHunger ? player.getFoodData().getFoodLevel() : HungerPenaltyPolicy.MAX_FOOD_LEVEL;
        float beforeSat = captureHunger ? player.getFoodData().getSaturationLevel() : 5.0f;

        // 3. Apply experience penalty directly via exact math (never calls giveExperiencePoints)
        int xpLost = 0;
        int xpLostPercent = 0;
        if (config.enableExperiencePenalty()) {
            ExperiencePenaltyPolicy.Result xpResult = ExperiencePenaltyPolicy.applyRetention(
                    player.experienceLevel,
                    player.experienceProgress,
                    config.experienceKeepRatio()
            );
            player.experienceLevel = xpResult.level();
            player.experienceProgress = xpResult.progress();
            player.totalExperience = xpResult.totalExperience();
            xpLost = xpResult.lostExperience();
            xpLostPercent = (int) Math.round((1.0D - config.experienceKeepRatio()) * 100.0D);
        }

        // 4. Apply durability penalty with IdentityHashMap deduplication
        int[] durabilityCounts = new int[2]; // [damaged, broken]
        if (config.enableDurabilityPenalty()) {
            Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            if (config.damageArmor()) {
                damageStack(player, player.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, seen, config, durabilityCounts);
                damageStack(player, player.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST, seen, config, durabilityCounts);
                damageStack(player, player.getItemBySlot(EquipmentSlot.LEGS), EquipmentSlot.LEGS, seen, config, durabilityCounts);
                damageStack(player, player.getItemBySlot(EquipmentSlot.FEET), EquipmentSlot.FEET, seen, config, durabilityCounts);
            }
            if (config.damageMainHand()) {
                damageStack(player, player.getMainHandItem(), EquipmentSlot.MAINHAND, seen, config, durabilityCounts);
            }
            if (config.damageOffHand()) {
                damageStack(player, player.getOffhandItem(), EquipmentSlot.OFFHAND, seen, config, durabilityCounts);
            }
            if (config.damageHotbar()) {
                VersionBridge.forEachHotbarItem(player, stack ->
                        damageStack(player, stack, null, seen, config, durabilityCounts));
            }
            if (config.damageInventory()) {
                VersionBridge.forEachMainInventoryItem(player, stack ->
                        damageStack(player, stack, null, seen, config, durabilityCounts));
            }
            if (config.damageCurios()) {
                AccessoryCompat.forEachCuriosStack(player, stack ->
                        damageStack(player, stack, null, seen, config, durabilityCounts));
            }
            if (config.damageTrinkets()) {
                AccessoryCompat.forEachTrinketsStack(player, stack ->
                        damageStack(player, stack, null, seen, config, durabilityCounts));
            }
            if (config.damageAccessories()) {
                AccessoryCompat.forEachAccessoriesStack(player, stack ->
                        damageStack(player, stack, null, seen, config, durabilityCounts));
            }
        }

        access.keepButPenalty$setPendingRespawn(new PendingRespawnState(
                true,
                !config.respawnDebuffs().isEmpty(),
                captureHunger,
                beforeFood,
                beforeSat,
                xpLost,
                xpLostPercent,
                durabilityCounts[0],
                durabilityCounts[1],
                config.penaltyMode(),
                (int) Math.round(config.durabilityPercent() * 100.0D),
                config.durabilityFlat()
        ));
    }

    public static void onPlayerRespawned(ServerPlayer player, boolean keepEverything) {
        PlayerPenaltyAccess access = (PlayerPenaltyAccess) player;
        PendingRespawnState pending = access.keepButPenalty$getPendingRespawn();
        access.keepButPenalty$setPendingRespawn(PendingRespawnState.empty());

        if (keepEverything || pending == null || !pending.active()) {
            return;
        }

        KeepButPenaltyConfig config = KeepButPenaltyConfig.get();

        // 1. Preserve below-full hunger with anti-starvation floor
        boolean hungerApplied = false;
        int restoredFood = HungerPenaltyPolicy.MAX_FOOD_LEVEL;
        if (pending.hasHungerSnapshot() && config.preserveHungerOnDeath()) {
            HungerPenaltyPolicy.Result hunger = HungerPenaltyPolicy.computeRespawnHunger(
                    pending.beforeFoodLevel(),
                    pending.beforeSaturationLevel(),
                    config.minRespawnFoodLevel()
            );
            if (hunger.apply()) {
                player.getFoodData().setFoodLevel(hunger.foodLevel());
                player.getFoodData().setSaturation(hunger.saturationLevel());
                hungerApplied = true;
                restoredFood = hunger.foodLevel();
            }
        }

        // 2. Apply progressive respawn debuffs
        int debuffsApplied = 0;
        if (pending.applyDebuffs() && !config.respawnDebuffs().isEmpty()) {
            debuffsApplied = applyRespawnDebuffs(player, config);
        }

        // 3. Show concise Actionbar summary if enabled and any penalty actually occurred
        if (config.showRespawnSummary()) {
            sendRespawnSummary(player, pending, hungerApplied, restoredFood, debuffsApplied);
        }
    }

    private static int applyRespawnDebuffs(ServerPlayer player, KeepButPenaltyConfig config) {
        DeathStreakPolicy.State streak = ((PlayerPenaltyAccess) player).keepButPenalty$getDeathStreak();
        int scheduledSeconds = DeathStreakPolicy.durationSeconds(
                config.respawnDebuffDurationsSecondsByDeath(),
                streak.deaths()
        );
        if (scheduledSeconds == 0) {
            return 0;
        }
        int applied = 0;
        for (String value : config.respawnDebuffs()) {
            RespawnDebuffSpec spec;
            try {
                spec = RespawnDebuffSpec.parse(value);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            int durationTicks = scheduledSeconds < 0 ? spec.durationTicks() : scheduledSeconds * 20;
            if (VersionBridge.addMobEffect(player, spec.effectId(), durationTicks, spec.amplifier())) {
                applied++;
            } else if (UNKNOWN_DEBUFFS.add(spec.effectId())) {
                System.getLogger(MOD_ID).log(System.Logger.Level.WARNING,
                        "Unknown configured respawn debuff: " + spec.effectId());
            }
        }
        return applied;
    }

    private static void damageStack(
            ServerPlayer player,
            ItemStack stack,
            EquipmentSlot slot,
            Set<ItemStack> seen,
            KeepButPenaltyConfig config,
            int[] counts
    ) {
        if (stack == null || stack.isEmpty() || !seen.add(stack) || !stack.isDamageableItem()) {
            return;
        }
        if (VersionBridge.isExcluded(stack, config.exclusionMatcher())) {
            return;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }
        boolean isElytra = stack.is(Items.ELYTRA);
        DurabilityPenaltyPolicy.Outcome outcome = DurabilityPenaltyPolicy.evaluate(
                stack.getDamageValue(),
                maxDamage,
                config.penaltyMode(),
                config.durabilityPercent(),
                config.durabilityFlat(),
                config.allowBreak(),
                isElytra
        );
        if (outcome.action() == DurabilityPenaltyPolicy.Action.NONE) {
            return;
        }
        if (outcome.action() == DurabilityPenaltyPolicy.Action.SET_DAMAGE) {
            stack.setDamageValue(outcome.nextDamage());
            counts[0]++;
        } else if (outcome.action() == DurabilityPenaltyPolicy.Action.HURT_AND_BREAK) {
            stack.setDamageValue(outcome.nextDamage());
            VersionBridge.hurtAndBreak(stack, outcome.breakDamageAmount(), player, slot);
            counts[0]++;
            counts[1]++;
        }
    }

    private static void sendRespawnSummary(
            ServerPlayer player,
            PendingRespawnState pending,
            boolean hungerApplied,
            int restoredFood,
            int debuffsApplied
    ) {
        List<Component> parts = new ArrayList<>(4);
        if (pending.xpLost() > 0) {
            parts.add(Component.translatable(
                    "message.keep_but_penalty.summary.xp",
                    pending.xpLostPercent(),
                    pending.xpLost()
            ));
        }
        if (pending.itemsDamaged() > 0) {
            if (pending.durabilityMode() == DurabilityPenaltyMode.PERCENT_MAX) {
                parts.add(Component.translatable(
                        "message.keep_but_penalty.summary.durability_percent",
                        pending.durabilityPercentDisplay()
                ));
            } else {
                parts.add(Component.translatable(
                        "message.keep_but_penalty.summary.durability_flat",
                        pending.durabilityFlatDisplay()
                ));
            }
        }
        if (hungerApplied) {
            parts.add(Component.translatable(
                    "message.keep_but_penalty.summary.hunger",
                    restoredFood
            ));
        }
        if (debuffsApplied > 0) {
            parts.add(Component.translatable(
                    "message.keep_but_penalty.summary.debuffs",
                    debuffsApplied
            ));
        }
        if (parts.isEmpty()) {
            return;
        }
        MutableComponent joined = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                joined.append(Component.literal(" · ").withStyle(ChatFormatting.GRAY));
            }
            joined.append(parts.get(i));
        }
        Component message = Component.translatable("message.keep_but_penalty.summary.prefix", joined)
                .withStyle(ChatFormatting.GOLD);
        VersionBridge.sendActionBar(player, message);
    }

    private static boolean isPvpDeath(ServerPlayer victim, DamageSource source) {
        if (source != null) {
            if (source.getEntity() instanceof Player attacker && attacker != victim) {
                return true;
            }
            if (source.getDirectEntity() instanceof Player attacker && attacker != victim) {
                return true;
            }
        }
        return victim.getKillCredit() instanceof Player killer && killer != victim;
    }
}
