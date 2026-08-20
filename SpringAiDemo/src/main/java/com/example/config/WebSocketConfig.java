package com.example.config;

import com.example.security.CustomUserDetailsService;
import com.example.security.JwtHandshakeInterceptor;
import com.example.security.JwtService;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.chatWebSocketHandler = chatWebSocketHandler;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry) {

        registry.addHandler(
                        chatWebSocketHandler,
                        "/ws/chat"
                )
                .addInterceptors(
                        new JwtHandshakeInterceptor(
                                jwtService,
                                userDetailsService
                        )
                )
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:5174"
                );
    }
}