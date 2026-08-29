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
    private final ChatConversationRepository chatConversationRepository;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ChatConversationRepository chatConversationRepository) {

        this.chatClient = chatClientBuilder.build();
        this.chatConversationRepository = chatConversationRepository;
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

        ChatConversation conversation = new ChatConversation();
        conversation.setUserEmail(userEmail != null ? userEmail.trim().toLowerCase() : null);
        conversation.setTitle(title != null ? title : "New Chat");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        return chatConversationRepository.save(conversation);
    }

    // =====================================================
    // GET USER CONVERSATIONS
    // =====================================================

    public List<ChatConversation> getUserConversations(String userEmail) {
        String normalizedEmail = userEmail != null ? userEmail.trim().toLowerCase() : "";
        return chatConversationRepository.findByUserEmailOrderByUpdatedAtDesc(normalizedEmail);
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

        ChatConversation conversation = chatConversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // =================================================
        // SECURITY CHECK (Case-insensitive & Null-safe)
        // =================================================

        String owner = conversation.getUserEmail() != null ? conversation.getUserEmail().trim().toLowerCase() : "";
        String current = userEmail != null ? userEmail.trim().toLowerCase() : "";

        System.out.println("Access Check -> DB Owner: [" + owner + "], WS User: [" + current + "]");

        if (current.isBlank() || !owner.equals(current)) {
            System.err.println("❌ Security Check Failed: User " + current + " is not authorized for conversation owned by " + owner);
            throw new RuntimeException("You are not allowed to access this conversation");
        }

        // =================================================
        // SAVE USER MESSAGE
        // =================================================

        conversation.getMessages().add(new ChatMessage("user", prompt));
        conversation.setUpdatedAt(LocalDateTime.now());
        chatConversationRepository.save(conversation);

        // =================================================
        // STORE AI RESPONSE
        // =================================================

        StringBuilder aiResponse = new StringBuilder();

        // =================================================
        // CALL GEMINI
        // =================================================

        chatClient
                .prompt()
                .user(prompt)
                .stream()
                .content()
                .subscribe(
                        // On Next Chunk
                        chunk -> {
                            System.out.println("AI CHUNK: " + chunk);
                            aiResponse.append(chunk);
                            onChunk.accept(chunk);
                        },

                        // On Error
                        error -> {
                            System.err.println("❌ AI STREAM ERROR: " + error.getMessage());
                            error.printStackTrace();
                            onChunk.accept("\n\n*Error generating response: " + error.getMessage() + "*");
                            onComplete.run(); // Complete stream cleanly so frontend stops waiting
                        },

                        // On Complete
                        () -> {
                            System.out.println("AI STREAM COMPLETED");

                            try {
                                conversation.getMessages().add(
                                        new ChatMessage("assistant", aiResponse.toString())
                                );
                                conversation.setUpdatedAt(LocalDateTime.now());
                                chatConversationRepository.save(conversation);
                            } catch (Exception e) {
                                System.err.println("Failed to save final AI response: " + e.getMessage());
                            }

                            onComplete.run();
                        }
                );
    }
}