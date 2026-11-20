package dev.rinchan.keepbutpenalty.mixin;

import dev.rinchan.keepbutpenalty.state.KeepInventoryProjection;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRules.class)
abstract class GameRulesMixin {
    @Inject(method = "getBoolean", at = @At("HEAD"), cancellable = true)
    private void keepButPenalty$projectKeepInventory(
            GameRules.Key<GameRules.BooleanValue> key,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (key == GameRules.RULE_KEEPINVENTORY && KeepInventoryProjection.isActive()) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
