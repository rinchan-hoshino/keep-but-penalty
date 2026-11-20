package dev.rinchan.keepbutpenalty.neoforge.client;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import dev.rinchan.keepbutpenalty.client.KeepButPenaltyConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = KeepButPenalty.MOD_ID, dist = Dist.CLIENT)
public final class KeepButPenaltyNeoForgeClient {
    public KeepButPenaltyNeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new KeepButPenaltyConfigScreen(parent));
    }
}
