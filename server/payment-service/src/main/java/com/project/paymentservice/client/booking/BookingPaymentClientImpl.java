package com.project.paymentservice.client.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class BookingPaymentClientImpl implements BookingPaymentClient {
    private static final Logger log = LoggerFactory.getLogger(BookingPaymentClientImpl.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String internalToken;
    private final Duration readTimeout;

    public BookingPaymentClientImpl(
            ObjectMapper objectMapper,
            @Value("${booking.service.base-url:http://localhost:8083}") String baseUrl,
            @Value("${booking.service.internal-token:}") String internalToken,
            @Value("${booking.service.connect-timeout:5000}") int connectTimeout,
            @Value("${booking.service.read-timeout:10000}") int readTimeout) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.internalToken = internalToken;
        this.readTimeout = Duration.ofMillis(readTimeout);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException("booking.service.internal-token must be configured");
        }
    }

    @Override
    public BookingPaymentContext getPaymentContext(Long bookingId) {
        return getContext("/internal/bookings/" + bookingId + "/payment-context");
    }

    @Override
    public BookingPaymentContext getPaymentContext(String bookingPublicId) {
        return getContext("/internal/bookings/" + bookingPublicId + "/payment-context");
    }

    @Override
    public BookingPaymentContext getPaymentContextByCode(String bookingCode) {
        String encoded = URLEncoder.encode(bookingCode, StandardCharsets.UTF_8);
        return getContext("/internal/bookings/code/" + encoded + "/payment-context");
    }

    @Override
    public BookingPaymentResultResponse notifyPaymentResult(
            String bookingPublicId, BookingPaymentResultRequest request) {
        try {
            HttpResponse<String> response = send(
                    "/internal/bookings/" + bookingPublicId + "/payment-results",
                    "POST",
                    objectMapper.writeValueAsString(request));
            if (response.statusCode() == 200) {
                return readData(response.body(), BookingPaymentResultResponse.class);
            }
            if (response.statusCode() == 409) {
                throw conflict(response.body());
            }
            throw mapError(response.statusCode(), response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    private BookingPaymentContext getContext(String path) {
        try {
            HttpResponse<String> response = send(path, "GET", null);
            if (response.statusCode() == 200) {
                BookingPaymentContext context = readData(response.body(), BookingPaymentContext.class);
                validateContext(context);
                return context;
            }
            throw mapError(response.statusCode(), response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    private HttpResponse<String> send(String path, String method, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(readTimeout)
                .header("Content-Type", "application/json")
                .header("X-Internal-Token", internalToken);
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.GET();
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private <T> T readData(String body, Class<T> type) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw new BusinessException(
                    "BOOKING_SERVICE_UNAVAILABLE",
                    "Booking Service returned empty data",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return objectMapper.treeToValue(data, type);
    }

    private void validateContext(BookingPaymentContext context) {
        Instant now = Instant.now();
        if (context == null
                || context.getBookingPublicId() == null
                || context.getAccountId() == null
                || context.getAmount() == null
                || context.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || context.getCurrency() == null
                || context.getAmountLockedAt() == null
                || context.getAmountLockedAt().isAfter(now)
                || context.getExpiresAt() == null
                || !context.getExpiresAt().isAfter(now)
                || !Boolean.TRUE.equals(context.getPayable())) {
            throw new BusinessException(
                    "BOOKING_NOT_PAYABLE",
                    "Booking chưa sẵn sàng thanh toán hoặc đã hết hạn",
                    HttpStatus.CONFLICT);
        }
        BookingPaymentContext.AnalyticsSnapshotData snapshot = context.getAnalyticsSnapshot();
        if (snapshot == null || snapshot.getMovieTitle() == null || snapshot.getMovieTitle().isBlank()
                || snapshot.getTicketCount() == null || snapshot.getTicketCount() <= 0
                || snapshot.getTotalAmount() == null
                || snapshot.getTotalAmount().compareTo(context.getAmount()) != 0) {
            throw new BusinessException(
                    "BOOKING_CONTEXT_INVALID",
                    "Booking Service trả về snapshot thanh toán không hợp lệ",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BusinessException conflict(String body) {
        String code = errorCode(body, "PAYMENT_RESULT_CONFLICT");
        return new BusinessException(code,
                "Booking yêu cầu đối soát kết quả thanh toán", HttpStatus.CONFLICT);
    }

    private BusinessException mapError(int status, String body) {
        if (status == 404) {
            return new BusinessException("BOOKING_NOT_FOUND", "Không tìm thấy đơn đặt vé", HttpStatus.NOT_FOUND);
        }
        if (status == 409) {
            return new BusinessException(errorCode(body, "BOOKING_NOT_PAYABLE"),
                    "Đơn đặt vé không còn khả dụng để thanh toán", HttpStatus.CONFLICT);
        }
        if (status == 401 || status == 403) {
            return new BusinessException("BOOKING_SERVICE_AUTH_FAILED",
                    "Xác thực nội bộ với Booking Service thất bại", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                "Booking Service trả về trạng thái " + status, HttpStatus.SERVICE_UNAVAILABLE);
    }

    private String errorCode(String body, String fallback) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String code = root.path("errorCode").asText();
            if (code.isBlank()) {
                code = root.path("code").asText();
            }
            return code.isBlank() ? fallback : code;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private BusinessException unavailable(Exception exception) {
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        log.warn("Booking Service communication failed: {}", exception.getMessage());
        return new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                "Không thể kết nối Booking Service", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
