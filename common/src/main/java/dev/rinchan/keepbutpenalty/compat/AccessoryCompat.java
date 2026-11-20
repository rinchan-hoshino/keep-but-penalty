package dev.rinchan.keepbutpenalty.compat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Zero-dependency optional reflection bridge for Curios (Forge/NeoForge),
 * Trinkets (Fabric), and Accessories (Fabric/NeoForge).
 * Safe on all Minecraft versions including 26.x without compile-time mod dependencies.
 */
public final class AccessoryCompat {
    private static volatile boolean curiosChecked;
    private static volatile Method curiosGetInventory;

    private static volatile boolean trinketsChecked;
    private static volatile Method trinketsGetComponent;

    private static volatile boolean accessoriesChecked;
    private static volatile Method accessoriesGetOptionally;

    private AccessoryCompat() {}

    public static void forEachCuriosStack(ServerPlayer player, Consumer<ItemStack> consumer) {
        Method getter = resolveCuriosGetter();
        if (getter == null) return;
        try {
            Object maybeHandler = getter.invoke(null, player);
            Object handler = unwrapOptionalLike(maybeHandler);
            if (handler == null) return;
            Method getCurios = handler.getClass().getMethod("getCurios");
            Object mapObj = getCurios.invoke(handler);
            if (!(mapObj instanceof Map<?, ?> map)) return;
            for (Object stacksHandler : map.values()) {
                if (stacksHandler == null) continue;
                Object dynHandler = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
                if (dynHandler == null) continue;
                int slots = (Integer) dynHandler.getClass().getMethod("getSlots").invoke(dynHandler);
                Method getStack = dynHandler.getClass().getMethod("getStackInSlot", int.class);
                for (int i = 0; i < slots; i++) {
                    Object stackObj = getStack.invoke(dynHandler, i);
                    if (stackObj instanceof ItemStack stack && !stack.isEmpty()) {
                        consumer.accept(stack);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void forEachTrinketsStack(ServerPlayer player, Consumer<ItemStack> consumer) {
        Method getter = resolveTrinketsGetter();
        if (getter == null) return;
        try {
            Object maybeComponent = getter.invoke(null, player);
            Object component = unwrapOptionalLike(maybeComponent);
            if (component == null) return;
            Object equippedObj = component.getClass().getMethod("getAllEquipped").invoke(component);
            if (!(equippedObj instanceof List<?> equipped)) return;
            for (Object pair : equipped) {
                if (pair == null) continue;
                Object stackObj = invokeNoArg(pair, "getB", "getRight");
                if (stackObj instanceof ItemStack stack && !stack.isEmpty()) {
                    consumer.accept(stack);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void forEachAccessoriesStack(ServerPlayer player, Consumer<ItemStack> consumer) {
        Method getter = resolveAccessoriesGetter();
        if (getter == null) return;
        try {
            Object maybeCap = getter.invoke(null, player);
            Object cap = unwrapOptionalLike(maybeCap);
            if (cap == null) return;
            Object containersObj = cap.getClass().getMethod("getContainers").invoke(cap);
            if (!(containersObj instanceof Map<?, ?> containers)) return;
            Collection<?> values = containers.values();
            for (Object container : values) {
                if (container == null) continue;
                Object inv = container.getClass().getMethod("getAccessories").invoke(container);
                if (inv == null) continue;
                int size = (Integer) inv.getClass().getMethod("getContainerSize").invoke(inv);
                Method getItem = inv.getClass().getMethod("getItem", int.class);
                for (int i = 0; i < size; i++) {
                    Object stackObj = getItem.invoke(inv, i);
                    if (stackObj instanceof ItemStack stack && !stack.isEmpty()) {
                        consumer.accept(stack);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Method resolveCuriosGetter() {
        if (!curiosChecked) {
            try {
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                curiosGetInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
            } catch (Throwable ignored) {
                curiosGetInventory = null;
            }
            curiosChecked = true;
        }
        return curiosGetInventory;
    }

    private static Method resolveTrinketsGetter() {
        if (!trinketsChecked) {
            try {
                Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
                trinketsGetComponent = api.getMethod("getTrinketComponent", LivingEntity.class);
            } catch (Throwable ignored) {
                trinketsGetComponent = null;
            }
            trinketsChecked = true;
        }
        return trinketsGetComponent;
    }

    private static Method resolveAccessoriesGetter() {
        if (!accessoriesChecked) {
            try {
                Class<?> api = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
                accessoriesGetOptionally = api.getMethod("getOptionally", LivingEntity.class);
            } catch (Throwable ignored) {
                accessoriesGetOptionally = null;
            }
            accessoriesChecked = true;
        }
        return accessoriesGetOptionally;
    }

    private static Object unwrapOptionalLike(Object maybe) {
        if (maybe == null) return null;
        if (maybe instanceof Optional<?> opt) {
            return opt.orElse(null);
        }
        // Handle Forge 1.20.1 LazyOptional without importing Forge classes
        try {
            Boolean present = (Boolean) maybe.getClass().getMethod("isPresent").invoke(maybe);
            if (Boolean.TRUE.equals(present)) {
                return maybe.getClass().getMethod("orElse", Object.class).invoke(maybe, new Object[]{null});
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String primary, String fallback) {
        try {
            return target.getClass().getMethod(primary).invoke(target);
        } catch (Throwable ignored) {
        }
        try {
            return target.getClass().getMethod(fallback).invoke(target);
        } catch (Throwable ignored) {
        }
        return null;
    }
}
