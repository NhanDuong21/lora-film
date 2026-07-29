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
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import com.project.authservice.util.RefreshTokenHashUtil;
import org.springframework.security.core.userdetails.User;

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
            
            // Check blacklist
            String tokenHash = RefreshTokenHashUtil.hash(token);
            try {
                if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + tokenHash))) {
                    writeUnauthorized(response, "Token has been revoked");
                    return;
                }
            } catch (org.springframework.dao.DataAccessException exception) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Authentication service is temporarily unavailable\","
                                + "\"errorCode\":\"AUTH_SERVICE_UNAVAILABLE\",\"data\":null}");
                return;
            }
            
            try {
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(normalizedRole));
                    jwtUtil.extractPermissions(token).stream()
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                    User principal = new User(username, "", authorities);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    writeUnauthorized(response, "Invalid or expired access token");
                    return;
                }
            } catch (Exception e) {
                logger.debug("Rejected invalid access token", e);
                writeUnauthorized(response, "Invalid or expired access token");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message
                + "\",\"errorCode\":\"AUTH_UNAUTHORIZED\",\"data\":null}");
    }
    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }
}
