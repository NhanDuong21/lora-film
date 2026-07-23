package com.lorafilm.booking.common.filter;

import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.security.principal.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final BookingMetricsManager metricsManager;

    public RequestLoggingFilter(BookingMetricsManager metricsManager) {
        this.metricsManager = metricsManager;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        return path != null && (path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/favicon.ico"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        metricsManager.incrementApiRequest();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            metricsManager.recordApiLatency(duration);

            int status = response.getStatus();
            if (status >= 400) {
                metricsManager.incrementApiError();
            }

            // Extract User ID from Security Context
            Long userId = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
                userId = ((UserPrincipal) auth.getPrincipal()).getId();
            }

            if (userId != null) {
                MDC.put("userId", String.valueOf(userId));
            } else {
                MDC.remove("userId");
            }

            MDC.put("action", "API_REQUEST");

            log.info("Request processed: method={} uri={} status={} duration={}ms userId={} correlationId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration,
                    userId != null ? userId : "anonymous",
                    MDC.get("correlationId")
            );

            // Clean up request-specific MDC keys (MDC.clear() will run in CorrelationIdFilter)
            MDC.remove("userId");
            MDC.remove("action");
        }
    }
}
