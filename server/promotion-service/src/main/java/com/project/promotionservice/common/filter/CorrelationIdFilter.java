package com.project.promotionservice.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String TRACE_HEADER = "X-Trace-ID";

    private final ObjectMapper objectMapper;

    public CorrelationIdFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        List<String> correlationHeaders = Collections.list(request.getHeaders(CORRELATION_HEADER));

        // 1. Validate Duplicated Header
        if (correlationHeaders.size() > 1) {
            writeErrorResponse(response, "DUPLICATED_HEADER", "Duplicated X-Correlation-ID header is not allowed", HttpStatus.BAD_REQUEST);
            return;
        }

        String correlationId = correlationHeaders.isEmpty() ? null : correlationHeaders.getFirst();

        // 2. Validate format of Correlation ID if present
        if (correlationId != null) {
            if (correlationId.trim().isEmpty()) {
                writeErrorResponse(response, "INVALID_CORRELATION_ID", "Correlation ID must not be empty", HttpStatus.BAD_REQUEST);
                return;
            }
            correlationId = correlationId.trim();
        } else {
            correlationId = UUID.randomUUID().toString();
        }

        // Validate Trace ID if present
        List<String> traceHeaders = Collections.list(request.getHeaders(TRACE_HEADER));
        if (traceHeaders.size() > 1) {
            writeErrorResponse(response, "DUPLICATED_HEADER", "Duplicated X-Trace-ID header is not allowed", HttpStatus.BAD_REQUEST);
            return;
        }
        String traceId = traceHeaders.isEmpty() ? null : traceHeaders.getFirst();
        if (traceId != null) {
            if (traceId.trim().isEmpty()) {
                writeErrorResponse(response, "INVALID_TRACE_ID", "Trace ID must not be empty", HttpStatus.BAD_REQUEST);
                return;
            }
            traceId = traceId.trim();
        } else {
            traceId = correlationId;
        }

        MDC.put("correlationId", correlationId);
        MDC.put("traceId", traceId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private void writeErrorResponse(HttpServletResponse response, String errorCode, String message, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
