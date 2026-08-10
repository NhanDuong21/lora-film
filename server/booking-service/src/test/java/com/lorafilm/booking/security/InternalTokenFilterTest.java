package com.lorafilm.booking.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.security.filter.InternalTokenFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InternalTokenFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private InternalTokenFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalTokenFilter();
        ReflectionTestUtils.setField(filter, "internalToken", "general-token");
        ReflectionTestUtils.setField(filter, "internalPaymentToken", "payment-token");
        filter.init();
    }

    @Test
    void paymentResultRejectsGeneralInternalToken() throws Exception {
        MockHttpServletRequest request = internalRequest(
                "/internal/bookings/550e8400-e29b-41d4-a716-446655440000/payment-results",
                "general-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals("INTERNAL_TOKEN_INVALID", body.path("errorCode").asText());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void paymentResultAcceptsDedicatedPaymentToken() throws Exception {
        MockHttpServletRequest request = internalRequest(
                "/internal/bookings/550e8400-e29b-41d4-a716-446655440000/payment-results",
                "payment-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void ordinaryInternalRouteRejectsPaymentToken() throws Exception {
        MockHttpServletRequest request = internalRequest(
                "/internal/bookings/code/LORAFILM-20260727-000001",
                "payment-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void missingTokenReturnsStableMachineReadableError() throws Exception {
        MockHttpServletRequest request = internalRequest(
                "/internal/bookings/550e8400-e29b-41d4-a716-446655440000/payment-context",
                null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals("INTERNAL_TOKEN_MISSING", body.path("errorCode").asText());
        verify(chain, never()).doFilter(request, response);
    }

    private MockHttpServletRequest internalRequest(String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        if (token != null) {
            request.addHeader("X-Internal-Token", token);
        }
        return request;
    }
}
