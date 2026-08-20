package com.example.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtHandshakeInterceptor(
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

            String query =
                    request.getURI().getQuery();

            if (query == null || query.isEmpty()) {
                System.out.println("WebSocket rejected: No token");
                return false;
            }

            String token = null;

            for (String parameter : query.split("&")) {

                String[] parts = parameter.split("=", 2);

                if (parts.length == 2 &&
                        parts[0].equals("token")) {

                    token = parts[1];
                    break;
                }
            }

            if (token == null || token.isEmpty()) {
                System.out.println("WebSocket rejected: Token missing");
                return false;
            }

            String email =
                    jwtService.extractEmail(token);

            UserDetails userDetails =
                    userDetailsService
                            .loadUserByUsername(email);

            if (!jwtService.isTokenValid(
                    token,
                    userDetails)) {

                System.out.println(
                        "WebSocket rejected: Invalid JWT"
                );

                return false;
            }

            // Store authenticated user information
            // inside WebSocket session attributes
            attributes.put("email", email);

            System.out.println(
                    "WebSocket authenticated user: "
                            + email
            );

            return true;

        } catch (Exception e) {

            System.out.println(
                    "WebSocket JWT validation failed: "
                            + e.getMessage()
            );

            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        // Nothing required here
    }
}