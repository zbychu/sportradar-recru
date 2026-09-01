package com.sportradar.scoreboard;

import java.time.Clock;
import java.util.List;

public final class Scoreboard {

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
                .filter(Match::isInProgress)
                .anyMatch(match -> match.involves(homeTeam) || match.involves(awayTeam));
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
                .filter(Match::isInProgress)
                .sorted(Match.IN_PROGRESS_ORDER)
                .map(Match::toSummary)
                .toList();
    }

    public List<MatchSummary> getFinishedMatches() {
        return repository.findAll().stream()
                .filter(Match::isFinished)
                .sorted(Match.FINISHED_ORDER)
                .map(Match::toSummary)
                .toList();
    }
}
