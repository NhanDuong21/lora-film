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
                
                List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                
                // [TEMP - TO BE REMOVED WHEN RBAC IS IMPLEMENTED]: Basic role extraction supporting single role or roles list
                Object roleClaim = claims.get("role");
                if (roleClaim != null && StringUtils.hasText(roleClaim.toString())) {
                    String role = roleClaim.toString();
                    String roleAuth = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(roleAuth));
                    authorities.add(new SimpleGrantedAuthority(role)); // Add raw name as well for flexibility
                }
                
                Object rolesClaim = claims.get("roles");
                if (rolesClaim instanceof List<?> list) {
                    for (Object r : list) {
                        if (r != null && StringUtils.hasText(r.toString())) {
                            String roleStr = r.toString();
                            String roleAuth = roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr;
                            authorities.add(new SimpleGrantedAuthority(roleAuth));
                            authorities.add(new SimpleGrantedAuthority(roleStr));
                        }
                    }
                }
                
                if (authorities.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    authorities.add(new SimpleGrantedAuthority("USER"));
                }

                // [TODO - RE-ENABLE WHEN RBAC IS READY]: Map roles to granular permissions for Score Service
                // String roleStr = claims.get("role", String.class);
                // if ("ROLE_ADMIN".equalsIgnoreCase(roleStr)) {
                //     authorities.add(new SimpleGrantedAuthority("SCORE_READ"));
                //     authorities.add(new SimpleGrantedAuthority("SCORE_ADJUST"));
                //     authorities.add(new SimpleGrantedAuthority("SCORE_MANAGE"));
                //     authorities.add(new SimpleGrantedAuthority("MEMBERSHIP_TIER_READ"));
                //     authorities.add(new SimpleGrantedAuthority("MEMBERSHIP_TIER_MANAGE"));
                // } else if ("ROLE_SCORE_MANAGE".equalsIgnoreCase(roleStr)) {
                //     authorities.add(new SimpleGrantedAuthority("SCORE_MANAGE"));
                //     authorities.add(new SimpleGrantedAuthority("SCORE_READ"));
                // } else if ("ROLE_SCORE_ADJUST".equalsIgnoreCase(roleStr)) {
                //     authorities.add(new SimpleGrantedAuthority("SCORE_ADJUST"));
                //     authorities.add(new SimpleGrantedAuthority("SCORE_READ"));
                // } else if ("ROLE_MEMBERSHIP_TIER_MANAGE".equalsIgnoreCase(roleStr)) {
                //     authorities.add(new SimpleGrantedAuthority("MEMBERSHIP_TIER_MANAGE"));
                //     authorities.add(new SimpleGrantedAuthority("MEMBERSHIP_TIER_READ"));
                // }
 
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
