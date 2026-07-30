package com.project.notificationservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "dGVzdC1vbmx5LWp3dC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaA==";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accountIdIsUsedAsNotificationOwnerIdentity() throws Exception {
        String token = Jwts.builder()
                .subject("customer@example.com")
                .claim("userId", 42L)
                .claim("role", "CUSTOMER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        request.addHeader("Authorization", "Bearer " + token);

        new JwtAuthenticationFilter(SECRET).doFilter(
                request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) -> {
                });

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("42");
    }
}
