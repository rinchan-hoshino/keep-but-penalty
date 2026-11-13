package dev.rinchan.keepbutpenalty.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rinchan.keepbutpenalty.rinlib.minecraft.KeepInventoryProjection;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRules.class)
abstract class GameRulesMixin {
    @WrapMethod(method = "getBoolean")
    private boolean keepButPenalty$projectKeepInventory(
            GameRules.Key<GameRules.BooleanValue> key,
            Operation<Boolean> original
    ) {
        if (key == GameRules.RULE_KEEPINVENTORY && KeepInventoryProjection.isActive()) {
            return true;
        }
        return original.call(key);
    }
}
