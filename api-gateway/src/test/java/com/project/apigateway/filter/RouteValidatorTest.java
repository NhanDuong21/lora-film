package com.project.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RouteValidatorTest {

    private final RouteValidator validator = new RouteValidator();

    @org.junit.jupiter.api.Test
    void publicPeopleCatalogIsOpenForGuestGetRequests() {
        org.springframework.mock.http.server.reactive.MockServerHttpRequest request =
                org.springframework.mock.http.server.reactive.MockServerHttpRequest
                        .get("/api/public/people?role=ACTOR")
                        .build();

        org.junit.jupiter.api.Assertions.assertFalse(validator.isSecured.test(request));
    }

    @Test
    void registrationStatusAndOAuthArePublic() {
        assertThat(isSecured(HttpMethod.GET,
                "/api/auth/registrations/a1b2c3d4-e5f6-7890-abcd-ef1234567890/status")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/oauth2/authorization/google")).isFalse();
        assertThat(isSecured(HttpMethod.POST, "/api/auth/identity-number/inspect")).isFalse();
    }

    @Test
    void optionsAndPublicAvatarAreNotBlockedByGatewayAuthentication() {
        assertThat(isSecured(HttpMethod.OPTIONS, "/api/users/1")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/users/profile/avatar/files/avatar.jpg")).isFalse();
    }

    @Test
    void socketIoHandshakeIsPublicBecauseItOnlyProjectsSeatAvailability() {
        assertThat(isSecured(HttpMethod.GET,
                "/socket.io/?EIO=4&transport=websocket")).isFalse();
    }

    @Test
    void protectedUserAndAdminEndpointsRequireAuthentication() {
        assertThat(isSecured(HttpMethod.GET, "/api/users/1")).isTrue();
        assertThat(isSecured(HttpMethod.GET, "/api/accounts")).isTrue();
        assertThat(isSecured(HttpMethod.GET, "/api/admin/user-audits")).isTrue();
        assertThat(isSecured(HttpMethod.POST, "/api/customer/movies")).isTrue();
    }

    @Test
    void publicCatalogReadsDoNotRequireAuthentication() {
        assertThat(isSecured(HttpMethod.GET, "/api/customer/movies")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/customer/movies/movie-1/booking-options")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/cinemas/cinema-1")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/showtimes/showtime-1")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/customer/concessions")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/membership-tiers")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/promotions/public")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/promotions/public?size=6")).isFalse();
    }

    @Test
    void paymentProviderCallbacksAndReturnsDoNotRequireCustomerJwt() {
        assertThat(isSecured(HttpMethod.GET, "/api/payments/callback/vnpay")).isFalse();
        assertThat(isSecured(HttpMethod.POST, "/api/payments/callback/momo")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/payments/return/vnpay")).isFalse();
    }

    private boolean isSecured(HttpMethod method, String path) {
        return validator.isSecured.test(MockServerHttpRequest.method(method, path).build());
    }
}
