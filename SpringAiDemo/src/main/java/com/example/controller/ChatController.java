package com.example.controller;



import com.example.service.ChatService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    @GetMapping()
    public String testTheEndpoint(){
        return "Jwt working Great..";
    }
    @PostMapping
    public String chat(@RequestBody String prompt) {
        return chatService.getResponse(prompt);
    }
}
