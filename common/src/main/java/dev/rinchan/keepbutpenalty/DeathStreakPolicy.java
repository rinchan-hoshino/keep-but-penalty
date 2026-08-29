package dev.rinchan.keepbutpenalty;

import java.util.List;

public final class DeathStreakPolicy {
    public static final int MAX_DURATION_SECONDS = 86_400;

    private DeathStreakPolicy() {
    }

    public static State advance(State previous, long currentGameTime, long resetTicks) {
        boolean reset = previous.deaths() <= 0
            || currentGameTime < previous.lastDeathGameTime()
            || currentGameTime - previous.lastDeathGameTime() >= resetTicks;
        int deaths = reset ? 1 : (int) Math.min(Integer.MAX_VALUE, (long) previous.deaths() + 1L);
        return new State(deaths, currentGameTime);
    }

    public static int durationSeconds(List<? extends Integer> schedule, int deaths) {
        if (schedule.isEmpty()) {
            return -1;
        }
        int index = Math.max(0, Math.min(schedule.size() - 1, deaths - 1));
        return schedule.get(index);
    }

    public static boolean isValidDuration(Object value) {
        return value instanceof Integer seconds && seconds >= 0 && seconds <= MAX_DURATION_SECONDS;
    }

    public record State(int deaths, long lastDeathGameTime) {
        public State {
            deaths = Math.max(0, deaths);
        }

        public static State empty() {
            return new State(0, Long.MIN_VALUE);
        }
    }
}
