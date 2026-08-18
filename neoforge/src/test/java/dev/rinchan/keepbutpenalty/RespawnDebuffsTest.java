package dev.rinchan.keepbutpenalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.Test;

class RespawnDebuffsTest {
    @Test
    void defaultPenaltyUsesVanillaWeaknessAndMiningFatigueOneForOneMinute() {
        var effects = KeepButPenalty.createRespawnDebuffs(
            KeepButPenaltyConfig.respawnDebuffDurationSeconds.getDefault(),
            KeepButPenaltyConfig.respawnWeaknessLevel.getDefault(),
            KeepButPenaltyConfig.respawnMiningFatigueLevel.getDefault()
        );

        assertTrue(KeepButPenaltyConfig.enableRespawnDebuffs.getDefault());
        assertEquals(1, KeepButPenaltyConfig.respawnWeaknessLevel.getDefault());
        assertEquals(1, KeepButPenaltyConfig.respawnMiningFatigueLevel.getDefault());
        assertEffects(effects, 60 * 20, 0, 0);
    }

    @Test
    void configuredLevelsConvertToVanillaAmplifiers() {
        var effects = KeepButPenalty.createRespawnDebuffs(45, 2, 3);

        assertEffects(effects, 45 * 20, 1, 2);
    }

    private static void assertEffects(
        List<MobEffectInstance> effects,
        int durationTicks,
        int weaknessAmplifier,
        int miningFatigueAmplifier
    ) {
        assertEquals(2, effects.size());
        assertSame(MobEffects.WEAKNESS, effects.get(0).getEffect());
        assertSame(MobEffects.DIG_SLOWDOWN, effects.get(1).getEffect());
        assertEquals(durationTicks, effects.get(0).getDuration());
        assertEquals(durationTicks, effects.get(1).getDuration());
        assertEquals(weaknessAmplifier, effects.get(0).getAmplifier());
        assertEquals(miningFatigueAmplifier, effects.get(1).getAmplifier());
    }
}
