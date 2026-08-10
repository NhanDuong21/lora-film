package com.project.analyticsservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.analyticsservice.common.ApiResponse;
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

    private final byte[] expectedToken;
    private final ObjectMapper objectMapper;

    public InternalTokenFilter(
            @Value("${app.internal-token:${ANALYTICS_INTERNAL_TOKEN:${INTERNAL_NOTIFICATION_TOKEN:secret-internal-token}}}")
            String internalToken,
                               ObjectMapper objectMapper) {
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("app.internal-token must be configured");
        }
        this.expectedToken = internalToken.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String applicationPath = contextPath == null || contextPath.isEmpty()
                ? requestUri
                : requestUri.substring(Math.min(contextPath.length(), requestUri.length()));
        return !applicationPath.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader("X-Internal-Token");
        if (provided == null || !MessageDigest.isEqual(
                expectedToken, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(),
                    ApiResponse.error("Invalid internal service token", "INTERNAL_TOKEN_INVALID"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
