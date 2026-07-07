package com.project.promotionservice.service.booking;

import com.project.promotionservice.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class RealBookingInternalClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private RealBookingInternalClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        client = new RealBookingInternalClient(restTemplate, "http://localhost:8083", "test-token");
    }

    @Test
    void getBookingContext_Success() {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Success\",\n" +
                "  \"data\": {\n" +
                "    \"bookingId\": 1,\n" +
                "    \"accountId\": 2,\n" +
                "    \"bookingStatus\": \"PENDING_PAYMENT\",\n" +
                "    \"amount\": 100000,\n" +
                "    \"expiresAt\": \"2026-07-10T12:00:00\"\n" +
                "  }\n" +
                "}";

        mockServer.expect(requestTo("http://localhost:8083/internal/bookings/1/payment-context"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        BookingContext context = client.getBookingContext(1L);

        assertThat(context).isNotNull();
        assertThat(context.getBookingId()).isEqualTo(1L);
        assertThat(context.getUserId()).isEqualTo(2L);
        assertThat(context.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(context.getAmount()).isEqualByComparingTo("100000");
        mockServer.verify();
    }

    @Test
    void getBookingContext_Timeout_ThrowsServiceUnavailable() {
        mockServer.expect(requestTo("http://localhost:8083/internal/bookings/1/payment-context"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Read timed out")));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            client.getBookingContext(1L);
        });

        assertThat(exception.getErrorCode()).isEqualTo("BOOKING_SERVICE_UNAVAILABLE");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        mockServer.verify();
    }
}
