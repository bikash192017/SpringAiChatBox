package com.example.config;

import com.example.security.CustomUserDetailsService;
import com.example.security.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        try {
            String query = request.getURI().getQuery();

            if (query == null || query.isBlank()) {
                System.out.println("WebSocket rejected: No query string found");
                return false;
            }

            String token = null;

            for (String parameter : query.split("&")) {
                String[] parts = parameter.split("=", 2);
                if (parts.length == 2 && "token".equals(parts[0])) {
                    token = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    break;
                }
            }

            if (token == null || token.isBlank()) {
                System.out.println("WebSocket rejected: Token parameter missing");
                return false;
            }

            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String email = jwtService.extractEmail(token);

            if (email == null || email.isBlank()) {
                System.out.println("WebSocket rejected: Invalid token payload");
                return false;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtService.isTokenValid(token, userDetails)) {
                System.out.println("WebSocket rejected: Token expired or invalid for " + email);
                return false;
            }

            // Store user details and email in the WebSocket session
            attributes.put("user", userDetails);
            attributes.put("email", email);

            System.out.println("WebSocket handshake authenticated for: " + email);
            return true;

        } catch (Exception e) {
            System.err.println("WebSocket authentication error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }
}