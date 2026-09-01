package com.sportradar.scoreboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreboardEdgeCasesTest {

    private Scoreboard scoreboard;

    @BeforeEach
    void setUp() {
        scoreboard = new Scoreboard(new InMemoryMatchRepository(), new MutableClock(Instant.parse("2026-06-01T12:00:00Z")));
    }

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
