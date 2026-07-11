package com.project.promotionservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    private final String internalToken;

    public InternalTokenFilter(@Value("${app.internal-token:secret-internal-token}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        
        if (path != null && path.startsWith("/internal")) {
            String tokenHeader = request.getHeader("X-Internal-Token");
            
            if (tokenHeader == null || tokenHeader.trim().isEmpty()) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                ApiResponse<Void> apiResponse = ApiResponse.error("Missing internal token", "INTERNAL_UNAUTHORIZED");
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(response.getOutputStream(), apiResponse);
                return;
            } else if (!tokenHeader.equals(internalToken)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                ApiResponse<Void> apiResponse = ApiResponse.error("Invalid internal token", "FORBIDDEN");
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(response.getOutputStream(), apiResponse);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
