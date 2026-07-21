package com.lorafilm.movie.integration.location.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.location.client.LocationClient;
import com.lorafilm.movie.integration.location.dto.LocationSuggestion;
import com.lorafilm.movie.integration.location.dto.UpstreamLocationResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {
    
    private static final Logger log = LoggerFactory.getLogger(LocationService.class);
    private final LocationClient locationClient;

    public LocationService(LocationClient locationClient) {
        this.locationClient = locationClient;
    }

    public List<LocationSuggestion> getSuggestions(String query, int limit) {
        if (query == null || query.trim().length() < 2 || query.length() > 200) {
            throw new BusinessException(ErrorCode.LOCATION_QUERY_INVALID);
        }
        
        int finalLimit = Math.max(1, Math.min(limit, 10));
        
        UpstreamLocationResponse response = locationClient.fetchSuggestions(query.trim(), finalLimit);
        
        if (response == null || !response.isSuccess() || response.getData() == null) {
            log.warn("Invalid or empty response from Location API");
            return Collections.emptyList();
        }
        
        return response.getData().stream()
                .limit(finalLimit) // Enforce limit on backend just in case upstream ignores it
                .map(this::mapToLocationSuggestion)
                .collect(Collectors.toList());
    }
    
    private LocationSuggestion mapToLocationSuggestion(UpstreamLocationResponse.UpstreamSuggestion upstream) {
        LocationSuggestion suggestion = new LocationSuggestion();
        suggestion.setId(upstream.getId() != null ? upstream.getId() : generateStableId(upstream));
        suggestion.setLabel(upstream.getLabel());
        suggestion.setAddress(upstream.getAddress() != null ? upstream.getAddress() : upstream.getLabel());
        suggestion.setDistrict(upstream.getDistrict());
        suggestion.setCity(upstream.getCity());
        suggestion.setProvince(upstream.getProvince());
        suggestion.setCountry(upstream.getCountry());
        suggestion.setLatitude(upstream.getLatitude());
        suggestion.setLongitude(upstream.getLongitude());
        return suggestion;
    }
    
    private String generateStableId(UpstreamLocationResponse.UpstreamSuggestion upstream) {
        if (upstream.getLatitude() != null && upstream.getLongitude() != null) {
            return upstream.getLatitude() + "," + upstream.getLongitude();
        }
        return java.util.UUID.randomUUID().toString();
    }
}
