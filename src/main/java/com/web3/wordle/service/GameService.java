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

    private final Game game = new Game();
    private final Map<String, WebSocketSession> sessions = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Conecta um jogador ao jogo
    public void connectPlayer(WebSocketSession session) throws Exception {
        if (sessions.size() < 2) {
            sessions.put(session.getId(), session);
            String placeholderWord = "guess"; // Palavra padrão até que o jogador configure a sua
            boolean playerAdded = game.addPlayer(session.getId(), placeholderWord);

            if (playerAdded) {
                session.sendMessage(new TextMessage("{\"status\":\"waiting\", \"message\":\"Aguardando outro jogador...\"}"));
            } else {
                session.sendMessage(new TextMessage("{\"status\":\"error\", \"message\":\"Erro ao adicionar jogador.\"}"));
                session.close();
            }

            if (sessions.size() == 2) {
                // Notifica todos os jogadores que o jogo pode começar
                for (WebSocketSession playerSession : sessions.values()) {
                    playerSession.sendMessage(new TextMessage("{\"status\":\"start\", \"message\":\"O jogo começou! Configure sua palavra.\"}"));
                }
            }

        } else {
            // Fecha a conexão se a sala estiver cheia
            session.sendMessage(new TextMessage("{\"status\":\"error\", \"message\":\"Sala cheia.\"}"));
            session.close();
        }

    }

    // Desconecta um jogador
    public void disconnectPlayer(WebSocketSession session) throws Exception {
        Player opponent = game.getOpponent(session.getId());
        sessions.remove(session.getId());

        if (opponent != null) {
            WebSocketSession opponentSession = sessions.get(opponent.getSessionId());
            if (opponentSession != null) {
                opponentSession.sendMessage(new TextMessage("{\"status\":\"win\", \"message\":\"O outro jogador saiu. Você venceu!\"}"));
                opponentSession.close();
            }
        }

        session.close();

        // Finaliza o jogo se não houver mais jogadores
        if (sessions.isEmpty()) {
            closeGame();
        }
    }

    public void processAction(WebSocketSession session, MessageDTO message) throws Exception {

        System.out.println("Processando ação de " + session.getId() + ": " + message);

        if (game.isGameOver()) {
            System.out.println("Jogo já encerrado. Ignorando ação.");
            session.sendMessage(new TextMessage( "{\"status\":\"error\", \"message\":\"O jogo já foi encerrado.\"}"));
            return;
        }

        Player player = game.getPlayer(session.getId());
        Player opponent = game.getOpponent(session.getId());

        if (player == null) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage("{\"status\":\"error\", \"message\":\"Jogador não encontrado.\"}"));
            }
            return;
        }

        if ("guess".equals(message.getType())) {
            String guessedWord = message.getWord();
            List<WordCheckResult> result = game.checkWord(guessedWord, opponent.getWord());

            // Envia o resultado ao jogador atual
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
            }

            // Verifica vitória
            if (opponent.getWord().equals(guessedWord)) {
                player.setWon(true);
                game.setGameOver(true);

                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("{\"status\":\"win\", \"message\":\"Você venceu!\"}"));
                }

                WebSocketSession opponentSession = sessions.get(opponent.getSessionId());
                if (opponentSession != null && opponentSession.isOpen()) {
                    opponentSession.sendMessage(new TextMessage("{\"status\":\"lose\", \"message\":\"Você perdeu!\"}"));
                }

                closeGame();
            }
        }
    }

    private void closeGame() {
        System.out.println("Encerrando o jogo...");
        game.setGameOver(true);

        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                System.out.println("Encerrando sessão: " + session.getId());
                try {
                    session.sendMessage(new TextMessage("{\"status\":\"game_over\", \"message\":\"O jogo foi encerrado.\"}"));
                    session.close();
                } catch (IOException e) {
                    System.out.println("Erro ao encerrar sessão " + session.getId() + ": " + e.getMessage());
                }
            }
        }
        sessions.clear();
        System.out.println("Todas as sessões foram encerradas.");
    }
}