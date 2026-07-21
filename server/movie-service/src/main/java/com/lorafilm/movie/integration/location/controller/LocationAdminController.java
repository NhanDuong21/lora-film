package com.lorafilm.movie.integration.location.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lorafilm.movie.integration.location.dto.LocationSuggestion;
import com.lorafilm.movie.integration.location.service.LocationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/locations")
public class LocationAdminController {

    private final LocationService locationService;

    public LocationAdminController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSuggestions(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "8") int limit) {
        
        List<LocationSuggestion> suggestions = locationService.getSuggestions(query, limit);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", suggestions
        ));
    }
}
