package dev.rinchan.keepbutpenalty.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rinchan.keepbutpenalty.DeathStreakAccess;
import dev.rinchan.keepbutpenalty.KeepButPenaltyConfig;
import dev.rinchan.rinlib.minecraft.KeepInventoryProjection;
import dev.rinchan.rinlib.state.ReentrantFlag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @WrapMethod(method = "die")
    private void keepButPenalty$projectInventoryRetentionDuringDeath(DamageSource source, Operation<Void> original) {
        if (!KeepButPenaltyConfig.KEEP_INVENTORY.get()) {
            original.call(source);
            return;
        }
        try (ReentrantFlag.Scope ignored = KeepInventoryProjection.enter()) {
            original.call(source);
        }
    }

    @WrapMethod(method = "restoreFrom")
    private void keepButPenalty$projectInventoryRetentionDuringDeathClone(
            ServerPlayer previous,
            boolean keepEverything,
            Operation<Void> original
    ) {
        if (!KeepButPenaltyConfig.KEEP_INVENTORY.get() || keepEverything) {
            original.call(previous, keepEverything);
        } else {
            try (ReentrantFlag.Scope ignored = KeepInventoryProjection.enter()) {
                original.call(previous, false);
            }
        }
        ((DeathStreakAccess) this).keepButPenalty$setDeathStreak(
            ((DeathStreakAccess) previous).keepButPenalty$getDeathStreak()
        );
    }

}
