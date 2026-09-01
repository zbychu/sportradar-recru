package com.sportradar.scoreboard;

import java.time.Instant;

public record MatchSummary(MatchId id, String homeTeam, String awayTeam, int homeGoals, int awayGoals,
                            Instant startTime) {

    static MatchSummary from(Match match) {
        return new MatchSummary(
                match.id(),
                match.homeTeam(),
                match.awayTeam(),
                match.score().homeGoals(),
                match.score().awayGoals(),
                match.startTime()
        );
    }
}
