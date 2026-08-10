package com.project.paymentservice.client.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.exception.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EmployeeCinemaScopeClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String internalToken;
    private final Duration timeout;

    public EmployeeCinemaScopeClient(
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

    public String requireActiveCinema(Long accountId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/internal/employees/"
                            + accountId + "/cinema-scope"))
                    .timeout(timeout)
                    .header("X-Internal-Token", internalToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw unavailable();
            JsonNode data = objectMapper.readTree(response.body()).path("data");
            String status = data.path("employeeStatus").asText();
            String cinema = data.path("cinemaPublicId").asText();
            if (!"ACTIVE".equals(status) || cinema.isBlank()) {
                throw new BusinessException(
                        "EMPLOYEE_CINEMA_NOT_ASSIGNED",
                        "Tài khoản nhân viên chưa được phân công rạp đang hoạt động.",
                        HttpStatus.FORBIDDEN);
            }
            return cinema.trim().toLowerCase(Locale.ROOT);
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(
                "USER_SERVICE_UNAVAILABLE",
                "Không thể xác định rạp làm việc của nhân viên.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
