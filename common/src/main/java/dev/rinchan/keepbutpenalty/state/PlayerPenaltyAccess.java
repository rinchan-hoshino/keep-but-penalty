package dev.rinchan.keepbutpenalty.state;

import dev.rinchan.keepbutpenalty.policy.DeathStreakPolicy;

public interface PlayerPenaltyAccess {
    DeathStreakPolicy.State keepButPenalty$getDeathStreak();

    void keepButPenalty$setDeathStreak(DeathStreakPolicy.State state);

    PendingRespawnState keepButPenalty$getPendingRespawn();

    void keepButPenalty$setPendingRespawn(PendingRespawnState state);
}
