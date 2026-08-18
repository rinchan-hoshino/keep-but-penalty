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
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(KeepButPenalty.MOD_ID)
public class KeepButPenaltyNeoForge {
    public KeepButPenaltyNeoForge(IEventBus modBus) {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, KeepButPenaltyConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(this::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(this::onLivingExperienceDrop);
        NeoForge.EVENT_BUS.addListener(this::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KeepButPenalty.applyDeathPenalty(player);
        }
    }

    private void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KeepButPenalty.shouldKeepInventory(player)) {
            event.setCanceled(true);
            event.getDrops().clear();
        }
    }

    private void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KeepButPenalty.shouldKeepInventory(player)) {
            event.setCanceled(true);
            event.setDroppedExperience(0);
        }
    }

    private void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            KeepButPenalty.restoreAfterClone(player);
        }
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KeepButPenalty.finishRespawn(player);
            if (!event.isEndConquered()) {
                KeepButPenalty.applyRespawnWeakness(player);
            }
        }
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        KeepButPenalty.clearPlayer(event.getEntity().getUUID());
    }
}
