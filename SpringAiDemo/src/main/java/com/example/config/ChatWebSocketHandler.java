package com.example.config;

import com.example.dto.ChatWebSocketRequest;
import com.example.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatWebSocketHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ChatWebSocketRequest request =
                    objectMapper.readValue(
                            message.getPayload(),
                            ChatWebSocketRequest.class
                    );

            String conversationId = request.getConversationId();
            String prompt = request.getMessage();
            String userEmail = (String) session.getAttributes().get("userEmail");

            System.out.println("=================================");
            System.out.println("CONVERSATION ID: " + conversationId);
            System.out.println("USER PROMPT: " + prompt);
            System.out.println("USER EMAIL: " + userEmail);
            System.out.println("=================================");

            chatService.streamResponse(
                    prompt,
                    userEmail,
                    conversationId,

                    // On Next Chunk
                    chunk -> {
                        try {
                            if (session.isOpen()) {
                                synchronized (session) {
                                    session.sendMessage(new TextMessage(chunk));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error writing WS chunk: " + e.getMessage());
                        }
                    },

                    // On Complete
                    () -> {
                        try {
                            if (session.isOpen()) {
                                synchronized (session) {
                                    session.sendMessage(new TextMessage("[DONE]"));
                                }
                                System.out.println("AI RESPONSE COMPLETED");
                            }
                        } catch (Exception e) {
                            System.err.println("Error writing [DONE]: " + e.getMessage());
                        }
                    }
            );

        } catch (Exception e) {
            System.err.println("WebSocket message processing error: " + e.getMessage());
            e.printStackTrace();

            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage("Error processing message: " + e.getMessage()));
                        session.sendMessage(new TextMessage("[DONE]"));
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}