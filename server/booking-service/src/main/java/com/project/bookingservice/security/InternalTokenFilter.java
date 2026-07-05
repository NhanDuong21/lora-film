package com.project.bookingservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    @Value("${booking.internal.token:}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/internal/")) {
            String token = request.getHeader("X-Internal-Token");
            
            if (expectedToken == null || expectedToken.isEmpty()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal token is not configured");
                return;
            }

            if (token == null || !token.equals(expectedToken)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal token");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
