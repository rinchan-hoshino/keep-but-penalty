package dev.rinchan.keepbutpenalty.state;

import dev.rinchan.keepbutpenalty.config.DurabilityPenaltyMode;

/**
 * Snapshot of death-time penalties carried across ServerPlayer.restoreFrom
 * and applied/displayed cleanly after respawn packets are sent.
 */
public record PendingRespawnState(
        boolean active,
        boolean applyDebuffs,
        boolean hasHungerSnapshot,
        int beforeFoodLevel,
        float beforeSaturationLevel,
        int xpLost,
        int xpLostPercent,
        int itemsDamaged,
        int itemsBroken,
        DurabilityPenaltyMode durabilityMode,
        int durabilityPercentDisplay,
        int durabilityFlatDisplay
) {
    public static PendingRespawnState empty() {
        return new PendingRespawnState(
                false, false, false, 20, 5.0f, 0, 0, 0, 0, DurabilityPenaltyMode.PERCENT_MAX, 0, 0
        );
    }
}
