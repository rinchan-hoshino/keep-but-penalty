package dev.rinchan.keepbutpenalty.compat;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import dev.rinchan.keepbutpenalty.KeepButPenaltyConfig;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosCompat {
    private static boolean keepRuleRegistered;

    private CuriosCompat() {
    }

    public static void registerKeepRule() {
        if (keepRuleRegistered) {
            return;
        }
        keepRuleRegistered = true;
        NeoForge.EVENT_BUS.addListener(CuriosCompat::keepEquippedOnDeath);
    }

    private static void keepEquippedOnDeath(DropRulesEvent event) {
        if (event.getEntity() instanceof ServerPlayer && KeepButPenaltyConfig.keepInventory.get()) {
            event.addOverride(stack -> true, ICurio.DropRule.ALWAYS_KEEP);
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
