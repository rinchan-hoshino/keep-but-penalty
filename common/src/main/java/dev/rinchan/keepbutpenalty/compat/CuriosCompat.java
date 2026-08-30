package dev.rinchan.keepbutpenalty.compat;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import dev.rinchan.keepbutpenalty.KeepButPenaltyConfig;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static void registerDropRetention() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CuriosCompat::keepDrops);
    }

    private static void keepDrops(DropRulesEvent event) {
        if (KeepButPenaltyConfig.KEEP_INVENTORY.get()) {
            event.addOverride(stack -> true, DropRule.ALWAYS_KEEP);
        }
    }

    public static void damageEquipped(ServerPlayer player, Set<ItemStack> seen) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getCurios().values().forEach(stacksHandler -> {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                KeepButPenalty.damage(stacks.getStackInSlot(slot), seen);
            }
        }));
    }
}
