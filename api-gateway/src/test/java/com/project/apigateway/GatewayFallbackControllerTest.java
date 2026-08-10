package com.project.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayFallbackControllerTest {

    @Test
    void tmdbFallbackPreservesGatewayErrorResponseShape() {
        var response = new GatewayFallbackController().tmdbImportFallback();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsAllEntriesOf(Map.of(
                "success", false,
                "message", "TMDB import service is temporarily unavailable",
                "errorCode", "TMDB_SERVICE_UNAVAILABLE"));
        assertThat(response.getBody()).containsKey("data");
        assertThat(response.getBody().get("data")).isNull();
    }
}
