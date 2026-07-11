package com.lorafilm.movie.common.security;
 
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.api.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
 
@Component
public class InternalTokenFilter extends OncePerRequestFilter {
 
    @Value("${app.internal-token}")
    private String internalToken;
 
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        if (path != null && path.startsWith("/api/internal")) {
            String tokenHeader = request.getHeader("X-Internal-Token");
            if (tokenHeader == null || tokenHeader.isEmpty()) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                ApiResponse<Void> apiResponse = ApiResponse.fail("ERR_401_UNAUTHORIZED", "Missing internal token");
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(response.getOutputStream(), apiResponse);
                return;
            } else if (!tokenHeader.equals(internalToken)) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                ApiResponse<Void> apiResponse = ApiResponse.fail("ERR_403_FORBIDDEN", "Invalid internal token");
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(response.getOutputStream(), apiResponse);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
