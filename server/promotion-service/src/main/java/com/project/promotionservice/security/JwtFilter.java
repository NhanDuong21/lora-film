package com.project.promotionservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
                Claims claims = jwtProvider.getClaimsFromToken(jwt);
                String role = claims.get("role", String.class);
                Long userId = claims.get("userId", Long.class);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (role == null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                } else {
                    String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(roleName));
                    
                    // Map role to permission authorities
                    if ("ROLE_ADMIN".equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(role)) {
                        authorities.add(new SimpleGrantedAuthority("PROMOTION_READ"));
                        authorities.add(new SimpleGrantedAuthority("PROMOTION_CREATE"));
                        authorities.add(new SimpleGrantedAuthority("PROMOTION_UPDATE"));
                        authorities.add(new SimpleGrantedAuthority("PROMOTION_MANAGE"));
                    } else if ("ROLE_EMPLOYEE".equalsIgnoreCase(roleName) || "EMPLOYEE".equalsIgnoreCase(role)) {
                        authorities.add(new SimpleGrantedAuthority("PROMOTION_READ"));
                    }
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId != null ? userId.toString() : (claims.getSubject() != null ? claims.getSubject() : "anonymous"),
                        null,
                        authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Do nothing, just proceed without authentication
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
