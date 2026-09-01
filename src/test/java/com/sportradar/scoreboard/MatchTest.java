package com.sportradar.scoreboard;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchTest {

    private final Instant now = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void should_assign_a_unique_id_to_each_match() {
        // given / when
        Match first = Match.start("Mexico", "Canada", now);
        Match second = Match.start("Spain", "Brazil", now);

        // then
        assertThat(first.id()).isNotNull();
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void should_start_match_in_progress_with_zero_score() {
        // given / when
        Match match = Match.start("Mexico", "Canada", now);

        // then
        assertThat(match.status()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(match.score()).isEqualTo(Score.initial());
        assertThat(match.homeTeam()).isEqualTo("Mexico");
        assertThat(match.awayTeam()).isEqualTo("Canada");
        assertThat(match.startTime()).isEqualTo(now);
    }

    @Test
    void should_update_score_of_a_match_in_progress() {
        // given
        Match match = Match.start("Mexico", "Canada", now);

        // when
        match.updateScore(Score.of(0, 5));

        // then
        assertThat(match.score()).isEqualTo(Score.of(0, 5));
    }

    @Test
    void should_reject_score_update_once_match_is_finished() {
        // given
        Match match = Match.start("Mexico", "Canada", now);
        match.finish(now.plusSeconds(60));

        // when / then
        assertThatThrownBy(() -> match.updateScore(Score.of(1, 0)))
                .isInstanceOf(MatchAlreadyFinishedException.class);
    }

    @Test
    void should_reject_score_that_decreases_goals() {
        // given
        Match match = Match.start("Mexico", "Canada", now);
        match.updateScore(Score.of(2, 2));

        // when / then
        assertThatThrownBy(() -> match.updateScore(Score.of(1, 2)))
                .isInstanceOf(InvalidScoreException.class)
                .hasMessageContaining("decrease");
    }

    @Test
    void should_finish_a_match_in_progress() {
        // given
        Match match = Match.start("Mexico", "Canada", now);
        Instant finishTime = now.plusSeconds(90 * 60);

        // when
        match.finish(finishTime);

        // then
        assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(match.finishTime()).isEqualTo(finishTime);
    }

    @Test
    void should_reject_finishing_an_already_finished_match() {
        // given
        Match match = Match.start("Mexico", "Canada", now);
        match.finish(now.plusSeconds(60));

        // when / then
        assertThatThrownBy(() -> match.finish(now.plusSeconds(120)))
                .isInstanceOf(MatchAlreadyFinishedException.class);
    }

    @Test
    void should_reject_blank_team_names() {
        // given / when / then
        assertThatThrownBy(() -> Match.start("  ", "Canada", now))
                .isInstanceOf(InvalidTeamException.class);
        assertThatThrownBy(() -> Match.start("Mexico", "  ", now))
                .isInstanceOf(InvalidTeamException.class);
    }

    @Test
    void should_reject_a_team_playing_against_itself() {
        // given / when / then
        assertThatThrownBy(() -> Match.start("Mexico", "Mexico", now))
                .isInstanceOf(InvalidTeamException.class);
    }
}
