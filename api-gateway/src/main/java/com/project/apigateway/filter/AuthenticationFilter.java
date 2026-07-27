package com.project.apigateway.filter;

import com.project.apigateway.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (validator.isSecured.test(exchange.getRequest())) {
                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || authHeader.isBlank()) {
                    return onError(exchange, "Authentication is required", "UNAUTHORIZED",
                            HttpStatus.UNAUTHORIZED);
                }

                if (authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
                    authHeader = authHeader.substring(7);
                } else {
                    return onError(exchange, "Invalid authorization header format", "AUTH_TOKEN_INVALID",
                            HttpStatus.UNAUTHORIZED);
                }
                
                try {
                    Claims claims = jwtUtil.getAllClaimsFromToken(authHeader);
                    Long userId = claims.get("userId", Long.class);
                    String role = claims.get("role", String.class);
                    if (claims.getSubject() == null || userId == null || role == null || role.isBlank()) {
                        return onError(exchange, "Access token is missing required claims",
                                "AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED);
                    }
                    
                    ServerHttpRequest authenticatedRequest = exchange.getRequest().mutate()
                            .header("loggedInUser", claims.getSubject())
                            .header("loggedInUserId", String.valueOf(userId))
                            .header("loggedInRole", role)
                            .build();
                    return chain.filter(exchange.mutate().request(authenticatedRequest).build());
                } catch (Exception e) {
                    return onError(exchange, "Invalid or expired access token", "AUTH_TOKEN_INVALID",
                            HttpStatus.UNAUTHORIZED);
                }
            }
            return chain.filter(exchange);
        });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, String code, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(
                    Map.of("success", false, "message", message, "code", code));
        } catch (JsonProcessingException exception) {
            body = "{\"success\":false,\"message\":\"Authentication failed\",\"code\":\"UNAUTHORIZED\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    public static class Config {
    }
}
