package dev.rinchan.keepbutpenalty.forge.client;

import dev.rinchan.keepbutpenalty.client.KeepButPenaltyConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class KeepButPenaltyForgeClient {
    private KeepButPenaltyForgeClient() {}

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new KeepButPenaltyConfigScreen(parent)));
    }
}
