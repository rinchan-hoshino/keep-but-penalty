package dev.rinchan.keepbutpenalty;

public interface DeathStreakAccess {
    DeathStreakPolicy.State keepButPenalty$getDeathStreak();

    void keepButPenalty$setDeathStreak(DeathStreakPolicy.State state);
}
