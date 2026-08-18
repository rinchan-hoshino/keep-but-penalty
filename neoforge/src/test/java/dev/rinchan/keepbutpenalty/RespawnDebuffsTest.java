package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.Test;

class RespawnDebuffsTest {
    @Test
    void defaultPenaltyUsesVanillaWeaknessAndMiningFatigueOneForOneMinute() {
        var effects = KeepButPenalty.createRespawnDebuffs(
            KeepButPenaltyConfig.respawnDebuffDurationSeconds.getDefault()
        );

        assertTrue(KeepButPenaltyConfig.enableRespawnDebuffs.getDefault());
        assertEquals(2, effects.size());
        assertSame(MobEffects.WEAKNESS, effects.get(0).getEffect());
        assertSame(MobEffects.DIG_SLOWDOWN, effects.get(1).getEffect());
        for (var effect : effects) {
            assertEquals(60 * 20, effect.getDuration());
            assertEquals(0, effect.getAmplifier());
        }
    }
}
