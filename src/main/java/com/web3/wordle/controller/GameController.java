package com.web3.wordle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.wordle.dto.MessageDTO;
import com.web3.wordle.service.GameService;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Controller
public class GameController extends TextWebSocketHandler {

    private final GameService gameService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Jogador conectado: " + session.getId());
        gameService.connectPlayer(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("Mensagem recebida de " + session.getId() + ": " + message.getPayload());
        try {
            MessageDTO messageDTO = objectMapper.readValue(message.getPayload(), MessageDTO.class);
            gameService.processAction(session, messageDTO);
        } catch (Exception e) {
            System.out.println("Erro ao processar mensagem: " + e.getMessage());
            session.sendMessage(new TextMessage("{\"status\":\"error\", \"message\":\"Erro ao processar a mensagem.\"}"));
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Desconecta o jogador ao fechar a conexão
        System.out.println("Sessão fechada: " + session.getId() + " com status: " + status);
        gameService.disconnectPlayer(session);
    }

}
