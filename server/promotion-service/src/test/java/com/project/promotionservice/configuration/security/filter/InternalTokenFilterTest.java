package com.project.promotionservice.configuration.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.configuration.security.principal.InternalServicePrincipal;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalTokenFilterTest {

    private static final String BOOKING_TOKEN = "booking-token-unique";
    private static final String PAYMENT_TOKEN = "payment-token-unique";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startupRejectsSharedServiceToken() {
        InternalTokenFilter filter = new InternalTokenFilter(
                new ObjectMapper().findAndRegisterModules(),
                BOOKING_TOKEN, BOOKING_TOKEN, "");

        assertThatThrownBy(filter::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not share");
    }

    @Test
    void internalRequestRequiresExactlyOneIdentityHeaderPair() throws Exception {
        InternalTokenFilter filter = filter();
        MockHttpServletRequest request = internalRequest();
        request.addHeader("X-Service-Name", "BOOKING_SERVICE");
        request.addHeader("X-Service-Name", "PAYMENT_SERVICE");
        request.addHeader("X-Internal-Token", BOOKING_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("Rejected request must not reach the controller");
        });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void tokenIsBoundToTheDeclaredServiceIdentity() throws Exception {
        InternalTokenFilter filter = filter();
        MockHttpServletRequest request = internalRequest();
        request.addHeader("X-Service-Name", "PAYMENT_SERVICE");
        request.addHeader("X-Internal-Token", BOOKING_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("Rejected request must not reach the controller");
        });

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void validIdentityCreatesServicePrincipalAndRole() throws Exception {
        InternalTokenFilter filter = filter();
        MockHttpServletRequest request = internalRequest();
        request.addHeader("X-Service-Name", "booking-service");
        request.addHeader("X-Internal-Token", BOOKING_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        FilterChain chain = (req, res) ->
                authentication.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getPrincipal())
                .isInstanceOfSatisfying(
                        InternalServicePrincipal.class,
                        principal -> assertThat(principal.getServiceName())
                                .isEqualTo("BOOKING_SERVICE"));
        assertThat(authentication.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_INTERNAL", "ROLE_BOOKING_SERVICE");
    }

    private InternalTokenFilter filter() {
        InternalTokenFilter filter = new InternalTokenFilter(
                new ObjectMapper().findAndRegisterModules(),
                BOOKING_TOKEN, PAYMENT_TOKEN, "operations-token-unique");
        filter.init();
        return filter;
    }

    private MockHttpServletRequest internalRequest() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/internal/reservations");
        request.setServletPath("/internal/reservations");
        return request;
    }
}
