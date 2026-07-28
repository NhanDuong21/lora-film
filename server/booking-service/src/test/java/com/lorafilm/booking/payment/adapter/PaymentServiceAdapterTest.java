package com.lorafilm.booking.payment.adapter;

import com.lorafilm.booking.common.exception.IntegrationException;
import com.lorafilm.booking.payment.dto.PaymentRequestDto;
import com.lorafilm.booking.payment.dto.PaymentResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PaymentServiceAdapterTest {

    private MockRestServiceServer server;
    private PaymentServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://payment-service");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new PaymentServiceAdapter(builder.build());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer customer-jwt");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requestPaymentPropagatesSecurityAndMapsSuccessfulResponse() {
        server.expect(once(), requestTo("http://payment-service/api/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer customer-jwt"))
                .andExpect(header("Idempotency-Key", "booking-payment-42-MOCK"))
                .andExpect(content().json("""
                        {
                          "bookingId": 42,
                          "paymentMethod": "MOCK"
                        }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "success": true,
                                  "message": "Payment created successfully",
                                  "data": {
                                    "paymentId": 81,
                                    "paymentTransactionCode": "PAY-000081",
                                    "paymentMethod": "MOCK",
                                    "paymentUrl": "http://payment.example/81",
                                    "amount": 125000.00,
                                    "currency": "VND",
                                    "status": "PENDING"
                                  }
                                }
                                """));

        PaymentResponseDto response = adapter.requestPayment(paymentRequest());

        assertThat(response.paymentId()).isEqualTo(81L);
        assertThat(response.bookingId()).isEqualTo(42L);
        assertThat(response.transactionCode()).isEqualTo("PAY-000081");
        assertThat(response.paymentStatus()).isEqualTo("PENDING");
        assertThat(response.amount()).isEqualByComparingTo("125000.00");
        assertThat(response.currency()).isEqualTo("VND");
        assertThat(response.paymentUrl()).isEqualTo("http://payment.example/81");
        server.verify();
    }

    @Test
    void requestPaymentRejectsResponseWithDifferentAmount() {
        server.expect(once(), requestTo("http://payment-service/api/payments"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "success": true,
                                  "data": {
                                    "paymentId": 81,
                                    "paymentTransactionCode": "PAY-000081",
                                    "amount": 1,
                                    "currency": "VND",
                                    "status": "PENDING"
                                  }
                                }
                                """));

        assertThatThrownBy(() -> adapter.requestPayment(paymentRequest()))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("amount different");
        server.verify();
    }

    @Test
    void requestPaymentRequiresBearerToken() {
        RequestContextHolder.resetRequestAttributes();

        assertThatThrownBy(() -> adapter.requestPayment(paymentRequest()))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("authorization is unavailable");
    }

    private PaymentRequestDto paymentRequest() {
        return new PaymentRequestDto(
                42L,
                "BK-42",
                new BigDecimal("125000.00"),
                "VND",
                "mock",
                "MOCK",
                9L,
                Instant.now().plusSeconds(600));
    }

}
