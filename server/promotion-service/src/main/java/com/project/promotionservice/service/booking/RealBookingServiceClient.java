package com.project.promotionservice.service.booking;

import com.project.promotionservice.dto.BookingResponse;
import com.project.promotionservice.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@Profile("!test")
public class RealBookingServiceClient implements BookingServiceClient {

    private final RestTemplate restTemplate;
    private final String bookingServiceUrl;

    public RealBookingServiceClient(RestTemplate restTemplate,
                                    @Value("${booking-service.url:http://localhost:8083}") String bookingServiceUrl) {
        this.restTemplate = restTemplate;
        this.bookingServiceUrl = bookingServiceUrl;
    }

    @Override
    public BookingResponse getBooking(Long bookingId, String authHeader) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = bookingServiceUrl + "/api/bookings/" + bookingId;

            ResponseEntity<BookingApiResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    BookingApiResponse.class
            );

            BookingApiResponse body = responseEntity.getBody();
            if (body != null && body.isSuccess()) {
                return body.getData();
            }

            String errorCode = body != null ? body.getErrorCode() : "BOOKING_SERVICE_ERROR";
            String msg = body != null ? body.getMessage() : "Failed to retrieve booking details";
            throw new BusinessException(msg, errorCode, HttpStatus.valueOf(responseEntity.getStatusCode().value()));

        } catch (HttpClientErrorException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                throw new BusinessException("Booking not found", "PROMOTION_BOOKING_NOT_FOUND", status);
            } else if (status == HttpStatus.FORBIDDEN) {
                throw new BusinessException("Booking does not belong to the user", "PROMOTION_BOOKING_OWNERSHIP_MISMATCH", status);
            } else if (status == HttpStatus.BAD_REQUEST) {
                throw new BusinessException("Invalid booking request", "PROMOTION_BOOKING_NOT_ELIGIBLE", status);
            }
            throw new BusinessException("Booking service error: " + e.getMessage(), "BOOKING_SERVICE_ERROR", status);
        } catch (HttpServerErrorException e) {
            throw new BusinessException("Booking service server error", "BOOKING_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw new BusinessException("Booking service is down or timed out", "BOOKING_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Unexpected error when calling booking service", "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static class BookingApiResponse {
        private boolean success;
        private String message;
        private String errorCode;
        private BookingResponse data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public BookingResponse getData() { return data; }
        public void setData(BookingResponse data) { this.data = data; }
    }
}
