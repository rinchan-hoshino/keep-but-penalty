package dev.rinchan.keepbutpenalty.mixin;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    @Inject(method = "respawn", at = @At("RETURN"))
    private void keepButPenalty$onPlayerRespawn(
            ServerPlayer oldPlayer,
            boolean keepEverything,
            Entity.RemovalReason removalReason,
            CallbackInfoReturnable<ServerPlayer> cir
    ) {
        ServerPlayer respawned = cir.getReturnValue();
        if (respawned != null) {
            KeepButPenalty.onPlayerRespawned(respawned, keepEverything);
        }
    }
}
