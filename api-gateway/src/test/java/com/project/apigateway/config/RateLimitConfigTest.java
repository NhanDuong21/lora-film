package com.project.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigTest {
    private final KeyResolver keyResolver = new RateLimitConfig().clientKeyResolver();

    @Test
    void usesAuthenticatedAccountAsRateLimitKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/profile")
                        .header("X-Authenticated-User-Id", "18")
                        .remoteAddress(new InetSocketAddress("192.0.2.10", 12345))
                        .build());

        assertThat(keyResolver.resolve(exchange).block()).isEqualTo("account:18");
    }

    @Test
    void usesRemoteAddressForPublicRequests() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login")
                        .remoteAddress(new InetSocketAddress("192.0.2.10", 12345))
                        .build());

        assertThat(keyResolver.resolve(exchange).block()).isEqualTo("ip:192.0.2.10");
    }
}
