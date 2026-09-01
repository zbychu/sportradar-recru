package com.sportradar.scoreboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryMatchRepository implements MatchRepository {

    private final Map<MatchId, Match> matches = new LinkedHashMap<>();

    @Override
    public void save(Match match) {
        matches.put(match.id(), match);
    }

    @Override
    public Optional<Match> findById(MatchId id) {
        return Optional.ofNullable(matches.get(id));
    }

    @Override
    public List<Match> findAll() {
        return new ArrayList<>(matches.values());
    }
}
