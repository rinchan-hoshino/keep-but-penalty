package dev.rinchan.keepbutpenalty.compat;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import dev.rinchan.keepbutpenalty.KeepButPenaltyConfig;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.events.OnDropCallback;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class AccessoriesCompat {
    private static boolean keepRuleRegistered;

    private AccessoriesCompat() {
    }

    public static void registerKeepRule() {
        if (keepRuleRegistered) {
            return;
        }
        keepRuleRegistered = true;
        OnDropCallback.EVENT.register((rule, stack, reference, source) -> {
            LivingEntity wearer = reference.entity();
            if (wearer instanceof ServerPlayer && KeepButPenaltyConfig.keepInventory.get()) {
                return DropRule.KEEP;
            }
            return rule;
        });
    }

    public static void damageEquipped(ServerPlayer player, Set<ItemStack> seen) {
        AccessoriesCapability.getOptionally(player).ifPresent(capability -> capability.getContainers().values().forEach(container -> {
            for (int slot = 0; slot < container.getAccessories().getContainerSize(); slot++) {
                KeepButPenalty.damage(container.getAccessories().getItem(slot), seen);
            }
        }));
    }
}
