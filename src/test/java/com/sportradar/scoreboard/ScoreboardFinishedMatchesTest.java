package com.sportradar.scoreboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ScoreboardFinishedMatchesTest {

    private MutableClock clock;
    private Scoreboard scoreboard;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-06-01T12:00:00Z"));
        scoreboard = new Scoreboard(new InMemoryMatchRepository(), clock);
    }

    @Test
    void should_include_a_finished_match_in_the_history() {
        // given
        MatchId id = scoreboard.startMatch("Mexico", "Canada");
        scoreboard.updateScore(id, 0, 5);

        // when
        scoreboard.finishMatch(id);

        // then
        assertThat(scoreboard.getFinishedMatches())
                .extracting(MatchSummary::homeTeam, MatchSummary::awayTeam, MatchSummary::score)
                .containsExactly(tuple("Mexico", "Canada", Score.of(0, 5)));
    }

    @Test
    void should_not_include_a_match_still_in_progress_in_the_history() {
        // given
        scoreboard.startMatch("Mexico", "Canada");

        // when / then
        assertThat(scoreboard.getFinishedMatches()).isEmpty();
    }

    @Test
    void should_order_finished_matches_by_most_recently_finished_first() {
        // given
        MatchId first = scoreboard.startMatch("Mexico", "Canada");
        MatchId second = scoreboard.startMatch("Spain", "Brazil");

        // when
        scoreboard.finishMatch(first);
        clock.advance(Duration.ofMinutes(1));
        scoreboard.finishMatch(second);

        // then
        List<MatchSummary> history = scoreboard.getFinishedMatches();
        assertThat(history)
                .extracting(MatchSummary::homeTeam)
                .containsExactly("Spain", "Mexico");
    }
}
