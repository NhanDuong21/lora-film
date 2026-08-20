package com.project.promotionservice.automation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class BirthdayAudienceClient {
    private static final int PAGE_SIZE = 500;
    private final RestClient restClient;

    public BirthdayAudienceClient(
            RestClient.Builder builder,
            @Value("${promotion.user-service.url:http://localhost:8086}") String serviceUrl,
            @Value("${promotion.user-service.internal-token:${USER_SERVICE_INTERNAL_TOKEN:}}")
            String internalToken) {
        this.restClient = builder.baseUrl(serviceUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    public List<String> findEligible(LocalDate date, int limit) {
        List<String> result = new ArrayList<>();
        try {
            for (int page = 0; result.size() < limit; page++) {
                int pageNumber = page;
                JsonNode response = restClient.get()
                        .uri(uri -> uri.path("/api/v1/internal/users/birthday-eligible")
                                .queryParam("date", date)
                                .queryParam("page", pageNumber)
                                .queryParam("size", PAGE_SIZE)
                                .build())
                        .retrieve().body(JsonNode.class);
                JsonNode data = response == null ? null : response.path("data");
                if (data == null || !data.isArray()) break;
                int before = result.size();
                data.forEach(item -> {
                    if (result.size() < limit && item.path("customerId").canConvertToLong()) {
                        result.add(item.path("customerId").asText());
                    }
                });
                if (data.size() < PAGE_SIZE || result.size() == before) break;
            }
            return List.copyOf(result);
        } catch (RestClientException exception) {
            throw new BusinessException(
                    "USER_SERVICE_UNAVAILABLE",
                    "User Service is unavailable while building the birthday audience",
                    HttpStatus.BAD_GATEWAY);
        }
    }
}
