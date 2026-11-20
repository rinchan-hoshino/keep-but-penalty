package dev.rinchan.keepbutpenalty.forge;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import dev.rinchan.keepbutpenalty.forge.client.KeepButPenaltyForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(KeepButPenalty.MOD_ID)
public final class KeepButPenaltyForge {
    public KeepButPenaltyForge() {
        KeepButPenalty.init(FMLPaths.CONFIGDIR.get());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> KeepButPenaltyForgeClient::registerConfigScreen);
    }
}
