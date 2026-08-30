package dev.rinchan.keepbutpenalty.neoforge;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import dev.rinchan.keepbutpenalty.KeepButPenaltyConfig;
import dev.rinchan.keepbutpenalty.compat.AccessoriesCompat;
import dev.rinchan.keepbutpenalty.compat.CuriosCompat;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(KeepButPenalty.MOD_ID)
public final class KeepButPenaltyNeoForge {
    public KeepButPenaltyNeoForge(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, KeepButPenaltyConfig.SPEC);
        if (ModList.get().isLoaded("curios")) CuriosCompat.registerDropRetention();
        if (ModList.get().isLoaded("accessories")) AccessoriesCompat.registerDropRetention();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KeepButPenaltyNeoForge::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(KeepButPenaltyNeoForge::onPlayerRespawn);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KeepButPenalty.recordFinalDeath(player);
            KeepButPenalty.applyDeathPenalty(player);
        }
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isEndConquered()) {
            KeepButPenalty.applyRespawnDebuffs(player);
        }
    }
}
