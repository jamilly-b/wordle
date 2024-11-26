package com.web3.wordle.model;

public class Player {
    private String sessionId;
    private String word;
    private boolean hasWon = false;

    public Player(String sessionId, String word) {
        this.sessionId = sessionId;
        this.word = word;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getWord() {
        return word;
    }

    public boolean hasWon() {
        return hasWon;
    }

    public void setWon(boolean hasWon) {
        this.hasWon = hasWon;
    }

    public void setWord(String word) {
        this.word = word;
    }
}
