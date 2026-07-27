package com.project.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RouteValidatorTest {

    private final RouteValidator validator = new RouteValidator();

    @Test
    void registrationStatusAndOAuthArePublic() {
        assertThat(isSecured(HttpMethod.GET,
                "/api/auth/registrations/a1b2c3d4-e5f6-7890-abcd-ef1234567890/status")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/oauth2/authorization/google")).isFalse();
    }

    @Test
    void optionsAndPublicAvatarAreNotBlockedByGatewayAuthentication() {
        assertThat(isSecured(HttpMethod.OPTIONS, "/api/users/1")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/users/profile/avatar/files/avatar.jpg")).isFalse();
    }

    @Test
    void protectedUserAndAdminEndpointsRequireAuthentication() {
        assertThat(isSecured(HttpMethod.GET, "/api/users/1")).isTrue();
        assertThat(isSecured(HttpMethod.GET, "/api/accounts")).isTrue();
        assertThat(isSecured(HttpMethod.POST, "/api/customer/movies")).isTrue();
    }

    @Test
    void publicCatalogReadsDoNotRequireAuthentication() {
        assertThat(isSecured(HttpMethod.GET, "/api/customer/movies")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/customer/movies/movie-1/booking-options")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/cinemas/cinema-1")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/showtimes/showtime-1")).isFalse();
        assertThat(isSecured(HttpMethod.GET, "/api/customer/concessions")).isFalse();
    }

    private boolean isSecured(HttpMethod method, String path) {
        return validator.isSecured.test(MockServerHttpRequest.method(method, path).build());
    }
}
