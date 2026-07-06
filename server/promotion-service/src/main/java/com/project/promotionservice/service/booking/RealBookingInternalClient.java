package com.project.promotionservice.service.booking;

import com.project.promotionservice.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Profile("!test")
public class RealBookingInternalClient implements BookingInternalClient {

    private final RestTemplate restTemplate;
    private final String bookingServiceUrl;
    private final String internalToken;

    public RealBookingInternalClient(RestTemplate restTemplate,
                                     @Value("${booking-service.url:http://localhost:8083}") String bookingServiceUrl,
                                     @Value("${app.internal-token:secret-internal-token}") String internalToken) {
        this.restTemplate = restTemplate;
        this.bookingServiceUrl = bookingServiceUrl;
        this.internalToken = internalToken;
    }

    @Override
    public BookingContext getBookingContext(Long bookingId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", internalToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = bookingServiceUrl + "/internal/bookings/" + bookingId + "/payment-context";

            ResponseEntity<BookingContextApiResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    BookingContextApiResponse.class
            );

            BookingContextApiResponse body = responseEntity.getBody();
            if (body != null && body.isSuccess() && body.getData() != null) {
                PaymentContextResponse data = body.getData();
                return BookingContext.builder()
                        .bookingId(data.getBookingId())
                        .userId(data.getAccountId())
                        .status(data.getBookingStatus())
                        .expiresAt(data.getExpiresAt())
                        .amount(data.getAmount())
                        .build();
            }
            throw new BusinessException("Failed to retrieve booking details", "BOOKING_SERVICE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (HttpClientErrorException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                throw new BusinessException("Booking not found", "PROMOTION_BOOKING_NOT_FOUND", status);
            }
            throw new BusinessException("Booking service error: " + e.getMessage(), "BOOKING_SERVICE_UNAVAILABLE", status);
        } catch (Exception e) {
            throw new BusinessException("Booking service is down or timed out", "BOOKING_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static class BookingContextApiResponse {
        private boolean success;
        private String message;
        private String errorCode;
        private PaymentContextResponse data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public PaymentContextResponse getData() { return data; }
        public void setData(PaymentContextResponse data) { this.data = data; }
    }

    private static class PaymentContextResponse {
        private Long bookingId;
        private Long accountId;
        private String bookingStatus;
        private BigDecimal amount;
        private LocalDateTime expiresAt;

        public Long getBookingId() { return bookingId; }
        public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        public String getBookingStatus() { return bookingStatus; }
        public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    }
}
