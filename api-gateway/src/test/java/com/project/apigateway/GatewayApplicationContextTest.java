package com.project.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationContextTest {
    @Autowired
    private RouteLocator routeLocator;

    @Test
    void createsTmdbRouteWithConfiguredGatewayFilters() {
        assertThat(routeLocator.getRoutes()
                .filter(route -> "tmdb-test".equals(route.getId()))
                .collectList()
                .block())
                .hasSize(1);
    }
}
