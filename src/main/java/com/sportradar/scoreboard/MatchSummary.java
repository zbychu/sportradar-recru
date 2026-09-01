package com.sportradar.scoreboard;

import java.time.Instant;

public record MatchSummary(MatchId id, String homeTeam, String awayTeam, Score score, Instant startTime,
                            Instant finishTime) {
}
