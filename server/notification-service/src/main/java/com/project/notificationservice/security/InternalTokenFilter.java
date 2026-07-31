package com.project.notificationservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.api.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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
            @Value("${notification.security.internal-token:}") String internalToken,
            ObjectMapper objectMapper) {
        this.internalToken = internalToken;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/v1/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String provided = request.getHeader("X-Internal-Token");
        boolean valid = !internalToken.isBlank() && provided != null
                && MessageDigest.isEqual(
                        internalToken.getBytes(StandardCharsets.UTF_8),
                        provided.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    ApiResponse.error("Invalid service credential", "INTERNAL_UNAUTHORIZED"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
