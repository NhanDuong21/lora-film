package com.project.authservice.security;

import com.project.authservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                if (jwtUtil.validateToken(token)) {
                    String tokenHash = com.project.authservice.util.RefreshTokenHashUtil.hash(token);
                    Long accountId = jwtUtil.extractUserId(token);
                    Long sessionId = jwtUtil.extractSessionId(token);
                    if (isRevoked(tokenHash, accountId, sessionId, token)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);
                    var permissions = jwtUtil.extractPermissions(token);
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    permissions.stream()
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                    if (permissions.contains("PERM_ROOT_ACCESS")
                            && !"ADMIN".equals(role) && !"ROLE_ADMIN".equals(role)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                logger.warn("Rejected invalid access token");
            }
        }
        
        filterChain.doFilter(request, response);
    }
    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    private boolean isRevoked(String tokenHash, Long accountId, Long sessionId, String token) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + tokenHash))) {
            return true;
        }
        if (sessionId != null && Boolean.TRUE.equals(redisTemplate.hasKey("revoked_session:" + sessionId))) {
            return true;
        }
        String revokedAt = redisTemplate.opsForValue().get("account_revoked_after:" + accountId);
        if (revokedAt == null) {
            return false;
        }
        try {
            return jwtUtil.extractIssuedAtMillis(token) < Long.parseLong(revokedAt);
        } catch (NumberFormatException exception) {
            return true;
        }
    }
}
