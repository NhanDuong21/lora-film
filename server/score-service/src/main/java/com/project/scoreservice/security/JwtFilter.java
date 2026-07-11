package com.project.scoreservice.security;
 
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
import java.util.Collections;
import java.util.List;
 
@Component
public class JwtFilter extends OncePerRequestFilter {
 
    private final JwtProvider jwtProvider;
 
    public JwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }
 
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
 
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
 
        try {
            String jwt = getJwtFromRequest(request);
 
            if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
                Claims claims = jwtProvider.getClaimsFromToken(jwt);
                String role = claims.get("role", String.class);
                
                List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                if (role == null) {
                    role = "ROLE_USER"; // default fallback
                } else if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }
                authorities.add(new SimpleGrantedAuthority(role));

                // Map roles to granular permissions
                if ("ROLE_ADMIN".equalsIgnoreCase(role)) {
                    authorities.add(new SimpleGrantedAuthority("SCORE_READ"));
                    authorities.add(new SimpleGrantedAuthority("SCORE_ADJUST"));
                    authorities.add(new SimpleGrantedAuthority("SCORE_MANAGE"));
                    authorities.add(new SimpleGrantedAuthority("MEMBERSHIP_TIER_READ"));
                    authorities.add(new SimpleGrantedAuthority("MEMBERSHIP_TIER_MANAGE"));
                } else if ("ROLE_SCORE_MANAGE".equalsIgnoreCase(role)) {
                    authorities.add(new SimpleGrantedAuthority("SCORE_MANAGE"));
                    authorities.add(new SimpleGrantedAuthority("SCORE_READ"));
                } else if ("ROLE_SCORE_ADJUST".equalsIgnoreCase(role)) {
                    authorities.add(new SimpleGrantedAuthority("SCORE_ADJUST"));
                    authorities.add(new SimpleGrantedAuthority("SCORE_READ"));
                } else if ("ROLE_MEMBERSHIP_TIER_MANAGE".equalsIgnoreCase(role)) {
                    authorities.add(new SimpleGrantedAuthority("MEMBERSHIP_TIER_MANAGE"));
                    authorities.add(new SimpleGrantedAuthority("MEMBERSHIP_TIER_READ"));
                }
 
                Object userIdVal = claims.get("userId");
                String principal = userIdVal != null ? userIdVal.toString() : claims.getSubject();
 
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
 
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
