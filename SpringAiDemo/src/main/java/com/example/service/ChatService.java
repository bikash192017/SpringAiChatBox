package com.example.service;



import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String getResponse(String prompt) {

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
    public void streamResponse(
            String prompt,
            java.util.function.Consumer<String> onChunk,
            Runnable onComplete) {

        chatClient
                .prompt()
                .user(prompt)
                .stream()
                .content()
                .subscribe(
                        chunk -> {
                            System.out.println("AI CHUNK: " + chunk);
                            onChunk.accept(chunk);
                        },
                        error -> {
                            System.out.println("AI ERROR: " + error.getMessage());
                            error.printStackTrace();
                        },
                        () -> {
                            System.out.println("AI STREAM COMPLETED");
                            onComplete.run();
                        }
                );
    }
}
