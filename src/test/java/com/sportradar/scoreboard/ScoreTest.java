package com.sportradar.scoreboard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreTest {

    @Test
    void should_create_score_with_given_goals() {
        // given
        int homeGoals = 2;
        int awayGoals = 3;

        // when
        Score score = Score.of(homeGoals, awayGoals);

        // then
        assertThat(score.homeGoals()).isEqualTo(2);
        assertThat(score.awayGoals()).isEqualTo(3);
    }

    @Test
    void should_start_at_zero_zero() {
        // given / when
        Score score = Score.initial();

        // then
        assertThat(score.homeGoals()).isZero();
        assertThat(score.awayGoals()).isZero();
    }

    @Test
    void should_calculate_total_goals() {
        // given
        Score score = Score.of(2, 3);

        // when
        int total = score.total();

        // then
        assertThat(total).isEqualTo(5);
    }

    @Test
    void should_reject_negative_home_goals() {
        // given / when / then
        assertThatThrownBy(() -> Score.of(-1, 0))
                .isInstanceOf(InvalidScoreException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void should_reject_negative_away_goals() {
        // given / when / then
        assertThatThrownBy(() -> Score.of(0, -1))
                .isInstanceOf(InvalidScoreException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void should_report_no_decrease_when_goals_increase_or_stay_the_same() {
        // given
        Score previous = Score.of(2, 2);

        // when / then
        assertThat(Score.of(3, 2).hasDecreasedFrom(previous)).isFalse();
        assertThat(Score.of(2, 2).hasDecreasedFrom(previous)).isFalse();
    }

    @Test
    void should_report_a_decrease_when_either_side_loses_goals() {
        // given
        Score previous = Score.of(2, 2);

        // when / then
        assertThat(Score.of(1, 2).hasDecreasedFrom(previous)).isTrue();
        assertThat(Score.of(2, 1).hasDecreasedFrom(previous)).isTrue();
    }

    @Test
    void should_consider_scores_with_same_goals_equal() {
        // given
        Score first = Score.of(2, 3);
        Score second = Score.of(2, 3);

        // when / then
        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }
}
