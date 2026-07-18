package com.lorafilm.movie.integration.location.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.location.client.LocationClient;
import com.lorafilm.movie.integration.location.dto.LocationSuggestion;
import com.lorafilm.movie.integration.location.dto.UpstreamLocationResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class LocationServiceTest {

    private LocationClient locationClient;
    private LocationService locationService;

    @BeforeEach
    public void setup() {
        locationClient = Mockito.mock(LocationClient.class);
        locationService = new LocationService(locationClient);
    }

    @Test
    public void testGetSuggestions_Success() {
        UpstreamLocationResponse response = new UpstreamLocationResponse();
        response.setSuccess(true);
        UpstreamLocationResponse.UpstreamSuggestion suggestion = new UpstreamLocationResponse.UpstreamSuggestion();
        suggestion.setLabel("Test Address");
        response.setData(List.of(suggestion));

        when(locationClient.fetchSuggestions(anyString(), anyInt())).thenReturn(response);

        List<LocationSuggestion> suggestions = locationService.getSuggestions("Test", 5);

        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        assertEquals("Test Address", suggestions.get(0).getLabel());
    }

    @Test
    public void testGetSuggestions_InvalidQueryLength() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            locationService.getSuggestions("A", 5);
        });
        
        assertEquals(ErrorCode.LOCATION_QUERY_INVALID, exception.getErrorCode());
    }

    @Test
    public void testGetSuggestions_EmptyResponse() {
        UpstreamLocationResponse response = new UpstreamLocationResponse();
        response.setSuccess(false);

        when(locationClient.fetchSuggestions(anyString(), anyInt())).thenReturn(response);

        List<LocationSuggestion> suggestions = locationService.getSuggestions("Test", 5);

        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
    }
}
