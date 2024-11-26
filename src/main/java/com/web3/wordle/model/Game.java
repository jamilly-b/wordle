package com.web3.wordle.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
    private Map<String, Player> players = new HashMap<>();
    private boolean gameOver = false;

    public boolean addPlayer(String sessionId, String word) {
        if (players.size() < 2) {
            players.put(sessionId, new Player(sessionId, word));
            return true;
        }
        return false;
    }

    public Player getPlayer(String sessionId) {
        return players.get(sessionId);
    }

    public Player getOpponent(String sessionId) {
        return players.values().stream()
                .filter(player -> !player.getSessionId().equals(sessionId))
                .findFirst()
                .orElse(null);
    }

    public boolean isFull() {
        return players.size() == 2;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public List<WordCheckResult> checkWord(String guessedWord, String reservedWord) {
        List<WordCheckResult> result = new ArrayList<>();
        for (int i = 0; i < guessedWord.length(); i++) {
            char guessedLetter = guessedWord.charAt(i);
            if (reservedWord.charAt(i) == guessedLetter) {
                result.add(new WordCheckResult(guessedLetter, "correct"));
            } else if (reservedWord.indexOf(guessedLetter) != -1) {
                result.add(new WordCheckResult(guessedLetter, "misplaced"));
            } else {
                result.add(new WordCheckResult(guessedLetter, "absent"));
            }
        }
        return result;
    }
}