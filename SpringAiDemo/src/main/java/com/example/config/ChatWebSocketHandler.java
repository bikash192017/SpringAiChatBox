package com.example.config;

import com.example.service.ChatService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler
        extends TextWebSocketHandler {

    private final ChatService chatService;

    public ChatWebSocketHandler(
            ChatService chatService) {

        this.chatService = chatService;
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) {

        UserDetails user =
                (UserDetails) session
                        .getAttributes()
                        .get("user");

        if (user == null) {

            System.out.println(
                    "Unauthorized WebSocket message"
            );

            return;
        }

        String email = user.getUsername();

        String prompt = message.getPayload();

        System.out.println(
                "User: " + email
        );

        System.out.println(
                "Received prompt: " + prompt
        );

        chatService.streamResponse(

                prompt,

                chunk -> {

                    try {

                        if (session.isOpen()) {

                            System.out.println(
                                    "SENDING CHUNK TO UI: "
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

                () -> {

                    try {

                        if (session.isOpen()) {

                            System.out.println(
                                    "SENDING DONE TO UI"
                            );

                            session.sendMessage(
                                    new TextMessage("[DONE]")
                            );
                        }

                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                }
        );
    }
}