package dev.rinchan.keepbutpenalty.mixin;

import dev.rinchan.keepbutpenalty.state.KeepInventoryProjection;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRules.class)
abstract class GameRulesMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    private <T> void keepButPenalty$projectKeepInventory(GameRule<T> rule, CallbackInfoReturnable<T> result) {
        if (rule == GameRules.KEEP_INVENTORY && KeepInventoryProjection.isActive()) {
            result.setReturnValue((T) Boolean.TRUE);
        }
    }
}
