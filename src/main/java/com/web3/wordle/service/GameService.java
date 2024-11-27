package com.web3.wordle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.wordle.dto.MessageDTO;
import com.web3.wordle.model.Game;
import com.web3.wordle.model.Player;
import com.web3.wordle.model.WordCheckResult;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameService {
    private Game game = new Game();
    private final Map<String, WebSocketSession> sessions = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int playerReadyCount = 0;

    public void connectPlayer(WebSocketSession session) throws Exception {
        if (sessions.size() < 2) {
            sessions.put(session.getId(), session);
            game.addPlayer(session.getId(), "");

            session.sendMessage(new TextMessage(
                    jsonMessage("waiting", "Waiting for another player...")
            ));

            if (sessions.size() == 2) {
                broadcastMessage("start", "Game is ready. Set your words!");
            }
        } else {
            session.sendMessage(new TextMessage(
                    jsonMessage("error", "Game room is full")
            ));
            session.close();
        }
    }

    public void processAction(WebSocketSession session, MessageDTO message) throws Exception {
        if (game.isGameOver()) {
            session.sendMessage(new TextMessage(
                    jsonMessage("error", "Game is already over")
            ));
            return;
        }

        if (message.getType().equals("set_word")) {
            handleWordSetting(session, message.getWord());
        } else if (message.getType().equals("guess")) {
            handleGuess(session, message.getWord());
        }
    }

    private void handleWordSetting(WebSocketSession session, String word) throws Exception {
        if (word.length() != 5) {
            session.sendMessage(new TextMessage(
                    jsonMessage("error", "Word must be 5 characters long")
            ));
            return;
        }

        Player player = game.getPlayer(session.getId());
        player.setWord(word);
        playerReadyCount++;

        if (playerReadyCount == 2) {
            broadcastMessage("game_ready", "Both players are ready. Start guessing!");
        }
    }

    private void handleGuess(WebSocketSession session, String guessedWord) throws Exception {
        Player currentPlayer = game.getPlayer(session.getId());
        Player opponentPlayer = game.getOpponent(session.getId());

        List<WordCheckResult> result = game.checkWord(guessedWord, opponentPlayer.getWord());
        
        session.sendMessage(new TextMessage(
                String.format("{\"status\":\"guess_result\", \"message\":%s}",
                        objectMapper.writeValueAsString(result))
        ));

        WebSocketSession opponentSession = sessions.get(opponentPlayer.getSessionId());
        if (opponentSession != null) {
            opponentSession.sendMessage(new TextMessage(
                    String.format("{\"status\":\"opponent_guess\", \"message\":%s}",
                            objectMapper.writeValueAsString(result))
            ));
        }

        if (opponentPlayer.getWord().equalsIgnoreCase(guessedWord)) {
            handleGameEnd(currentPlayer, opponentPlayer);
        }
    }



    private void handleGameEnd(Player winner, Player loser) throws Exception {
        game.setGameOver(true);

        WebSocketSession winnerSession = sessions.get(winner.getSessionId());
        WebSocketSession loserSession = sessions.get(loser.getSessionId());

        if (winnerSession != null) {
            winnerSession.sendMessage(new TextMessage(
                    jsonMessage("win", "Congratulations! You won the game!")
            ));
        }

        if (loserSession != null) {
            loserSession.sendMessage(new TextMessage(
                    jsonMessage("lose", "Sorry, you lost the game.")
            ));
        }

        closeGame();
    }

    public void disconnectPlayer(WebSocketSession session) throws Exception {
        if (!sessions.containsKey(session.getId())) {
            return;
        }

        Player opponent = game.getOpponent(session.getId());
        sessions.remove(session.getId());

        if (opponent != null) {
            WebSocketSession opponentSession = sessions.get(opponent.getSessionId());
            if (opponentSession != null) {
                opponentSession.sendMessage(new TextMessage(
                        jsonMessage("opponent_left", "Your opponent left. You won!")
                ));
                opponentSession.close();
            }
        }

        session.close();
        closeGame();
    }

    private void closeGame() {
        game = new Game();
        sessions.clear();
        playerReadyCount = 0;
    }

    private void broadcastMessage(String status, Object message) throws IOException {
        String messageJson;
        if (message instanceof String) {
            messageJson = String.format("\"%s\"", ((String) message).replace("\"", "\\\""));
        } else {
            messageJson = objectMapper.writeValueAsString(message);
        }

        String finalMessage = String.format("{\"status\":\"%s\", \"message\":%s}", status, messageJson);

        for (WebSocketSession session : sessions.values()) {
            session.sendMessage(new TextMessage(finalMessage));
        }
    }


    private String jsonMessage(String status, String message) {
        return String.format("{\"status\":\"%s\", \"message\":\"%s\"}", status, message);
    }
}