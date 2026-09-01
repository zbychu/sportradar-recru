package com.sportradar.scoreboard;

import java.util.List;
import java.util.Optional;

public interface MatchRepository {

    void save(Match match);

    Optional<Match> findById(MatchId id);

    List<Match> findAll();
}
