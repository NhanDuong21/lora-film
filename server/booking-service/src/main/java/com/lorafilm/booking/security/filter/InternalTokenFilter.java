package com.lorafilm.booking.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.common.response.ApiResponse;
import jakarta.annotation.PostConstruct;
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

    @Value("${app.internal-token}")
    private String internalToken;

    @PostConstruct
    public void init() {
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("CRITICAL SECURITY FAILURE: 'app.internal-token' configuration is missing! Must configure APP_INTERNAL_TOKEN environment variable.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }

        if (path != null && path.toLowerCase().startsWith("/internal")) {
            String tokenHeader = request.getHeader("X-Internal-Token");
            if (tokenHeader == null || tokenHeader.isEmpty()) {
                tokenHeader = request.getHeader("X-Service-Token");
            }

            if (tokenHeader == null || tokenHeader.isEmpty()) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                ApiResponse<Void> apiResponse = ApiResponse.error("ERR_401_UNAUTHORIZED: Missing internal token");
                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules();
                mapper.writeValue(response.getOutputStream(), apiResponse);
                return;
            } else if (!isConstantTimeEquals(tokenHeader, internalToken)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                ApiResponse<Void> apiResponse = ApiResponse.error("ERR_403_FORBIDDEN: Invalid internal token");
                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules();
                mapper.writeValue(response.getOutputStream(), apiResponse);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isConstantTimeEquals(String tokenA, String tokenB) {
        if (tokenA == null || tokenB == null) {
            return false;
        }
        byte[] aBytes = tokenA.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = tokenB.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
