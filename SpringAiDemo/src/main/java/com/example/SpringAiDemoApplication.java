package com.example;

import com.example.entity.ChatConversation;
import com.example.entity.ChatMessage;
import com.example.repository.ChatConversationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringAiDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiDemoApplication.class, args);
	}


    @Bean
    CommandLineRunner testMongoDB(
            ChatConversationRepository repository) {

        return args -> {

            System.out.println(
                    "========== MONGODB TEST STARTED =========="
            );

            ChatConversation conversation =
                    new ChatConversation(
                            "test@gmail.com",
                            "MongoDB Test"
                    );

            conversation.getMessages().add(
                    new ChatMessage(
                            "USER",
                            "Testing MongoDB connection"
                    )
            );

            ChatConversation saved =
                    repository.save(conversation);

            System.out.println(
                    "MONGODB DOCUMENT SAVED"
            );

            System.out.println(
                    "ID: " + saved.getId()
            );

            System.out.println(
                    "========== MONGODB TEST COMPLETED =========="
            );
        };
    }
    }
