package com.web3.wordle.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
    private Map<String, Player> players = new HashMap<>();
    private Map<Player, String> guesses = new HashMap<>();
    private String targetWord;
    private String currentTurn;
    private boolean gameOver = false;

    // Adiciona um jogador ao jogo
    public boolean addPlayer(String sessionId, String word) {
        if (players.size() < 2) {
            players.put(sessionId, new Player(sessionId, word));
            if (players.size() == 2) {
                targetWord = word; // Define a palavra do segundo jogador como a palavra-alvo
                currentTurn = sessionId; // Define o segundo jogador como o primeiro a jogar
            }
            return true;
        }
        return false;
    }

    // Obtém o jogador com base no ID da sessão
    public Player getPlayer(String sessionId) {
        return players.get(sessionId);
    }

    // Obtém o oponente do jogador
    public Player getOpponent(String sessionId) {
        return players.values().stream()
                .filter(player -> !player.getSessionId().equals(sessionId))
                .findFirst()
                .orElse(null);
    }

    // Verifica se o jogo está cheio
    public boolean isFull() {
        return players.size() == 2;
    }

    // Verifica se o jogo acabou
    public boolean isGameOver() {
        return gameOver;
    }

    // Define o estado do jogo como encerrado
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    // Verifica se o jogador pode jogar
    public boolean canPlayerPlay(Player player) {
        return player.getSessionId().equals(currentTurn);
    }

    // Passa a vez para o próximo jogador
    public void nextTurn() {
        currentTurn = players.values().stream()
                .filter(player -> !player.getSessionId().equals(currentTurn))
                .findFirst()
                .map(Player::getSessionId)
                .orElse(null);
    }

    // Valida se a palavra tem o mesmo comprimento da palavra-alvo
    private boolean isValidWord(String word) {
        return word != null && word.length() == targetWord.length();
    }

    // Submete uma tentativa do jogador
    public boolean submitGuess(Player player, String word) {
        if (!isValidWord(word)) {
            return false; // Palavra inválida
        }

        guesses.put(player, word);

        // Verifica se a palavra é a correta
        return targetWord.equalsIgnoreCase(word);
    }

    // Gera o feedback da palavra
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

    public String getFeedbackForWord(String word) {
        List<WordCheckResult> feedback = checkWord(word, targetWord);
        StringBuilder result = new StringBuilder();
        for (WordCheckResult item : feedback) {
            switch (item.getStatus()) {
                case "correct":
                    result.append("🟩");
                    break;
                case "misplaced":
                    result.append("🟨");
                    break;
                case "absent":
                    result.append("⬜");
                    break;
            }
        }
        return result.toString();
    }
}
