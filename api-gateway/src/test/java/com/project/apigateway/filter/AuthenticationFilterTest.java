package com.project.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

class AuthenticationFilterTest {

    @Test
    void protectedRouteRejectsMissingBearerToken() {
        AuthenticationFilter filter = filter(mock(JwtUtil.class));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/profile").build());

        filter.filter(exchange, ignored -> reactor.core.publisher.Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"errorCode\":\"AUTH_UNAUTHORIZED\"");
    }

    @Test
    void validAccessTokenForwardsTrustedIdentityAndPermissions() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        Claims claims = Jwts.claims().subject("member@example.com")
                .add(Map.of(
                        "userId", 42L,
                        "role", "ADMIN",
                        "tokenType", "access",
                        "permissions", List.of("USER_READ", "USER_WRITE")))
                .build();
        when(jwtUtil.getAllClaimsFromToken("valid-token")).thenReturn(claims);
        AuthenticationFilter filter = filter(jwtUtil);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .header("loggedInUserId", "999")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = value -> {
            forwarded.set(value);
            return reactor.core.publisher.Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("loggedInUserId"))
                .isEqualTo("42");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("loggedInPermissions"))
                .isEqualTo("USER_READ,USER_WRITE");
    }

    @Test
    void publicRouteStillStripsSpoofedIdentityHeaders() {
        AuthenticationFilter filter = filter(mock(JwtUtil.class));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .header("X-Authenticated-User-Id", "999")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, value -> {
            forwarded.set(value);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders()
                .containsKey("X-Authenticated-User-Id")).isFalse();
    }

    private AuthenticationFilter filter(JwtUtil jwtUtil) {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        when(redisTemplate.hasKey(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(reactor.core.publisher.Mono.just(false));
        return new AuthenticationFilter(new RouteValidator(), jwtUtil, new ObjectMapper(), redisTemplate);
    }
}
