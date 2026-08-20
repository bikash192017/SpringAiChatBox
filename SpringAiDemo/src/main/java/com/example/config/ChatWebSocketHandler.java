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

    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(
            ChatService chatService,
            ObjectMapper objectMapper) {

        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) {

        try {

            // =====================================================
            // CONVERT JSON MESSAGE
            // =====================================================

            ChatWebSocketRequest request =
                    objectMapper.readValue(
                            message.getPayload(),
                            ChatWebSocketRequest.class
                    );


            // =====================================================
            // GET CONVERSATION ID
            // =====================================================

            String conversationId =
                    request.getConversationId();


            // =====================================================
            // GET USER PROMPT
            // =====================================================

            String prompt =
                    request.getMessage();


            System.out.println(
                    "================================="
            );

            System.out.println(
                    "CONVERSATION ID: "
                            + conversationId
            );

            System.out.println(
                    "USER PROMPT: "
                            + prompt
            );

            System.out.println(
                    "================================="
            );


            // =====================================================
            // GET LOGGED-IN USER EMAIL
            // =====================================================

            String userEmail =
                    (String) session
                            .getAttributes()
                            .get("userEmail");


            System.out.println(
                    "USER EMAIL: "
                            + userEmail
            );


            // =====================================================
            // STREAM AI RESPONSE
            // =====================================================

            chatService.streamResponse(

                    prompt,

                    userEmail,

                    conversationId,

                    // =================================================
                    // AI CHUNK
                    // =================================================

                    chunk -> {

                        try {

                            if (session.isOpen()) {

                                System.out.println(
                                        "AI CHUNK: "
                                                + chunk
                                );

                                session.sendMessage(
                                        new TextMessage(chunk)
                                );
                            }

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    },


                    // =================================================
                    // COMPLETED
                    // =================================================

                    () -> {

                        try {

                            if (session.isOpen()) {

                                System.out.println(
                                        "AI RESPONSE COMPLETED"
                                );

                                session.sendMessage(
                                        new TextMessage(
                                                "[DONE]"
                                        )
                                );
                            }

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
            );

        } catch (Exception e) {

            System.out.println(
                    "WebSocket message processing error: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}