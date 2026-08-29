package dev.rinchan.keepbutpenalty.mixin;

import dev.rinchan.keepbutpenalty.DeathStreakAccess;
import dev.rinchan.keepbutpenalty.DeathStreakPolicy;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class DeathStreakPersistenceMixin implements DeathStreakAccess {
    @Unique private static final String KEEP_BUT_PENALTY_STREAK_DEATHS = "keep_but_penalty:streak_deaths";
    @Unique private static final String KEEP_BUT_PENALTY_STREAK_LAST_DEATH = "keep_but_penalty:streak_last_death";
    @Unique private DeathStreakPolicy.State keepButPenalty$deathStreak = DeathStreakPolicy.State.empty();

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void keepButPenalty$saveDeathStreak(net.minecraft.nbt.CompoundTag output, CallbackInfo callback) {
        if (keepButPenalty$deathStreak.deaths() > 0) {
            output.putInt(KEEP_BUT_PENALTY_STREAK_DEATHS, keepButPenalty$deathStreak.deaths());
            output.putLong(KEEP_BUT_PENALTY_STREAK_LAST_DEATH, keepButPenalty$deathStreak.lastDeathGameTime());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void keepButPenalty$loadDeathStreak(net.minecraft.nbt.CompoundTag input, CallbackInfo callback) {
        keepButPenalty$deathStreak = new DeathStreakPolicy.State(
            input.getInt(KEEP_BUT_PENALTY_STREAK_DEATHS),
            input.getLong(KEEP_BUT_PENALTY_STREAK_LAST_DEATH)
        );
    }

    @Override
    public DeathStreakPolicy.State keepButPenalty$getDeathStreak() { return keepButPenalty$deathStreak; }

    @Override
    public void keepButPenalty$setDeathStreak(DeathStreakPolicy.State state) { keepButPenalty$deathStreak = state; }
}
