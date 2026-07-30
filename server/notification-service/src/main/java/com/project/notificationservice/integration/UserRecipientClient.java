package com.project.notificationservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.notificationservice.exception.NotificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
public class UserRecipientClient {

    private final RestClient restClient;

    public UserRecipientClient(
            RestClient.Builder builder,
            @Value("${notification.user-service.url:http://localhost:8086}") String userServiceUrl,
            @Value("${notification.user-service.internal-token}") String internalToken) {
        this.restClient = builder
                .baseUrl(userServiceUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    public Optional<ResolvedRecipient> findByUserPublicId(String userPublicId) {
        Long accountId = parseAccountId(userPublicId);
        if (accountId == null) return Optional.empty();
        try {
            JsonNode response = restClient.get()
                    .uri("/api/v1/internal/users/{accountId}/notification-recipient", accountId)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (data == null || data.isMissingNode() || data.isNull()) {
                return Optional.empty();
            }
            return Optional.of(new ResolvedRecipient(
                    text(data, "email"),
                    text(data, "fullName")));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw new NotificationException(
                    "USER_RECIPIENT_LOOKUP_FAILED",
                    "User Service is unavailable while resolving the email recipient",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private Long parseAccountId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        return value.isBlank() ? null : value;
    }

    public record ResolvedRecipient(String email, String fullName) {
    }
}
