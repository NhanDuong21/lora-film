package com.project.promotionservice.configuration.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.configuration.security.principal.InternalServicePrincipal;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final String bookingServiceToken;
    private final String paymentServiceToken;
    private final String operationsServiceToken;

    public InternalTokenFilter(
            ObjectMapper objectMapper,
            @Value("${app.internal-auth.booking-service-token:}") String bookingServiceToken,
            @Value("${app.internal-auth.payment-service-token:}") String paymentServiceToken,
            @Value("${app.internal-auth.operations-service-token:}") String operationsServiceToken) {
        this.objectMapper = objectMapper;
        this.bookingServiceToken = normalizeSecret(bookingServiceToken);
        this.paymentServiceToken = normalizeSecret(paymentServiceToken);
        this.operationsServiceToken = normalizeSecret(operationsServiceToken);
    }

    @PostConstruct
    public void init() {
        if (bookingServiceToken == null || paymentServiceToken == null) {
            throw new IllegalStateException(
                    "CRITICAL SECURITY FAILURE: Booking and Payment internal tokens must be configured");
        }
        if (isConstantTimeEquals(bookingServiceToken, paymentServiceToken)) {
            throw new IllegalStateException(
                    "CRITICAL SECURITY FAILURE: Internal services must not share the same token");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }

        if (path != null && path.startsWith("/internal")) {
            List<String> serviceHeaders =
                    Collections.list(request.getHeaders("X-Service-Name"));
            List<String> tokenHeaders =
                    Collections.list(request.getHeaders("X-Internal-Token"));
            if (serviceHeaders.size() != 1 || tokenHeaders.size() != 1) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "ERR_401_UNAUTHORIZED: Exactly one X-Service-Name and X-Internal-Token are required");
                return;
            }
            String serviceName = normalizeServiceName(serviceHeaders.getFirst());
            String tokenHeader = tokenHeaders.getFirst();

            if (serviceName == null || serviceName.length() > 64
                    || tokenHeader == null || tokenHeader.isBlank()
                    || tokenHeader.length() > 512) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "ERR_401_UNAUTHORIZED: X-Service-Name and X-Internal-Token are required");
                return;
            }

            String expectedToken = serviceTokens().get(serviceName);
            if (expectedToken == null || !isConstantTimeEquals(tokenHeader, expectedToken)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        "ERR_403_FORBIDDEN: Invalid internal service identity");
                return;
            }

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_INTERNAL"),
                    new SimpleGrantedAuthority("ROLE_" + serviceName));
            InternalServicePrincipal principal = new InternalServicePrincipal(serviceName);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private Map<String, String> serviceTokens() {
        Map<String, String> tokens = new java.util.HashMap<>();
        tokens.put("BOOKING_SERVICE", bookingServiceToken);
        tokens.put("PAYMENT_SERVICE", paymentServiceToken);
        if (operationsServiceToken != null) {
            tokens.put("OPERATIONS_SERVICE", operationsServiceToken);
        }
        return tokens;
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(status);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message));
    }

    private String normalizeServiceName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private String normalizeSecret(String value) {
        return value == null || value.isBlank() ? null : value;
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
