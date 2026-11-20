package dev.rinchan.keepbutpenalty.fabric;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class KeepButPenaltyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        KeepButPenalty.init(FabricLoader.getInstance().getConfigDir());
    }
}
