package com.sportradar.scoreboard;

import java.time.Instant;
import java.util.Comparator;

public final class Match {

    public static final Comparator<Match> IN_PROGRESS_ORDER =
            Comparator.comparingInt((Match match) -> match.score.total())
                    .reversed()
                    .thenComparing((Match match) -> match.startTime, Comparator.reverseOrder());

    public static final Comparator<Match> FINISHED_ORDER =
            Comparator.comparing((Match match) -> match.finishTime, Comparator.reverseOrder());

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

    public boolean isInProgress() {
        return status == MatchStatus.IN_PROGRESS;
    }

    public boolean isFinished() {
        return status == MatchStatus.FINISHED;
    }

    public boolean involves(String team) {
        return homeTeam.equals(team) || awayTeam.equals(team);
    }

    public MatchSummary toSummary() {
        return new MatchSummary(id, homeTeam, awayTeam, score, startTime, finishTime);
    }
}
