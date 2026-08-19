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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        ReflectionTestUtils.setField(filter, "internalPromotionAuditToken", "promotion-audit-token");
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

    @Test
    void missingPromotionAuditTokenFailsStartupValidation() {
        InternalTokenFilter unconfigured = new InternalTokenFilter();
        ReflectionTestUtils.setField(unconfigured, "internalToken", "general-token");
        ReflectionTestUtils.setField(unconfigured, "internalPaymentToken", "payment-token");
        ReflectionTestUtils.setField(unconfigured, "internalPromotionAuditToken", "");

        assertThrows(IllegalStateException.class, unconfigured::init);
    }

    @Test
    void promotionAuditTokenCanOnlyReadLifecycleContext() throws Exception {
        String contextPath = "/internal/bookings/550e8400-e29b-41d4-a716-446655440000/lifecycle-context";
        MockHttpServletRequest contextRequest = internalRequest("GET", contextPath,
                "promotion-audit-token");
        MockHttpServletResponse contextResponse = new MockHttpServletResponse();
        FilterChain contextChain = mock(FilterChain.class);

        filter.doFilter(contextRequest, contextResponse, contextChain);

        verify(contextChain).doFilter(contextRequest, contextResponse);

        String paymentContextPath = "/internal/bookings/550e8400-e29b-41d4-a716-446655440000/payment-context";
        MockHttpServletRequest paymentContextRequest = internalRequest("GET", paymentContextPath,
                "promotion-audit-token");
        MockHttpServletResponse paymentContextResponse = new MockHttpServletResponse();
        FilterChain paymentContextChain = mock(FilterChain.class);

        filter.doFilter(paymentContextRequest, paymentContextResponse, paymentContextChain);

        assertEquals(403, paymentContextResponse.getStatus());
        verify(paymentContextChain, never()).doFilter(paymentContextRequest, paymentContextResponse);

        String refundPath = "/internal/bookings/550e8400-e29b-41d4-a716-446655440000/refund";
        MockHttpServletRequest refundRequest = internalRequest("POST", refundPath,
                "promotion-audit-token");
        MockHttpServletResponse refundResponse = new MockHttpServletResponse();
        FilterChain refundChain = mock(FilterChain.class);

        filter.doFilter(refundRequest, refundResponse, refundChain);

        assertEquals(403, refundResponse.getStatus());
        verify(refundChain, never()).doFilter(refundRequest, refundResponse);
    }

    private MockHttpServletRequest internalRequest(String path, String token) {
        return internalRequest("POST", path, token);
    }

    private MockHttpServletRequest internalRequest(
            String method, String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (token != null) {
            request.addHeader("X-Internal-Token", token);
        }
        return request;
    }
}
