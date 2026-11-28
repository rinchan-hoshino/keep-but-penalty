package dev.rinchan.keepbutpenalty;

import dev.rinchan.keepbutpenalty.config.ExclusionMatcher;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Minecraft 26.x version bridge. */
public final class VersionBridge {
    private VersionBridge() {}

    public static boolean isExcluded(ItemStack stack, ExclusionMatcher matcher) {
        if (matcher == null || (matcher.itemIds().isEmpty() && matcher.tagIds().isEmpty())) {
            return false;
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemId = key != null ? key.toString() : null;
        return matcher.matches(itemId, tagId -> {
            Identifier loc = Identifier.tryParse(tagId);
            return loc != null && stack.is(TagKey.create(Registries.ITEM, loc));
        });
    }

    public static void hurtAndBreak(ItemStack stack, int amount, ServerPlayer player, EquipmentSlot slot) {
        if (slot != null) {
            stack.hurtAndBreak(amount, player, slot);
        } else if (player.level() instanceof ServerLevel serverLevel) {
            stack.hurtAndBreak(amount, serverLevel, player, item -> {});
        }
    }

    public static boolean addMobEffect(ServerPlayer player, String effectId, int durationTicks, int amplifier) {
        Identifier id = Identifier.tryParse(effectId);
        if (id == null) {
            return false;
        }
        return BuiltInRegistries.MOB_EFFECT.get(id)
                .map(effect -> player.addEffect(new MobEffectInstance(effect, durationTicks, amplifier)))
                .orElse(false);
    }

    public static void sendActionBar(ServerPlayer player, Component message) {
        player.sendOverlayMessage(message);
    }

    public static void forEachInventoryItem(ServerPlayer player, boolean skipMainHand, Consumer<ItemStack> consumer) {
        var inv = player.getInventory();
        var items = inv.getNonEquipmentItems();
        int selected = inv.getSelectedSlot();
        for (int i = 0; i < items.size(); i++) {
            if (skipMainHand && i == selected) {
                continue;
            }
            consumer.accept(items.get(i));
        }
    }
}
