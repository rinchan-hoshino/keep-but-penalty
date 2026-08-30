package dev.rinchan.keepbutpenalty.compat;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import dev.rinchan.keepbutpenalty.KeepButPenaltyConfig;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.events.OnDeathCallback;
import io.wispforest.accessories.api.events.OnDropCallback;
import java.util.Set;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AccessoriesCompat {
    private AccessoriesCompat() {
    }

    public static void registerDropRetention() {
        OnDeathCallback.EVENT.register((current, entity, capability, source, drops) -> {
            if (entity instanceof ServerPlayer && KeepButPenaltyConfig.KEEP_INVENTORY.get()) {
                return TriState.FALSE;
            }
            return current;
        });
        OnDropCallback.EVENT.register((current, stack, reference, source) -> {
            if (reference.entity() instanceof ServerPlayer && KeepButPenaltyConfig.KEEP_INVENTORY.get()) {
                return DropRule.KEEP;
            }
            return current;
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
