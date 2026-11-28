package dev.rinchan.keepbutpenalty;

import dev.rinchan.keepbutpenalty.config.ExclusionMatcher;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Minecraft 1.20.1 version bridge. */
public final class VersionBridge {
    private VersionBridge() {}

    public static boolean isExcluded(ItemStack stack, ExclusionMatcher matcher) {
        if (matcher == null || (matcher.itemIds().isEmpty() && matcher.tagIds().isEmpty())) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemId = key != null ? key.toString() : null;
        return matcher.matches(itemId, tagId -> {
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            return loc != null && stack.is(TagKey.create(Registries.ITEM, loc));
        });
    }

    public static void hurtAndBreak(ItemStack stack, int amount, ServerPlayer player, EquipmentSlot slot) {
        stack.hurtAndBreak(amount, player, p -> {
            if (slot != null) {
                p.broadcastBreakEvent(slot);
            }
        });
    }

    public static boolean addMobEffect(ServerPlayer player, String effectId, int durationTicks, int amplifier) {
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        if (id == null) {
            return false;
        }
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect == null) {
            return false;
        }
        return player.addEffect(new MobEffectInstance(effect, durationTicks, amplifier));
    }

    public static void sendActionBar(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }

    public static void forEachInventoryItem(ServerPlayer player, boolean skipMainHand, Consumer<ItemStack> consumer) {
        var inv = player.getInventory();
        int selected = inv.selected;
        for (int i = 0; i < inv.items.size(); i++) {
            if (skipMainHand && i == selected) {
                continue;
            }
            consumer.accept(inv.items.get(i));
        }
    }
}
