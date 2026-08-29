package com.example.config;

import com.example.security.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");

            System.out.println("WebSocket Handshake - Token received: " + (token != null));

            if (token != null && !token.isBlank()) {
                try {
                    String email = jwtService.extractEmail(token);
                    System.out.println("WebSocket Handshake - Extracted Email: " + email);

                    if (email != null && !email.isBlank()) {
                        // Crucial: Must be "userEmail" in lowercase & trimmed
                        attributes.put("userEmail", email.trim().toLowerCase());
                        return true;
                    }
                } catch (Exception e) {
                    System.err.println("❌ WebSocket JWT extraction failed: " + e.getMessage());
                }
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }
}