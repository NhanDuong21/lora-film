package com.project.apigateway;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteConfigurationTest {

    @Test
    void tmdbRouteHasCircuitBreakerAndGlobalRateLimiter() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.example.properties")) {
            assertThat(input).isNotNull();
            properties.load(input);
        }

        assertThat(properties.getProperty("spring.cloud.gateway.routes[9].id"))
                .isEqualTo("tmdb-api");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[9].filters[2].name"))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[9].filters[2].args.name"))
                .isEqualTo("tmdbImport");
        assertThat(properties.getProperty("spring.cloud.gateway.default-filters[1].name"))
                .isEqualTo("RequestRateLimiter");
        assertThat(properties.getProperty("spring.cloud.gateway.default-filters[1].args.key-resolver"))
                .isEqualTo("#{@clientKeyResolver}");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[9].filters[0]"))
                .isEqualTo("AddRequestHeader=x-api-key,${TMDB_API_KEY}");

        assertThat(properties.getProperty("spring.cloud.gateway.routes[2].predicates[0]"))
                .contains("/api/admin/locations/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[3].predicates[0]"))
                .contains("/api/admin/coupons/**", "/api/admin/configurations/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[6].predicates[0]"))
                .contains("/api/employee/payments/**", "/api/admin/payments/**");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[10].uri"))
                .isEqualTo("${LOCATION_API_BASE_URL:https://location-api.nyanmovie.site}");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[10].filters[0]"))
                .isEqualTo("AddRequestHeader=x-api-key,${LOCATION_API_KEY}");
    }
}
