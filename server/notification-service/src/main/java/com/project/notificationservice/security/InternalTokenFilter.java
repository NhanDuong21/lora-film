package com.project.notificationservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.common.ApiResponse;
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

    @Value("${app.internal-token:secret-internal-token}")
    private String internalToken;

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        if (path != null && path.startsWith("/internal")) {
            String tokenHeader = request.getHeader("X-Internal-Token");
            if (tokenHeader == null || !tokenHeader.equals(internalToken)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding("UTF-8");

                ApiResponse<Void> apiResponse = ApiResponse.error("Invalid internal token", "UNAUTHORIZED");
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(response.getOutputStream(), apiResponse);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
