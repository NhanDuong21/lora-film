package com.project.userservice.client;

import com.project.userservice.exception.BusinessException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CinemaDirectoryClient {

    private final RestClient restClient;

    @Autowired
    public CinemaDirectoryClient(
            @Value("${services.movie-service.url:http://localhost:8082}") String baseUrl,
            @Value("${services.movie-service.timeout-millis:1500}") long timeoutMillis,
            @Value("${app.internal-token}") String internalToken) {
        Duration timeout = Duration.ofMillis(Math.max(100, timeoutMillis));
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .requestFactory(requestFactory)
                .build();
    }

    CinemaDirectoryClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void requireExisting(String cinemaPublicId) {
        if (cinemaPublicId == null || cinemaPublicId.isBlank()) {
            throw new BusinessException("Vui lòng chọn rạp làm việc", "USER_CINEMA_REQUIRED");
        }
        try {
            CinemaEnvelope response = restClient.get()
                    .uri("/internal/cinemas/{publicId}/exists", cinemaPublicId)
                    .retrieve()
                    .body(CinemaEnvelope.class);
            if (response == null || !response.success() || response.data() == null) {
                throw unavailable();
            }
            if (!response.data().exists()) {
                throw new BusinessException("Rạp đã chọn không tồn tại hoặc đã ngừng sử dụng",
                        "USER_CINEMA_NOT_FOUND");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable();
        }
    }

    private BusinessException unavailable() {
        return new BusinessException("Chưa thể kiểm tra thông tin rạp. Vui lòng thử lại sau.",
                "USER_CINEMA_VALIDATION_UNAVAILABLE");
    }

    private record CinemaEnvelope(boolean success, CinemaExistence data) {
    }

    private record CinemaExistence(boolean exists) {
    }
}
