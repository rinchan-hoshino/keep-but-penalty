package dev.rinchan.keepbutpenalty.neoforge;

import dev.rinchan.keepbutpenalty.KeepButPenalty;
import dev.rinchan.keepbutpenalty.KeepButPenaltyConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(KeepButPenalty.MOD_ID)
public class KeepButPenaltyNeoForge {
    public KeepButPenaltyNeoForge(IEventBus modBus) {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, KeepButPenaltyConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
    }

    private void onServerStarted(ServerStartedEvent event) {
        KeepButPenalty.publishInventoryRetention(event.getServer());
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KeepButPenalty.applyDeathPenalty(player);
        }
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isEndConquered()) {
            KeepButPenalty.applyRespawnDebuffs(player);
        }
    }
}
