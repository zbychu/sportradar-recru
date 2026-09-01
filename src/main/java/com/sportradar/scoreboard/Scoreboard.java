package com.sportradar.scoreboard;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;

public final class Scoreboard {

    private static final Comparator<Match> SUMMARY_ORDER =
            Comparator.comparingInt((Match match) -> match.score().total())
                    .reversed()
                    .thenComparing(Match::startTime, Comparator.reverseOrder());

    private static final Comparator<Match> FINISHED_ORDER =
            Comparator.comparing(Match::finishTime, Comparator.reverseOrder());

    private final MatchRepository repository;
    private final Clock clock;

    public Scoreboard(MatchRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Scoreboard() {
        this(new InMemoryMatchRepository(), Clock.systemUTC());
    }

    public MatchId startMatch(String homeTeam, String awayTeam) {
        requireNeitherTeamIsPlaying(homeTeam, awayTeam);
        Match match = Match.start(homeTeam, awayTeam, clock.instant());
        repository.save(match);
        return match.id();
    }

    public void updateScore(MatchId id, int homeGoals, int awayGoals) {
        Match match = findMatchOrThrow(id);
        match.updateScore(Score.of(homeGoals, awayGoals));
        repository.save(match);
    }

    public void finishMatch(MatchId id) {
        Match match = findMatchOrThrow(id);
        match.finish(clock.instant());
        repository.save(match);
    }

    private void requireNeitherTeamIsPlaying(String homeTeam, String awayTeam) {
        boolean alreadyPlaying = repository.findAll().stream()
                .filter(match -> match.status() == MatchStatus.IN_PROGRESS)
                .anyMatch(match -> match.homeTeam().equals(homeTeam) || match.awayTeam().equals(homeTeam)
                        || match.homeTeam().equals(awayTeam) || match.awayTeam().equals(awayTeam));
        if (alreadyPlaying) {
            throw new DuplicateMatchException(
                    "A match involving " + homeTeam + " or " + awayTeam + " is already in progress");
        }
    }

    private Match findMatchOrThrow(MatchId id) {
        return repository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException("No match found with id " + id));
    }

    public List<MatchSummary> getSummary() {
        return repository.findAll().stream()
                .filter(match -> match.status() == MatchStatus.IN_PROGRESS)
                .sorted(SUMMARY_ORDER)
                .map(MatchSummary::from)
                .toList();
    }

    public List<MatchSummary> getFinishedMatches() {
        return repository.findAll().stream()
                .filter(match -> match.status() == MatchStatus.FINISHED)
                .sorted(FINISHED_ORDER)
                .map(MatchSummary::from)
                .toList();
    }
}
