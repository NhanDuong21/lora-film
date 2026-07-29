package com.project.apigateway;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayFallbackController {

    @RequestMapping("/gateway-fallback/tmdb")
    public ResponseEntity<Map<String, Object>> tmdbImportFallback() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", "TMDB import service is temporarily unavailable");
        response.put("errorCode", "TMDB_SERVICE_UNAVAILABLE");
        response.put("data", null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
