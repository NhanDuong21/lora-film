package com.lorafilm.movie.integration.location.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.location.config.LocationProperties;
import com.lorafilm.movie.integration.location.dto.UpstreamLocationResponse;

import java.lang.reflect.Field;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class LocationClientTest {

    private LocationClient locationClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setup() throws Exception {
        LocationProperties properties = new LocationProperties();
        properties.setBaseUrl("https://location-api.nyanmovie.site");
        properties.setKey("test-key");

        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        // Since RestClient is builder-based and we want to use MockRestServiceServer
        // we inject the RestTemplate into a RestClient.
        RestClient restClient = RestClient.builder(restTemplate)
                .baseUrl("https://location-api.nyanmovie.site")
                .defaultHeader("x-api-key", "test-key")
                .build();
                
        locationClient = new LocationClient(properties);
        
        // Inject mock restClient
        Field restClientField = LocationClient.class.getDeclaredField("restClient");
        restClientField.setAccessible(true);
        restClientField.set(locationClient, restClient);
    }

    @Test
    public void testFetchSuggestions_Success() {
        mockServer.expect(requestTo("https://location-api.nyanmovie.site/api/v1/address/suggest?q=Can%20Tho"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-api-key", "test-key"))
                .andRespond(withSuccess("{\"success\":true,\"data\":[{\"label\":\"Can Tho\"}]}", MediaType.APPLICATION_JSON));

        UpstreamLocationResponse response = locationClient.fetchSuggestions("Can Tho", 5);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("Can Tho", response.getData().get(0).getLabel());
        mockServer.verify();
    }

    @Test
    public void testFetchSuggestions_RateLimit() {
        mockServer.expect(requestTo("https://location-api.nyanmovie.site/api/v1/address/suggest?q=Can%20Tho"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            locationClient.fetchSuggestions("Can Tho", 5);
        });

        assertEquals(ErrorCode.LOCATION_API_RATE_LIMITED, exception.getErrorCode());
    }

    @Test
    public void testFetchSuggestions_ServerError() {
        mockServer.expect(requestTo("https://location-api.nyanmovie.site/api/v1/address/suggest?q=Can%20Tho"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            locationClient.fetchSuggestions("Can Tho", 5);
        });

        assertEquals(ErrorCode.LOCATION_API_UNAVAILABLE, exception.getErrorCode());
    }
}
