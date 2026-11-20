package dev.rinchan.keepbutpenalty.mixin;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import dev.rinchan.keepbutpenalty.config.KeepButPenaltyConfig;
import dev.rinchan.keepbutpenalty.state.KeepInventoryProjection;
import dev.rinchan.keepbutpenalty.state.PlayerPenaltyAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pure Sponge Mixin hooks on ServerPlayer.die and ServerPlayer.restoreFrom.
 * Projects keepInventory=true only within the death/respawn scope without mutating world gamerules,
 * applies death penalties only on uncancelled final death (TAIL), and carries state across respawn.
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Unique private int keepButPenalty$dieProjectionDepth;
    @Unique private int keepButPenalty$restoreProjectionDepth;

    @Inject(method = "die", at = @At("HEAD"))
    private void keepButPenalty$enterDieProjection(DamageSource source, CallbackInfo ci) {
        if (KeepButPenaltyConfig.get().keepInventory()) {
            KeepInventoryProjection.push();
            this.keepButPenalty$dieProjectionDepth++;
        }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void keepButPenalty$applyDeathPenalty(DamageSource source, CallbackInfo ci) {
        KeepButPenalty.onPlayerDied((ServerPlayer) (Object) this, source);
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void keepButPenalty$exitDieProjection(DamageSource source, CallbackInfo ci) {
        if (this.keepButPenalty$dieProjectionDepth > 0) {
            this.keepButPenalty$dieProjectionDepth--;
            KeepInventoryProjection.pop();
        }
    }

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void keepButPenalty$enterRestoreProjection(ServerPlayer previous, boolean keepEverything, CallbackInfo ci) {
        if (!keepEverything && KeepButPenaltyConfig.get().keepInventory()) {
            KeepInventoryProjection.push();
            this.keepButPenalty$restoreProjectionDepth++;
        }
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void keepButPenalty$copyPenaltyState(ServerPlayer previous, boolean keepEverything, CallbackInfo ci) {
        PlayerPenaltyAccess self = (PlayerPenaltyAccess) this;
        PlayerPenaltyAccess prev = (PlayerPenaltyAccess) previous;
        self.keepButPenalty$setDeathStreak(prev.keepButPenalty$getDeathStreak());
        if (!keepEverything) {
            self.keepButPenalty$setPendingRespawn(prev.keepButPenalty$getPendingRespawn());
        }
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void keepButPenalty$exitRestoreProjection(ServerPlayer previous, boolean keepEverything, CallbackInfo ci) {
        if (this.keepButPenalty$restoreProjectionDepth > 0) {
            this.keepButPenalty$restoreProjectionDepth--;
            KeepInventoryProjection.pop();
        }
    }
}
