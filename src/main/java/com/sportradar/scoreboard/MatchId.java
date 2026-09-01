package com.sportradar.scoreboard;

import java.util.Objects;
import java.util.UUID;

public final class MatchId {

    private final UUID value;

    private MatchId(UUID value) {
        this.value = value;
    }

    public static MatchId newId() {
        return new MatchId(UUID.randomUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
