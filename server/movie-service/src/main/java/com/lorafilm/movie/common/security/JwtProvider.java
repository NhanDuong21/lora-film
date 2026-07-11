package com.lorafilm.movie.common.security;
 
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import javax.crypto.SecretKey;
 
@Component
public class JwtProvider {
 
    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;
 
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret));
    }
 
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
 
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
