package com.sportradar.scoreboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ScoreboardTest {

    private MutableClock clock;
    private Scoreboard scoreboard;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-06-01T12:00:00Z"));
        scoreboard = new Scoreboard(new InMemoryMatchRepository(), clock);
    }

    @Nested
    class Summary {

        @Test
        void should_include_a_started_match_in_the_summary() {
            // given / when
            scoreboard.startMatch("Mexico", "Canada");

            // then
            assertThat(scoreboard.getSummary())
                    .extracting(MatchSummary::homeTeam, MatchSummary::awayTeam, MatchSummary::score)
                    .containsExactly(tuple("Mexico", "Canada", Score.initial()));
        }

        @Test
        void should_reflect_score_updates_in_the_summary() {
            // given
            MatchId id = scoreboard.startMatch("Mexico", "Canada");

            // when
            scoreboard.updateScore(id, 0, 5);

            // then
            assertThat(scoreboard.getSummary())
                    .extracting(MatchSummary::score)
                    .containsExactly(Score.of(0, 5));
        }

        @Test
        void should_not_include_a_finished_match_in_the_summary() {
            // given
            MatchId id = scoreboard.startMatch("Mexico", "Canada");

            // when
            scoreboard.finishMatch(id);

            // then
            assertThat(scoreboard.getSummary()).isEmpty();
        }

        @Test
        void should_order_summary_by_total_score_descending() {
            // given
            MatchId low = scoreboard.startMatch("Mexico", "Canada");
            clock.advance(Duration.ofMinutes(1));
            MatchId high = scoreboard.startMatch("Spain", "Brazil");
            scoreboard.updateScore(low, 1, 0);
            scoreboard.updateScore(high, 5, 0);

            // when
            List<MatchSummary> summary = scoreboard.getSummary();

            // then
            assertThat(summary)
                    .extracting(MatchSummary::homeTeam)
                    .containsExactly("Spain", "Mexico");
        }

        @Test
        void should_order_tied_matches_by_most_recently_started_first() {
            // given
            MatchId first = scoreboard.startMatch("Mexico", "Canada");
            clock.advance(Duration.ofMinutes(1));
            MatchId second = scoreboard.startMatch("Spain", "Brazil");
            scoreboard.updateScore(first, 1, 1);
            scoreboard.updateScore(second, 1, 1);

            // when
            List<MatchSummary> summary = scoreboard.getSummary();

            // then
            assertThat(summary)
                    .extracting(MatchSummary::homeTeam)
                    .containsExactly("Spain", "Mexico");
        }

        @Test
        void should_order_matches_exactly_as_in_the_specification_example() {
            // given: matches started in the specified order, one minute apart
            MatchId mexicoCanada = scoreboard.startMatch("Mexico", "Canada");
            clock.advance(Duration.ofMinutes(1));
            MatchId spainBrazil = scoreboard.startMatch("Spain", "Brazil");
            clock.advance(Duration.ofMinutes(1));
            MatchId germanyFrance = scoreboard.startMatch("Germany", "France");
            clock.advance(Duration.ofMinutes(1));
            MatchId uruguayItaly = scoreboard.startMatch("Uruguay", "Italy");
            clock.advance(Duration.ofMinutes(1));
            MatchId argentinaAustralia = scoreboard.startMatch("Argentina", "Australia");

            // when
            scoreboard.updateScore(mexicoCanada, 0, 5);
            scoreboard.updateScore(spainBrazil, 10, 2);
            scoreboard.updateScore(germanyFrance, 2, 2);
            scoreboard.updateScore(uruguayItaly, 6, 6);
            scoreboard.updateScore(argentinaAustralia, 3, 1);

            // then
            assertThat(scoreboard.getSummary())
                    .extracting(MatchSummary::homeTeam, MatchSummary::awayTeam)
                    .containsExactly(
                            tuple("Uruguay", "Italy"),
                            tuple("Spain", "Brazil"),
                            tuple("Mexico", "Canada"),
                            tuple("Argentina", "Australia"),
                            tuple("Germany", "France")
                    );
        }
    }

    @Nested
    class FinishedMatches {

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

    @Nested
    class StartingAMatch {

        @Test
        void should_reject_starting_a_match_for_a_home_team_already_playing() {
            // given
            scoreboard.startMatch("Mexico", "Canada");

            // when / then
            assertThatThrownBy(() -> scoreboard.startMatch("Mexico", "Spain"))
                    .isInstanceOf(DuplicateMatchException.class);
        }

        @Test
        void should_reject_starting_a_match_for_an_away_team_already_playing() {
            // given
            scoreboard.startMatch("Mexico", "Canada");

            // when / then
            assertThatThrownBy(() -> scoreboard.startMatch("Spain", "Canada"))
                    .isInstanceOf(DuplicateMatchException.class);
        }

        @Test
        void should_allow_starting_a_new_match_for_a_team_whose_previous_match_finished() {
            // given
            MatchId id = scoreboard.startMatch("Mexico", "Canada");
            scoreboard.finishMatch(id);

            // when / then: no exception
            scoreboard.startMatch("Mexico", "Spain");
        }
    }

    @Nested
    class UnknownMatch {

        @Test
        void should_reject_updating_the_score_of_an_unknown_match() {
            // given / when / then
            assertThatThrownBy(() -> scoreboard.updateScore(MatchId.newId(), 1, 0))
                    .isInstanceOf(MatchNotFoundException.class);
        }

        @Test
        void should_reject_finishing_an_unknown_match() {
            // given / when / then
            assertThatThrownBy(() -> scoreboard.finishMatch(MatchId.newId()))
                    .isInstanceOf(MatchNotFoundException.class);
        }
    }

    @Nested
    class InvalidOperations {

        @Test
        void should_propagate_invalid_score_updates_through_the_scoreboard() {
            // given
            MatchId id = scoreboard.startMatch("Mexico", "Canada");

            // when / then
            assertThatThrownBy(() -> scoreboard.updateScore(id, -1, 0))
                    .isInstanceOf(InvalidScoreException.class);
        }

        @Test
        void should_propagate_match_already_finished_through_the_scoreboard() {
            // given
            MatchId id = scoreboard.startMatch("Mexico", "Canada");
            scoreboard.finishMatch(id);

            // when / then
            assertThatThrownBy(() -> scoreboard.updateScore(id, 1, 0))
                    .isInstanceOf(MatchAlreadyFinishedException.class);
        }
    }
}
