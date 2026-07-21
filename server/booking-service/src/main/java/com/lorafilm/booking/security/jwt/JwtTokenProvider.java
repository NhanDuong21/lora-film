package com.lorafilm.booking.security.jwt;

import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    // Structural JWT token provider foundation
    public boolean validateToken(String token) {
        return token != null && !token.isBlank();
    }
}
