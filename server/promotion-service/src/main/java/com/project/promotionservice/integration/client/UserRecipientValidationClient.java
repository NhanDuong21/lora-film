package com.project.promotionservice.integration.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class UserRecipientValidationClient {

    private final RestClient restClient;

    public UserRecipientValidationClient(
            RestClient.Builder builder,
            @Value("${promotion.user-service.url:http://localhost:8086}") String serviceUrl,
            @Value("${promotion.user-service.internal-token:${USER_SERVICE_INTERNAL_TOKEN:}}")
            String internalToken) {
        this.restClient = builder.baseUrl(serviceUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    public void requireAllActive(List<String> userPublicIds) {
        requireAllActive(userPublicIds, false);
    }

    public void requireAllActive(
            List<String> userPublicIds, boolean testAccountsOnly) {
        List<Long> requested = accountIds(userPublicIds);
        Set<Long> found = activeAccountIds(requested, testAccountsOnly);
        List<Long> missing = requested.stream()
                .filter(id -> !found.contains(id))
                .distinct()
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(
                    testAccountsOnly
                            ? "PROMOTION_TEST_RECIPIENT_REQUIRED"
                            : "PROMOTION_RECIPIENT_NOT_FOUND",
                    testAccountsOnly
                            ? "UAT benefits can only be issued to active test accounts: " + missing
                            : "Promotion recipients are missing or inactive: " + missing,
                    HttpStatus.BAD_REQUEST);
        }
    }

    public boolean isActiveTestAccount(String userPublicId) {
        List<Long> requested = accountIds(List.of(userPublicId));
        return activeAccountIds(requested, true).contains(requested.getFirst());
    }

    private List<Long> accountIds(List<String> userPublicIds) {
        try {
            return userPublicIds.stream().map(Long::valueOf).toList();
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    "PROMOTION_RECIPIENT_INVALID",
                    "Promotion recipients must be numeric account IDs",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Set<Long> activeAccountIds(
            List<Long> requested, boolean testAccountsOnly) {
        try {
            JsonNode response = restClient.post()
                    .uri("/api/v1/internal/users/validate-active")
                    .body(java.util.Map.of(
                            "accountIds", requested,
                            "testAccountsOnly", testAccountsOnly))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            Set<Long> found = new LinkedHashSet<>();
            if (data != null && data.isArray()) {
                data.forEach(item -> found.add(item.longValue()));
            }
            return found;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(
                    "USER_SERVICE_UNAVAILABLE",
                    "User Service is unavailable while validating promotion recipients",
                    HttpStatus.BAD_GATEWAY);
        }
    }
}
