package com.sportradar.scoreboard;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMatchRepositoryTest {

    private final Instant now = Instant.parse("2026-06-01T12:00:00Z");
    private final MatchRepository repository = new InMemoryMatchRepository();

    @Test
    void should_find_a_saved_match_by_id() {
        // given
        Match match = Match.start("Mexico", "Canada", now);
        repository.save(match);

        // when
        Optional<Match> found = repository.findById(match.id());

        // then
        assertThat(found).contains(match);
    }

    @Test
    void should_return_empty_when_match_id_is_unknown() {
        // given / when
        Optional<Match> found = repository.findById(MatchId.newId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void should_return_all_saved_matches() {
        // given
        Match first = Match.start("Mexico", "Canada", now);
        Match second = Match.start("Spain", "Brazil", now);
        repository.save(first);
        repository.save(second);

        // when
        List<Match> all = repository.findAll();

        // then
        assertThat(all).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void should_return_empty_list_when_no_matches_saved() {
        // given / when
        List<Match> all = repository.findAll();

        // then
        assertThat(all).isEmpty();
    }
}
