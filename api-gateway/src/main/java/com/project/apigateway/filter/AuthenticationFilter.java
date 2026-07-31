package com.project.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    private static final Pattern AUTHORITY = Pattern.compile("^[A-Za-z0-9_.:-]{1,100}$");
    private static final List<String> TRUSTED_IDENTITY_HEADERS = List.of(
            "loggedInUser", "loggedInUserId", "loggedInRole", "loggedInPermissions",
            "X-Authenticated-User", "X-Authenticated-User-Id",
            "X-Authenticated-Role", "X-Authenticated-Permissions");

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil, ObjectMapper objectMapper,
                                ReactiveStringRedisTemplate redisTemplate) {
        this.validator = validator;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> TRUSTED_IDENTITY_HEADERS.forEach(headers::remove))
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        if (!validator.isSecured.test(sanitizedRequest)) {
            return chain.filter(sanitizedExchange);
        }

        String authHeader = sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            return onError(sanitizedExchange, "Authentication is required",
                    "AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        if (!authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            return onError(sanitizedExchange, "Invalid authorization header format",
                    "AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        final Claims preliminaryClaims;
        try {
            preliminaryClaims = jwtUtil.getAllClaimsFromToken(token);
        } catch (Exception exception) {
            return onError(sanitizedExchange, "Invalid or expired access token",
                    "AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED);
        }
        Long accountId = numericClaim(preliminaryClaims.get("userId"));
        Long sessionId = numericClaim(preliminaryClaims.get("sid"));
        if (accountId == null) {
            return onError(sanitizedExchange, "Access token is missing required claims",
                    "AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED);
        }

        Mono<Boolean> tokenRevoked = redisTemplate.hasKey("blacklist:" + sha256(token));
        Mono<Boolean> sessionRevoked = sessionId == null
                ? Mono.just(false)
                : redisTemplate.hasKey("revoked_session:" + sessionId);
        Mono<String> accountRevokedAt = redisTemplate.opsForValue()
                .get("account_revoked_after:" + accountId)
                .defaultIfEmpty("");

        return Mono.zip(tokenRevoked, sessionRevoked, accountRevokedAt)
                .onErrorMap(exception -> new RuntimeException("REDIS_ERROR", exception))
                .flatMap(revocation -> {
                    if (isRevoked(revocation, preliminaryClaims)) {
                        return onError(sanitizedExchange, "Access token has been revoked",
                                "AUTH_TOKEN_REVOKED", HttpStatus.UNAUTHORIZED);
                    }
                    return authenticate(sanitizedExchange, sanitizedRequest, chain, token);
                })
                .onErrorResume(exception -> {
                    if ("REDIS_ERROR".equals(exception.getMessage())) {
                        return onError(sanitizedExchange,
                                "Authentication service is temporarily unavailable",
                                "AUTH_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
                    }
                    return Mono.error(exception);
                });
    }

    private boolean isRevoked(Tuple3<Boolean, Boolean, String> revocation, Claims claims) {
        if (Boolean.TRUE.equals(revocation.getT1()) || Boolean.TRUE.equals(revocation.getT2())) {
            return true;
        }
        if (revocation.getT3().isBlank()) {
            return false;
        }
        try {
            Number issuedAtMs = claims.get("iatMs", Number.class);
            long issuedAt = issuedAtMs == null
                    ? (claims.getIssuedAt() == null ? Long.MIN_VALUE : claims.getIssuedAt().getTime())
                    : issuedAtMs.longValue();
            return issuedAt < Long.parseLong(revocation.getT3());
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    private Mono<Void> authenticate(ServerWebExchange sanitizedExchange,
                                    ServerHttpRequest sanitizedRequest,
                                    GatewayFilterChain chain, String token) {
        try {
            Claims claims = jwtUtil.getAllClaimsFromToken(token);
            Long userId = numericClaim(claims.get("userId"));
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            String tokenType = claims.get("tokenType", String.class);
            if (subject == null || subject.isBlank() || userId == null
                    || role == null || !AUTHORITY.matcher(role).matches()
                    || !"access".equals(tokenType)) {
                return onError(sanitizedExchange, "Access token is missing required claims",
                        "AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED);
            }
            String permissions = extractPermissions(claims.get("permissions"));
            ServerHttpRequest authenticatedRequest = sanitizedRequest.mutate()
                    .header("loggedInUser", subject)
                    .header("loggedInUserId", String.valueOf(userId))
                    .header("loggedInRole", role)
                    .header("loggedInPermissions", permissions)
                    .header("X-Authenticated-User", subject)
                    .header("X-Authenticated-User-Id", String.valueOf(userId))
                    .header("X-Authenticated-Role", role)
                    .header("X-Authenticated-Permissions", permissions)
                    .build();
            return chain.filter(sanitizedExchange.mutate().request(authenticatedRequest).build());
        } catch (Exception exception) {
            return onError(sanitizedExchange, "Invalid or expired access token",
                    "AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private Long numericClaim(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String extractPermissions(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return "";
        }
        return collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(permission -> AUTHORITY.matcher(permission).matches())
                .distinct()
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String sha256(String value) {
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message,
                               String errorCode, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", message);
            response.put("errorCode", errorCode);
            response.put("data", null);
            body = objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException exception) {
            body = ("{\"success\":false,\"message\":\"Authentication failed\","
                    + "\"errorCode\":\"AUTH_UNAUTHORIZED\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
