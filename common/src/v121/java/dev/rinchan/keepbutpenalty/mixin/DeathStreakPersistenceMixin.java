package dev.rinchan.keepbutpenalty.mixin;

import dev.rinchan.keepbutpenalty.policy.DeathStreakPolicy;
import dev.rinchan.keepbutpenalty.state.PendingRespawnState;
import dev.rinchan.keepbutpenalty.state.PlayerPenaltyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class DeathStreakPersistenceMixin implements PlayerPenaltyAccess {
    @Unique private static final String KEY_STREAK_DEATHS = "keep_but_penalty:streak_deaths";
    @Unique private static final String KEY_STREAK_LAST_DEATH = "keep_but_penalty:streak_last_death";
    @Unique private DeathStreakPolicy.State keepButPenalty$deathStreak = DeathStreakPolicy.State.empty();
    @Unique private PendingRespawnState keepButPenalty$pendingRespawn = PendingRespawnState.empty();

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void keepButPenalty$saveDeathStreak(CompoundTag output, CallbackInfo callback) {
        if (keepButPenalty$deathStreak.deaths() > 0) {
            output.putInt(KEY_STREAK_DEATHS, keepButPenalty$deathStreak.deaths());
            output.putLong(KEY_STREAK_LAST_DEATH, keepButPenalty$deathStreak.lastDeathGameTime());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void keepButPenalty$loadDeathStreak(CompoundTag input, CallbackInfo callback) {
        keepButPenalty$deathStreak = new DeathStreakPolicy.State(
                input.getInt(KEY_STREAK_DEATHS),
                input.getLong(KEY_STREAK_LAST_DEATH)
        );
    }

    @Override
    public DeathStreakPolicy.State keepButPenalty$getDeathStreak() {
        return keepButPenalty$deathStreak;
    }

    @Override
    public void keepButPenalty$setDeathStreak(DeathStreakPolicy.State state) {
        keepButPenalty$deathStreak = state == null ? DeathStreakPolicy.State.empty() : state;
    }

    @Override
    public PendingRespawnState keepButPenalty$getPendingRespawn() {
        return keepButPenalty$pendingRespawn;
    }

    @Override
    public void keepButPenalty$setPendingRespawn(PendingRespawnState state) {
        keepButPenalty$pendingRespawn = state == null ? PendingRespawnState.empty() : state;
    }
}
