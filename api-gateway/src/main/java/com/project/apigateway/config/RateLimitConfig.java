package com.project.apigateway.config;

import java.net.InetSocketAddress;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    private static final String AUTHENTICATED_USER_ID_HEADER = "X-Authenticated-User-Id";

    @Bean
    public KeyResolver clientKeyResolver() {
        return exchange -> {
            String accountId = exchange.getRequest().getHeaders()
                    .getFirst(AUTHENTICATED_USER_ID_HEADER);
            if (accountId != null && !accountId.isBlank()) {
                return Mono.just("account:" + accountId);
            }

            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just("ip:" + remoteAddress.getAddress().getHostAddress());
            }
            return Mono.just("ip:unknown");
        };
    }
}
