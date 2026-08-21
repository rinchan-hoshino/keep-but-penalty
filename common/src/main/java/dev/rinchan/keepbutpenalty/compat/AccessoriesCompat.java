package dev.rinchan.keepbutpenalty.compat;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import io.wispforest.accessories.api.AccessoriesCapability;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AccessoriesCompat {
    private AccessoriesCompat() {
    }

    public static void damageEquipped(ServerPlayer player, Set<ItemStack> seen) {
        AccessoriesCapability.getOptionally(player).ifPresent(capability -> capability.getContainers().values().forEach(container -> {
            for (int slot = 0; slot < container.getAccessories().getContainerSize(); slot++) {
                KeepButPenalty.damage(container.getAccessories().getItem(slot), seen);
            }
        }));
    }
}
