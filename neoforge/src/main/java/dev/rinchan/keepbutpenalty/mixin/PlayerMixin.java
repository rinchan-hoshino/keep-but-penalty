package dev.rinchan.keepbutpenalty.mixin;

import dev.rinchan.keepbutpenalty.KeepButPenaltyConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void keepButPenalty$keepPlayerInventory(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer && KeepButPenaltyConfig.KEEP_INVENTORY.get()) {
            ci.cancel();
        }
    }
}
