package com.sportradar.scoreboard;

import java.util.Objects;

public final class Score {

    private final int homeGoals;
    private final int awayGoals;

    private Score(int homeGoals, int awayGoals) {
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }

    public static Score initial() {
        return new Score(0, 0);
    }

    public static Score of(int homeGoals, int awayGoals) {
        if (homeGoals < 0) {
            throw new InvalidScoreException("Home goals must not be negative: " + homeGoals);
        }
        if (awayGoals < 0) {
            throw new InvalidScoreException("Away goals must not be negative: " + awayGoals);
        }
        return new Score(homeGoals, awayGoals);
    }

    public int homeGoals() {
        return homeGoals;
    }

    public int awayGoals() {
        return awayGoals;
    }

    public int total() {
        return homeGoals + awayGoals;
    }

    public boolean hasDecreasedFrom(Score previous) {
        return homeGoals < previous.homeGoals || awayGoals < previous.awayGoals;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Score other)) return false;
        return homeGoals == other.homeGoals && awayGoals == other.awayGoals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(homeGoals, awayGoals);
    }

    @Override
    public String toString() {
        return homeGoals + " - " + awayGoals;
    }
}
