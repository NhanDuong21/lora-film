package com.project.promotionservice.configuration.security.filter;

import com.project.promotionservice.configuration.security.jwt.JwtTokenProvider;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Claims claims = jwtTokenProvider.getClaimsFromToken(jwt);

                Long userId = extractUserId(claims);
                String email = claims.get("email", String.class);
                String username = claims.get("username", String.class);
                if (username == null) {
                    username = email != null ? email : (userId != null ? userId.toString() : claims.getSubject());
                }

                List<String> rolesList = extractListClaim(claims, "roles", "role");
                List<String> permissionsList = extractListClaim(claims, "permissions", "permission");

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                for (String r : rolesList) {
                    if (StringUtils.hasText(r)) {
                        String roleAuth = r.startsWith("ROLE_") ? r : "ROLE_" + r;
                        authorities.add(new SimpleGrantedAuthority(roleAuth));
                    }
                }

                for (String p : permissionsList) {
                    if (StringUtils.hasText(p)) {
                        authorities.add(new SimpleGrantedAuthority(p));
                    }
                }

                UserPrincipal principal = new UserPrincipal(userId, username, email, rolesList, permissionsList, authorities);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
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

    private Long extractUserId(Claims claims) {
        Object userIdVal = claims.get("userId");
        if (userIdVal == null) {
            userIdVal = claims.get("id");
        }
        if (userIdVal == null) {
            userIdVal = claims.getSubject();
        }

        if (userIdVal != null) {
            try {
                return Long.valueOf(userIdVal.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractListClaim(Claims claims, String primaryKey, String secondaryKey) {
        Object val = claims.get(primaryKey);
        if (val == null && secondaryKey != null) {
            val = claims.get(secondaryKey);
        }

        if (val instanceof List<?>) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) val) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        } else if (val instanceof String str) {
            if (str.contains(",")) {
                return List.of(str.split(","));
            } else if (!str.isBlank()) {
                return List.of(str.trim());
            }
        }
        return Collections.emptyList();
    }
}
