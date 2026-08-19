package com.lorafilm.booking.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.common.response.ErrorResponse;
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

    @Value("${app.internal-payment-token:${app.internal-token}}")
    private String internalPaymentToken;

    @Value("${app.internal-promotion-audit-token:}")
    private String internalPromotionAuditToken;

    @PostConstruct
    public void init() {
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("CRITICAL SECURITY FAILURE: 'app.internal-token' configuration is missing! Must configure APP_INTERNAL_TOKEN environment variable.");
        }
        if (internalPaymentToken == null || internalPaymentToken.isBlank()) {
            throw new IllegalStateException("CRITICAL SECURITY FAILURE: 'app.internal-payment-token' configuration is missing! Must configure PAYMENT_TO_BOOKING_INTERNAL_TOKEN environment variable.");
        }
        if (internalPromotionAuditToken == null || internalPromotionAuditToken.isBlank()) {
            throw new IllegalStateException("CRITICAL SECURITY FAILURE: promotion audit token is missing");
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
            String expectedToken = isPaymentAuthorityPath(path)
                    ? internalPaymentToken
                    : internalToken;
            String tokenHeader = request.getHeader("X-Internal-Token");
            if (tokenHeader == null || tokenHeader.isEmpty()) {
                tokenHeader = request.getHeader("X-Service-Token");
            }

            if (tokenHeader == null || tokenHeader.isEmpty()) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                ErrorResponse errorResponse = new ErrorResponse(
                        "INTERNAL_TOKEN_MISSING",
                        "Missing internal service token");
                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules();
                mapper.writeValue(response.getOutputStream(), errorResponse);
                return;
            } else if (!isConstantTimeEquals(tokenHeader, expectedToken)
                    && !(isPromotionAuditPath(request, path)
                    && isConstantTimeEquals(tokenHeader, internalPromotionAuditToken))) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                ErrorResponse errorResponse = new ErrorResponse(
                        "INTERNAL_TOKEN_INVALID",
                        "Invalid internal service token");
                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules();
                mapper.writeValue(response.getOutputStream(), errorResponse);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPaymentAuthorityPath(String path) {
        String normalized = path.toLowerCase();
        return normalized.startsWith("/internal/bookings/")
                && (normalized.endsWith("/payment-context")
                || normalized.endsWith("/payment-results")
                || normalized.endsWith("/refund-results")
                || normalized.endsWith("/refund"));
    }

    private boolean isPromotionAuditPath(HttpServletRequest request, String path) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && path.toLowerCase().startsWith("/internal/bookings/")
                && path.toLowerCase().endsWith("/lifecycle-context");
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
