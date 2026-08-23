package dev.rinchan.keepbutpenalty;

import dev.rinchan.keepbutpenalty.compat.AccessoriesCompat;
import dev.rinchan.keepbutpenalty.compat.CuriosCompat;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.fml.ModList;

public final class KeepButPenalty {
    public static final String MOD_ID = "keep_but_penalty";

    private KeepButPenalty() {
    }

    public static void publishInventoryRetention(MinecraftServer server) {
        server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);
    }

    public static void applyDeathPenalty(ServerPlayer player) {
        if (KeepButPenaltyConfig.enableExperiencePenalty.get()) {
            int remainingXp = (int) Math.floor(totalExperience(player) * KeepButPenaltyConfig.experienceKeepRatio.get());
            player.totalExperience = 0;
            player.experienceLevel = 0;
            player.experienceProgress = 0;
            if (remainingXp > 0) {
                player.giveExperiencePoints(remainingXp);
            }
        }

        if (KeepButPenaltyConfig.enableDurabilityPenalty.get()) {
            Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            if (KeepButPenaltyConfig.damageArmor.get()) {
                damage(player.getItemBySlot(EquipmentSlot.HEAD), seen);
                damage(player.getItemBySlot(EquipmentSlot.CHEST), seen);
                damage(player.getItemBySlot(EquipmentSlot.LEGS), seen);
                damage(player.getItemBySlot(EquipmentSlot.FEET), seen);
            }
            if (KeepButPenaltyConfig.damageMainHand.get()) {
                damage(player.getMainHandItem(), seen);
            }
            if (KeepButPenaltyConfig.damageOffHand.get()) {
                damage(player.getOffhandItem(), seen);
            }
            if (KeepButPenaltyConfig.damageCurios.get() && ModList.get().isLoaded("curios")) {
                CuriosCompat.damageEquipped(player, seen);
            }
            if (KeepButPenaltyConfig.damageAccessories.get() && ModList.get().isLoaded("accessories")) {
                AccessoriesCompat.damageEquipped(player, seen);
            }
        }
    }

    public static void applyRespawnDebuffs(ServerPlayer player) {
        if (!KeepButPenaltyConfig.enableRespawnDebuffs.get()) {
            return;
        }
        for (var effect : createRespawnDebuffs(
            KeepButPenaltyConfig.respawnDebuffDurationSeconds.get(),
            KeepButPenaltyConfig.respawnWeaknessLevel.get(),
            KeepButPenaltyConfig.respawnMiningFatigueLevel.get()
        )) {
            player.addEffect(effect);
        }
    }

    static List<MobEffectInstance> createRespawnDebuffs(
        int durationSeconds,
        int weaknessLevel,
        int miningFatigueLevel
    ) {
        int durationTicks = durationSeconds * 20;
        return List.of(
            new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, weaknessLevel - 1),
            new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, miningFatigueLevel - 1)
        );
    }

    public static void damage(ItemStack stack, Set<ItemStack> seen) {
        if (stack == null || stack.isEmpty() || !seen.add(stack) || !stack.isDamageableItem()) {
            return;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }
        int limit = KeepButPenaltyConfig.allowZeroDurability.get() ? maxDamage : Math.max(0, maxDamage - 1);
        int nextDamage = Math.min(limit, stack.getDamageValue() + KeepButPenaltyConfig.durabilityLoss.get());
        stack.setDamageValue(nextDamage);
    }

    private static int totalExperience(Player player) {
        int level = Math.max(0, player.experienceLevel);
        float progress = Math.max(0f, Math.min(1f, player.experienceProgress));
        return xpForLevel(level) + (int) Math.floor(progress * xpToNextLevel(level));
    }

    private static int xpForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) Math.floor(2.5D * level * level - 40.5D * level + 360D);
        }
        return (int) Math.floor(4.5D * level * level - 162.5D * level + 2220D);
    }

    private static int xpToNextLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        if (level >= 15) {
            return 37 + (level - 15) * 5;
        }
        return 7 + level * 2;
    }
}
