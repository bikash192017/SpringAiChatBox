package com.example.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    @Value("${MONGODB_URI:${spring.data.mongodb.uri:}}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        System.out.println("============================================");
        System.out.println("INITIALIZING MONGO CLIENT WITH URI:");
        System.out.println(mongoUri);
        System.out.println("============================================");

        if (mongoUri == null || mongoUri.isBlank()) {
            throw new IllegalStateException("MONGODB_URI environment variable is missing or empty!");
        }

        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        // Automatically extracts the database name from the URI (e.g., chatdb)
        return new MongoTemplate(mongoClient, "chatdb");
    }
}