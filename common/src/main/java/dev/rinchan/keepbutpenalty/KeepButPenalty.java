package dev.rinchan.keepbutpenalty;

import dev.rinchan.keepbutpenalty.compat.AccessoriesCompat;
import dev.rinchan.keepbutpenalty.compat.CuriosCompat;
import dev.rinchan.rinlib.minecraft.MobEffectState;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class KeepButPenalty {
    public static final String MOD_ID = "keep_but_penalty";
    private static final Set<String> UNKNOWN_DEBUFFS = ConcurrentHashMap.newKeySet();

    private KeepButPenalty() {
    }

    public static void applyDeathPenalty(ServerPlayer player) {
        if (KeepButPenaltyConfig.ENABLE_EXPERIENCE_PENALTY.get()) {
            int remainingXp = (int) Math.floor(totalExperience(player) * KeepButPenaltyConfig.EXPERIENCE_KEEP_RATIO.get());
            player.totalExperience = 0;
            player.experienceLevel = 0;
            player.experienceProgress = 0;
            if (remainingXp > 0) {
                player.giveExperiencePoints(remainingXp);
            }
        }

        if (KeepButPenaltyConfig.ENABLE_DURABILITY_PENALTY.get()) {
            Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            if (KeepButPenaltyConfig.DAMAGE_ARMOR.get()) {
                damage(player.getItemBySlot(EquipmentSlot.HEAD), seen);
                damage(player.getItemBySlot(EquipmentSlot.CHEST), seen);
                damage(player.getItemBySlot(EquipmentSlot.LEGS), seen);
                damage(player.getItemBySlot(EquipmentSlot.FEET), seen);
            }
            if (KeepButPenaltyConfig.DAMAGE_MAIN_HAND.get()) {
                damage(player.getMainHandItem(), seen);
            }
            if (KeepButPenaltyConfig.DAMAGE_OFF_HAND.get()) {
                damage(player.getOffhandItem(), seen);
            }
            if (KeepButPenaltyConfig.DAMAGE_CURIOS.get() && ModList.get().isLoaded("curios")) {
                CuriosCompat.damageEquipped(player, seen);
            }
            if (KeepButPenaltyConfig.DAMAGE_ACCESSORIES.get() && ModList.get().isLoaded("accessories")) {
                AccessoriesCompat.damageEquipped(player, seen);
            }
        }
    }

    public static void applyRespawnDebuffs(ServerPlayer player) {
        for (String value : KeepButPenaltyConfig.RESPAWN_DEBUFFS.get()) {
            RespawnDebuffSpec spec = RespawnDebuffSpec.parse(value);
            if (!MobEffectState.add(player, spec.effectId(), spec.durationTicks(), spec.amplifier())
                    && UNKNOWN_DEBUFFS.add(spec.effectId())) {
                System.getLogger(MOD_ID).log(System.Logger.Level.WARNING,
                        "Unknown configured respawn debuff: " + spec.effectId());
            }
        }
    }

    public static void damage(ItemStack stack, Set<ItemStack> seen) {
        if (stack == null || stack.isEmpty() || !seen.add(stack) || !stack.isDamageableItem()) {
            return;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }
        int limit = KeepButPenaltyConfig.ALLOW_ZERO_DURABILITY.get() ? maxDamage : Math.max(0, maxDamage - 1);
        int nextDamage = Math.min(limit, stack.getDamageValue() + KeepButPenaltyConfig.DURABILITY_LOSS.get());
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
