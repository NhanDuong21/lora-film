package com.project.paymentservice.client.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.exception.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EmployeeDirectoryClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String internalToken;
    private final Duration timeout;

    public EmployeeDirectoryClient(
            ObjectMapper objectMapper,
            @Value("${user.service.base-url:http://localhost:8086}") String baseUrl,
            @Value("${user.service.internal-token:${APP_INTERNAL_TOKEN:8f0a00f11a51ad253c9560e55236b464bab6b20e57642c01a9c896a98ff061ff}}") String internalToken,
            @Value("${user.service.read-timeout:3000}") int readTimeout) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.internalToken = internalToken;
        this.timeout = Duration.ofMillis(readTimeout);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("user.service.internal-token must be configured");
        }
    }

    public Map<Long, EmployeeDirectoryEntry> findByAccountIds(Collection<Long> values) {
        LinkedHashSet<Long> accountIds = new LinkedHashSet<>();
        if (values != null) {
            values.stream().filter(value -> value != null && value > 0).forEach(accountIds::add);
        }
        if (accountIds.isEmpty()) return Map.of();

        try {
            String body = objectMapper.writeValueAsString(Map.of("accountIds", accountIds));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/internal/employees/directory"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", internalToken)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw unavailable();

            Map<Long, EmployeeDirectoryEntry> directory = new LinkedHashMap<>();
            for (JsonNode item : objectMapper.readTree(response.body()).path("data")) {
                Long accountId = item.path("accountId").canConvertToLong()
                        ? item.path("accountId").longValue() : null;
                if (accountId == null) continue;
                directory.put(accountId, new EmployeeDirectoryEntry(
                        accountId,
                        nullableText(item, "employeeCode"),
                        nullableText(item, "fullName"),
                        nullableText(item, "avatarUrl"),
                        nullableText(item, "positionCode"),
                        nullableText(item, "positionName")));
            }
            return directory;
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    private String nullableText(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value == null || value.isNull() || value.asText().isBlank()
                ? null : value.asText();
    }

    private BusinessException unavailable() {
        return new BusinessException(
                "USER_SERVICE_UNAVAILABLE",
                "Không thể tải thông tin nhân viên cho biên bản chốt ca.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
