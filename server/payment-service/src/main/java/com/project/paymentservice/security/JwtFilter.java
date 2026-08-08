package com.project.paymentservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

                Number userIdNumber = claims.get("userId", Number.class);
                Long userId = userIdNumber != null ? userIdNumber.longValue() : null;
                String role = claims.get("role", String.class);

                if (userId != null && role != null) {
                    String normalizedRole = role.toUpperCase();
                    if (normalizedRole.startsWith("ROLE_")) {
                        normalizedRole = normalizedRole.substring("ROLE_".length());
                    }
                    List<org.springframework.security.core.GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            "ROLE_" + normalizedRole));
                    Object permissionClaim = claims.get("permissions");
                    if (permissionClaim instanceof Collection<?> permissions) {
                        permissions.stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                                .filter(authority -> !authorities.contains(authority))
                                .forEach(authorities::add);
                    }
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
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
