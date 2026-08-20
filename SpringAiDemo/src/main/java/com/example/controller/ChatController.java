package com.example.controller;

import com.example.dto.CreateConversationRequest;
import com.example.entity.ChatConversation;
import com.example.repository.ChatConversationRepository;
import com.example.service.ChatService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatConversationRepository conversationRepository;

    public ChatController(
            ChatService chatService,
            ChatConversationRepository conversationRepository) {

        this.chatService = chatService;
        this.conversationRepository = conversationRepository;
    }

    // =====================================================
    // Test JWT authentication
    // =====================================================

    @GetMapping
    public String testTheEndpoint() {

        return "Jwt working Great..";
    }


    // =====================================================
    // Normal HTTP chat endpoint
    // =====================================================

    @PostMapping
    public String chat(
            @RequestBody String prompt) {

        return chatService.getResponse(prompt);
    }


    // =====================================================
    // GET USER CHAT HISTORY
    // =====================================================

    @GetMapping("/history")
    public List<ChatConversation> getChatHistory(
            Authentication authentication) {

        String email = authentication.getName();

        return conversationRepository
                .findByUserEmailOrderByUpdatedAtDesc(email);
    }


    // =====================================================
    // CREATE NEW CONVERSATION
    // =====================================================

    @PostMapping("/conversation")
    public ChatConversation createConversation(
            @RequestBody CreateConversationRequest request,
            Authentication authentication) {

        return chatService.createConversation(
                authentication.getName(),
                request.getTitle()
        );
    }
}