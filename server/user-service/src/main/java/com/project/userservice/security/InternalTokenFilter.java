package com.project.userservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userservice.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    private final String internalToken;
    private final ObjectMapper objectMapper;

    public InternalTokenFilter(
            @Value("${app.internal-token:}") String internalToken,
            ObjectMapper objectMapper) {
        this.internalToken = internalToken;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader("X-Internal-Token");
        boolean valid = !internalToken.isBlank() && provided != null
                && MessageDigest.isEqual(
                        internalToken.getBytes(StandardCharsets.UTF_8),
                        provided.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            objectMapper.writeValue(response.getOutputStream(),
                    ApiResponse.error("Invalid internal service token", "INTERNAL_TOKEN_INVALID"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
