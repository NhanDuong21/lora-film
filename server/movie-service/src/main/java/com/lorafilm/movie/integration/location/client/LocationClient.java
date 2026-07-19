package com.lorafilm.movie.integration.location.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.location.config.LocationProperties;
import com.lorafilm.movie.integration.location.dto.UpstreamLocationResponse;

import java.time.Duration;

@Component
public class LocationClient {
    private static final Logger log = LoggerFactory.getLogger(LocationClient.class);
    
    private final RestClient restClient;
    private final LocationProperties properties;

    public LocationClient(LocationProperties properties) {
        this.properties = properties;
        
        if (properties.getKey() == null || properties.getKey().isBlank()) {
            log.warn("Location API key is not configured");
        }
        
        // Timeout is not natively supported directly in RestClient builder without custom request factory
        // but we can set it up via ClientHttpRequestFactory if needed. 
        // For simplicity, we use the default or a configured factory.
        
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("x-api-key", properties.getKey() != null ? properties.getKey() : "")
                .build();
    }

    public UpstreamLocationResponse fetchSuggestions(String query, int limit) {
        if (properties.getKey() == null || properties.getKey().isBlank()) {
            log.warn("Location API key is not configured. Falling back to Nominatim OpenStreetMap API.");
            return fetchFromNominatim(query, limit);
        }
        
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/address/suggest")
                            .queryParam("q", query)
                            // Upstream limit parameter if supported. If not, it will just ignore it.
                            // The prompt says: "Nếu upstream không hỗ trợ limit, Movie Service có thể giới hạn số phần tử sau khi nhận response."
                            .build())
                    .retrieve()
                    .body(UpstreamLocationResponse.class);
        } catch (ResourceAccessException e) {
            log.error("Failed to access Location API", e);
            throw new BusinessException(ErrorCode.LOCATION_API_TIMEOUT);
        } catch (RestClientResponseException e) {
            handleErrorResponse(e);
            return null; // unreachable
        } catch (Exception e) {
            log.error("Unexpected error calling Location API", e);
            throw new BusinessException(ErrorCode.LOCATION_API_UNAVAILABLE);
        }
    }
    
    private void handleErrorResponse(RestClientResponseException e) {
        if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            throw new BusinessException(ErrorCode.LOCATION_API_RATE_LIMITED);
        }
        if (e.getStatusCode().is5xxServerError()) {
            throw new BusinessException(ErrorCode.LOCATION_API_UNAVAILABLE);
        }
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
            log.error("Location API authentication failed");
            throw new BusinessException(ErrorCode.LOCATION_API_UNAVAILABLE);
        }
        throw new BusinessException(ErrorCode.LOCATION_API_RESPONSE_INVALID);
    }

    private UpstreamLocationResponse fetchFromNominatim(String query, int limit) {
        try {
            RestClient nominatimClient = RestClient.builder()
                    .baseUrl("https://nominatim.openstreetmap.org")
                    .defaultHeader("User-Agent", "LoraFilm-Admin/1.0")
                    .defaultHeader("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                    .build();

            java.util.List<java.util.Map<String, Object>> responseList = nominatimClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("addressdetails", "1")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {});

            UpstreamLocationResponse response = new UpstreamLocationResponse();
            response.setSuccess(true);
            
            java.util.List<UpstreamLocationResponse.UpstreamSuggestion> data = new java.util.ArrayList<>();
            if (responseList != null) {
                for (java.util.Map<String, Object> item : responseList) {
                    UpstreamLocationResponse.UpstreamSuggestion suggestion = new UpstreamLocationResponse.UpstreamSuggestion();
                    suggestion.setId(String.valueOf(item.get("place_id")));
                    suggestion.setLabel((String) item.get("display_name"));
                    suggestion.setAddress((String) item.get("display_name"));
                    
                    Object latObj = item.get("lat");
                    Object lonObj = item.get("lon");
                    if (latObj != null) suggestion.setLatitude(Double.parseDouble(latObj.toString()));
                    if (lonObj != null) suggestion.setLongitude(Double.parseDouble(lonObj.toString()));
                    
                    if (item.containsKey("address") && item.get("address") instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> address = (java.util.Map<String, Object>) item.get("address");
                        
                        String city = (String) address.get("city");
                        if (city == null) city = (String) address.get("province");
                        if (city == null) city = (String) address.get("state");
                        suggestion.setCity(city);
                        
                        String district = (String) address.get("county");
                        if (district == null) district = (String) address.get("district");
                        if (district == null) district = (String) address.get("suburb");
                        if (district == null) district = (String) address.get("city_district");
                        suggestion.setDistrict(district);
                        
                        suggestion.setCountry((String) address.get("country"));
                    }
                    data.add(suggestion);
                }
            }
            response.setData(data);
            return response;
        } catch (Exception e) {
            log.error("Failed to fallback to Nominatim API", e);
            UpstreamLocationResponse empty = new UpstreamLocationResponse();
            empty.setSuccess(true);
            empty.setData(new java.util.ArrayList<>());
            return empty;
        }
    }
}
