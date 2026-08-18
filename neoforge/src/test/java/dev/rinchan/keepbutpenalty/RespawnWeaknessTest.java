package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.Test;

class RespawnWeaknessTest {
    @Test
    void defaultPenaltyUsesVanillaWeaknessOneForThreeMinutes() {
        var effect = KeepButPenalty.createRespawnWeakness(
            KeepButPenaltyConfig.respawnWeaknessDurationSeconds.getDefault(),
            KeepButPenaltyConfig.respawnWeaknessAmplifier.getDefault()
        );

        assertTrue(KeepButPenaltyConfig.enableRespawnWeakness.getDefault());
        assertSame(MobEffects.WEAKNESS, effect.getEffect());
        assertEquals(180 * 20, effect.getDuration());
        assertEquals(0, effect.getAmplifier());
    }
}
