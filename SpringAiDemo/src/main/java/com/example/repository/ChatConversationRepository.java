package com.example.repository;

import com.example.entity.ChatConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatConversationRepository
        extends MongoRepository<ChatConversation, String> {

    List<ChatConversation> findByUserEmailOrderByUpdatedAtDesc(
            String userEmail
    );
}