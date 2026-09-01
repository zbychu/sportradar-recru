package com.sportradar.scoreboard;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;

public final class Scoreboard {

    private static final Comparator<Match> SUMMARY_ORDER =
            Comparator.comparingInt((Match match) -> match.score().total())
                    .reversed()
                    .thenComparing(Match::startTime, Comparator.reverseOrder());

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
        Match match = Match.start(homeTeam, awayTeam, clock.instant());
        repository.save(match);
        return match.id();
    }

    public void updateScore(MatchId id, int homeGoals, int awayGoals) {
        Match match = repository.findById(id).orElseThrow();
        match.updateScore(Score.of(homeGoals, awayGoals));
        repository.save(match);
    }

    public void finishMatch(MatchId id) {
        Match match = repository.findById(id).orElseThrow();
        match.finish(clock.instant());
        repository.save(match);
    }

    public List<MatchSummary> getSummary() {
        return repository.findAll().stream()
                .filter(match -> match.status() == MatchStatus.IN_PROGRESS)
                .sorted(SUMMARY_ORDER)
                .map(MatchSummary::from)
                .toList();
    }
}
