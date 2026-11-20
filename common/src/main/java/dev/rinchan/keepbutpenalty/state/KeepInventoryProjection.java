package dev.rinchan.keepbutpenalty.state;

/**
 * Reentrant ThreadLocal scope that projects vanilla keepInventory=true only while
 * a player death or death-respawn call is executing, never mutating the world gamerule.
 */
public final class KeepInventoryProjection {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private KeepInventoryProjection() {}

    public static void push() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void pop() {
        int next = DEPTH.get() - 1;
        if (next <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(next);
        }
    }

    public static boolean isActive() {
        return DEPTH.get() > 0;
    }
}
