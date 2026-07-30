package com.project.notificationservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String jwtSecret;

    public JwtAuthenticationFilter(@Value("${jwt.secret:}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (SecurityContextHolder.getContext().getAuthentication() == null
                && authorization != null
                && authorization.startsWith("Bearer ")
                && !jwtSecret.isBlank()) {
            try {
                Claims claims = Jwts.parser().verifyWith(signingKey()).build()
                        .parseSignedClaims(authorization.substring(7)).getPayload();
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                String role = claims.get("role", String.class);
                if (role != null && !role.isBlank()) {
                    String normalized = role.toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", "");
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
                }
                Object permissions = claims.get("permissions");
                if (permissions instanceof Collection<?> values) {
                    values.stream().map(String::valueOf)
                            .filter(value -> value.matches("[A-Za-z0-9_.:-]{1,100}"))
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                }
                Object userId = claims.get("userId");
                String principal = userId == null ? null : String.valueOf(userId);
                if (principal == null || principal.isBlank()) {
                    principal = claims.get("publicId", String.class);
                }
                if (principal == null || principal.isBlank()) {
                    principal = claims.getSubject();
                }
                if (principal != null && !principal.isBlank()) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, authorities));
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private SecretKey signingKey() {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        } catch (RuntimeException exception) {
            return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
