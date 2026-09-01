package com.sportradar.scoreboard;

public class MatchAlreadyFinishedException extends RuntimeException {

    public MatchAlreadyFinishedException(String message) {
        super(message);
    }
}
