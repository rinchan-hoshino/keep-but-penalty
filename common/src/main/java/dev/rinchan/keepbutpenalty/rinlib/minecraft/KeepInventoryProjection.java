package dev.rinchan.keepbutpenalty.rinlib.minecraft;

import dev.rinchan.keepbutpenalty.rinlib.state.ReentrantFlag;

public final class KeepInventoryProjection {
    private static final ReentrantFlag ACTIVE = new ReentrantFlag();

    private KeepInventoryProjection() {
    }

    public static ReentrantFlag.Scope enter() {
        return ACTIVE.enter();
    }

    public static boolean isActive() {
        return ACTIVE.isSet();
    }
}
