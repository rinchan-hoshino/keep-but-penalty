package dev.rinchan.keepbutpenalty.neoforge;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(KeepButPenalty.MOD_ID)
public final class KeepButPenaltyNeoForge {
    public KeepButPenaltyNeoForge() {
        KeepButPenalty.init(FMLPaths.CONFIGDIR.get());
    }
}
