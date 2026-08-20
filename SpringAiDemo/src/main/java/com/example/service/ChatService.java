package com.example.service;

import com.example.entity.ChatConversation;
import com.example.entity.ChatMessage;
import com.example.repository.ChatConversationRepository;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Service
public class ChatService {

    private final ChatClient chatClient;

    private final ChatConversationRepository
            chatConversationRepository;


    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ChatConversationRepository
                    chatConversationRepository) {

        this.chatClient =
                chatClientBuilder.build();

        this.chatConversationRepository =
                chatConversationRepository;
    }


    // =====================================================
    // NORMAL CHAT
    // =====================================================

    public String getResponse(String prompt) {

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }


    // =====================================================
    // CREATE NEW CONVERSATION
    // =====================================================

    public ChatConversation createConversation(
            String userEmail,
            String title) {

        ChatConversation conversation =
                new ChatConversation();

        conversation.setUserEmail(
                userEmail
        );

        conversation.setTitle(
                title
        );

        conversation.setCreatedAt(
                LocalDateTime.now()
        );

        conversation.setUpdatedAt(
                LocalDateTime.now()
        );

        return chatConversationRepository.save(
                conversation
        );
    }


    // =====================================================
    // GET USER CONVERSATIONS
    // =====================================================

    public List<ChatConversation>
    getUserConversations(
            String userEmail) {

        return chatConversationRepository
                .findByUserEmailOrderByUpdatedAtDesc(
                        userEmail
                );
    }


    // =====================================================
    // STREAM RESPONSE
    // =====================================================

    public void streamResponse(
            String prompt,
            String userEmail,
            String conversationId,
            Consumer<String> onChunk,
            Runnable onComplete) {


        // =================================================
        // FIND EXISTING CONVERSATION
        // =================================================

        ChatConversation conversation =
                chatConversationRepository
                        .findById(conversationId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Conversation not found"
                                )
                        );


        // =================================================
        // SECURITY CHECK
        // =================================================

        if (!conversation
                .getUserEmail()
                .equals(userEmail)) {

            throw new RuntimeException(
                    "You are not allowed to access this conversation"
            );
        }


        // =================================================
        // SAVE USER MESSAGE
        // =================================================

        conversation
                .getMessages()
                .add(
                        new ChatMessage(
                                "user",
                                prompt
                        )
                );


        conversation.setUpdatedAt(
                LocalDateTime.now()
        );


        chatConversationRepository.save(
                conversation
        );


        // =================================================
        // STORE AI RESPONSE
        // =================================================

        StringBuilder aiResponse =
                new StringBuilder();


        // =================================================
        // CALL GEMINI
        // =================================================

        chatClient
                .prompt()
                .user(prompt)
                .stream()
                .content()
                .subscribe(

                        // =================================
                        // CHUNK
                        // =================================

                        chunk -> {

                            System.out.println(
                                    "AI CHUNK: "
                                            + chunk
                            );

                            aiResponse.append(
                                    chunk
                            );

                            onChunk.accept(
                                    chunk
                            );
                        },


                        // =================================
                        // ERROR
                        // =================================

                        error -> {

                            System.out.println(
                                    "AI ERROR: "
                                            + error.getMessage()
                            );

                            error.printStackTrace();
                        },


                        // =================================
                        // COMPLETE
                        // =================================

                        () -> {

                            System.out.println(
                                    "AI STREAM COMPLETED"
                            );


                            // =============================
                            // SAVE AI MESSAGE
                            // =============================

                            conversation
                                    .getMessages()
                                    .add(
                                            new ChatMessage(
                                                    "assistant",
                                                    aiResponse.toString()
                                            )
                                    );


                            conversation.setUpdatedAt(
                                    LocalDateTime.now()
                            );


                            // =============================
                            // SAVE CONVERSATION
                            // =============================

                            chatConversationRepository
                                    .save(
                                            conversation
                                    );


                            // =============================
                            // INFORM WEBSOCKET
                            // =============================

                            onComplete.run();
                        }
                );
    }
}