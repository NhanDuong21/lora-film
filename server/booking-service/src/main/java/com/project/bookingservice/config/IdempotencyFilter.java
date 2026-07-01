package com.project.bookingservice.config;

import com.project.bookingservice.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1) // Ensure it runs early
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyService idempotencyService;
    private final org.springframework.web.servlet.HandlerExceptionResolver resolver;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyService idempotencyService,
                             @org.springframework.beans.factory.annotation.Qualifier("handlerExceptionResolver") org.springframework.web.servlet.HandlerExceptionResolver resolver) {
        this.idempotencyService = idempotencyService;
        this.resolver = resolver;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    private String generateHash(byte[] payload) {
        if (payload == null || payload.length == 0) return "";
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(payload);
            if (node.isObject()) {
                com.fasterxml.jackson.databind.node.ObjectNode obj = (com.fasterxml.jackson.databind.node.ObjectNode) node;
                // Sort arrays in the root to canonicalize (e.g. seatIds or reservationIds)
                obj.fieldNames().forEachRemaining(key -> {
                    com.fasterxml.jackson.databind.JsonNode child = obj.get(key);
                    if (child.isArray()) {
                        java.util.List<com.fasterxml.jackson.databind.JsonNode> list = new java.util.ArrayList<>();
                        child.elements().forEachRemaining(list::add);
                        list.sort((a, b) -> a.asText().compareTo(b.asText()));
                        com.fasterxml.jackson.databind.node.ArrayNode sortedArray = objectMapper.createArrayNode();
                        list.forEach(sortedArray::add);
                        obj.set(key, sortedArray);
                    }
                });
            }
            byte[] canonicalBytes = objectMapper.writeValueAsBytes(node);
            return org.springframework.util.DigestUtils.md5DigestAsHex(canonicalBytes);
        } catch (Exception e) {
            return org.springframework.util.DigestUtils.md5DigestAsHex(payload);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            // Endpoints that require it will fail at controller level, or we can just let it pass
            // but the requirement says "Extract the mandatory header Idempotency-Key"
            // We will only enforce it if the controller requires it. Since some POSTs might not require it.
            // Actually, wait, let's check if the path is one of the impacted endpoints.
            String path = request.getRequestURI();
            boolean isImpacted = path.startsWith("/api/bookings") || path.startsWith("/internal/bookings");
            if (isImpacted) {
                // If the header is missing but the endpoint is impacted, let the controller handle the @RequestHeader validation.
                // We'll just pass it through if no key is provided, or return 400.
                // For safety, let's just let it pass if missing, controller will throw MissingRequestHeaderException.
                filterChain.doFilter(request, response);
                return;
            } else {
                filterChain.doFilter(request, response);
                return;
            }
        }

        try {
            UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Idempotency-Key must be a valid UUID");
            return;
        }

        IdempotencyService.IdempotencyRecord record;
        try {
            record = idempotencyService.get(idempotencyKey);
        } catch (Exception e) {
            resolver.resolveException(request, response, null, e);
            return;
        }

        boolean acquired = false;
        if (record == null) {
            try {
                acquired = idempotencyService.acquire(idempotencyKey);
            } catch (Exception e) {
                resolver.resolveException(request, response, null, e);
                return;
            }
            if (!acquired) {
                try {
                    record = idempotencyService.get(idempotencyKey);
                } catch (Exception e) {
                    resolver.resolveException(request, response, null, e);
                    return;
                }
            }
        }

        if (record != null || !acquired) {
            int retries = 50;
            while ((record == null || record.getRequestHash() == null) && retries > 0) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                try {
                    record = idempotencyService.get(idempotencyKey);
                } catch (Exception e) {
                    resolver.resolveException(request, response, null, e);
                    return;
                }
                retries--;
            }

            if (record == null || record.getRequestHash() == null) {
                response.sendError(HttpStatus.CONFLICT.value(), "BOOKING_IDEMPOTENCY_CONFLICT");
                return;
            }

            byte[] requestBodyBytes = request.getInputStream().readAllBytes();
            String currentHash = generateHash(requestBodyBytes);

            if (currentHash.equals(record.getRequestHash())) {
                response.setStatus(record.getResponseStatus());
                if (record.getContentType() != null) {
                    response.setContentType(record.getContentType());
                }
                response.getOutputStream().write(record.getResponseBody());
                return;
            } else {
                response.sendError(HttpStatus.CONFLICT.value(), "BOOKING_IDEMPOTENCY_CONFLICT");
                return;
            }
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            byte[] requestBodyBytes = wrappedRequest.getContentAsByteArray();
            String hash = generateHash(requestBodyBytes);
            byte[] responseBodyBytes = wrappedResponse.getContentAsByteArray();

            IdempotencyService.IdempotencyRecord newRecord = new IdempotencyService.IdempotencyRecord(
                    hash,
                    wrappedResponse.getStatus(),
                    responseBodyBytes,
                    wrappedResponse.getContentType()
            );

            idempotencyService.save(idempotencyKey, newRecord);

            wrappedResponse.copyBodyToResponse();
        } catch (Exception e) {
            idempotencyService.remove(idempotencyKey);
            throw e;
        }
    }
}
