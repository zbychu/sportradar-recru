package com.sportradar.scoreboard;

import java.time.Instant;

public final class Match {

    private final MatchId id;
    private final String homeTeam;
    private final String awayTeam;
    private final Instant startTime;
    private Score score;
    private MatchStatus status;
    private Instant finishTime;

    private Match(String homeTeam, String awayTeam, Instant startTime) {
        this.id = MatchId.newId();
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.startTime = startTime;
        this.score = Score.initial();
        this.status = MatchStatus.IN_PROGRESS;
    }

    public static Match start(String homeTeam, String awayTeam, Instant startTime) {
        requireNonBlank(homeTeam, "Home team");
        requireNonBlank(awayTeam, "Away team");
        if (homeTeam.equals(awayTeam)) {
            throw new InvalidTeamException("A team cannot play against itself: " + homeTeam);
        }
        return new Match(homeTeam, awayTeam, startTime);
    }

    private static void requireNonBlank(String team, String label) {
        if (team == null || team.isBlank()) {
            throw new InvalidTeamException(label + " name must not be blank");
        }
    }

    public void updateScore(Score newScore) {
        requireInProgress();
        if (newScore.hasDecreasedFrom(score)) {
            throw new InvalidScoreException("Score cannot decrease: " + score + " -> " + newScore);
        }
        this.score = newScore;
    }

    public void finish(Instant finishTime) {
        requireInProgress();
        this.status = MatchStatus.FINISHED;
        this.finishTime = finishTime;
    }

    private void requireInProgress() {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new MatchAlreadyFinishedException("Match " + id + " is already finished");
        }
    }

    public MatchId id() {
        return id;
    }

    public String homeTeam() {
        return homeTeam;
    }

    public String awayTeam() {
        return awayTeam;
    }

    public Instant startTime() {
        return startTime;
    }

    public Score score() {
        return score;
    }

    public MatchStatus status() {
        return status;
    }

    public Instant finishTime() {
        return finishTime;
    }
}
