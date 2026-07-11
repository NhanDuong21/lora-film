package com.project.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.client.booking.BookingPaymentClientImpl;
import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.exception.BusinessException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class BookingPaymentClientTest {

    private MockWebServer mockWebServer;
    private BookingPaymentClientImpl client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        String baseUrl = mockWebServer.url("/").toString();
        // Remove trailing slash to match typical baseUrl format
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        client = new BookingPaymentClientImpl(
                objectMapper, baseUrl, "test-token", 1000, 2000);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getPaymentContext_Success() {
        String json = """
            {
              "success": true,
              "data": {
                "bookingId": 1001,
                "accountId": 15,
                "bookingStatus": "RESERVED",
                "payable": true,
                "amount": 150000,
                "currency": "VND",
                "expiresAt": "%s",
                "analyticsSnapshot": {
                  "movieId": 1,
                  "movieTitle": "Dune 2",
                  "ticketCount": 2
                }
              }
            }
            """.formatted(LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ISO_DATE_TIME));

        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        BookingPaymentContext context = client.getPaymentContext(1001L);

        assertNotNull(context);
        assertEquals(1001L, context.getBookingId());
        assertEquals(15L, context.getAccountId());
        assertEquals(0, new BigDecimal("150000").compareTo(context.getAmount()));
        assertEquals("VND", context.getCurrency());
        assertEquals(1L, context.getAnalyticsSnapshot().getMovieId());
    }

    @Test
    void getPaymentContext_NotFound() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        BusinessException ex = assertThrows(BusinessException.class, () -> client.getPaymentContext(1001L));
        assertEquals("BOOKING_NOT_FOUND", ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void getPaymentContext_NotPayable() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(409));

        BusinessException ex = assertThrows(BusinessException.class, () -> client.getPaymentContext(1001L));
        assertEquals("BOOKING_NOT_PAYABLE", ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void getPaymentContext_Timeout() {
        // Enqueue no response, rely on client timeout (1000ms connect, 2000ms read)
        // Since it's a real connection to mockWebServer that hangs, we can mock a delayed response
        // Wait, socket timeout in mockWebServer:
        mockWebServer.enqueue(new MockResponse().setBody("{}").setHeadersDelay(3, java.util.concurrent.TimeUnit.SECONDS));

        BusinessException ex = assertThrows(BusinessException.class, () -> client.getPaymentContext(1001L));
        assertEquals("BOOKING_SERVICE_UNAVAILABLE", ex.getErrorCode());
    }
}
