package com.lorafilm.booking.payment.controller;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.payment.event.PaymentEventConsumer;
import com.lorafilm.booking.payment.event.contract.PaymentEvent;
import com.lorafilm.booking.payment.event.contract.PaymentEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/mock/payment")
@Profile({"local", "test"})
public class MockPaymentController {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentController.class);

    private final BookingRepository bookingRepository;
    private final PaymentEventConsumer consumer;
    private final ObjectMapper objectMapper;

    public MockPaymentController(
            BookingRepository bookingRepository,
            PaymentEventConsumer consumer,
            ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.consumer = consumer;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/page", produces = MediaType.TEXT_HTML_VALUE)
    public String getMockPaymentPage(@RequestParam String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found: " + bookingCode));

        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Mock Payment Gateway</title>
            <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #121212; color: #e0e0e0; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                .card { background: #1e1e1e; padding: 40px; border-radius: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.5); width: 450px; text-align: center; border: 1px solid #333; }
                h2 { color: #bb86fc; margin-bottom: 20px; }
                p { margin: 10px 0; font-size: 16px; }
                .amount { font-size: 24px; font-weight: bold; color: #03dac6; margin: 20px 0; }
                .btn { display: block; width: 100%; padding: 12px; margin: 12px 0; border: none; border-radius: 6px; font-size: 16px; font-weight: bold; cursor: pointer; transition: background 0.2s; }
                .btn-success { background: #03dac6; color: #000; }
                .btn-success:hover { background: #018786; }
                .btn-fail { background: #cf6679; color: #fff; }
                .btn-fail:hover { background: #b00020; }
                .btn-expire { background: #f2a900; color: #000; }
                .btn-expire:hover { background: #c78b00; }
            </style>
            <script>
                async function sendSimulation(type) {
                    try {
                        const response = await fetch('/internal/mock/payment/' + type, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ bookingCode: '%s' })
                        });
                        const data = await response.json();
                        alert('Simulation ' + type.toUpperCase() + ' finished: ' + data.message);
                    } catch (e) {
                        alert('Error during simulation: ' + e);
                    }
                }
            </script>
        </head>
        <body>
            <div class="card">
                <h2>Cinema Mock Payment</h2>
                <p><strong>Booking Code:</strong> %s</p>
                <p><strong>Status:</strong> %s</p>
                <div class="amount">%s %s</div>
                <button class="btn btn-success" onclick="sendSimulation('success')">Pay Success</button>
                <button class="btn btn-fail" onclick="sendSimulation('fail')">Pay Fail</button>
                <button class="btn btn-expire" onclick="sendSimulation('expire')">Simulate Expiration</button>
            </div>
        </body>
        </html>
        """;

        return String.format(html,
            booking.getBookingCode(),
            booking.getBookingCode(),
            booking.getBookingStatus().name(),
            booking.getFinalAmount().toString(),
            booking.getCurrency()
        );
    }


    @PostMapping("/success")
    public ResponseEntity<ApiResponse<Map<String, String>>> simulateSuccess(@RequestBody Map<String, String> request) {
        String bookingCode = request.get("bookingCode");
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found: " + bookingCode));

        PaymentEvent event = createEvent("PAYMENT_SUCCESS", booking, "SUCCESS", null);
        try {
            consumer.consume(objectMapper.writeValueAsString(event));
            return ResponseEntity.ok(ApiResponse.success("SUCCESS payment simulation processed successfully", Map.of("bookingCode", bookingCode)));
        } catch (Exception e) {
            log.error("Failed to process success simulation", e);
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/fail")
    public ResponseEntity<ApiResponse<Map<String, String>>> simulateFail(@RequestBody Map<String, String> request) {
        String bookingCode = request.get("bookingCode");
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found: " + bookingCode));

        PaymentEvent event = createEvent("PAYMENT_FAILED", booking, "FAILED", "Gateway rejection or insufficient funds");
        try {
            consumer.consume(objectMapper.writeValueAsString(event));
            return ResponseEntity.ok(ApiResponse.success("FAILED payment simulation processed successfully", Map.of("bookingCode", bookingCode)));
        } catch (Exception e) {
            log.error("Failed to process fail simulation", e);
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/expire")
    public ResponseEntity<ApiResponse<Map<String, String>>> simulateExpire(@RequestBody Map<String, String> request) {
        String bookingCode = request.get("bookingCode");
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found: " + bookingCode));

        PaymentEvent event = createEvent("PAYMENT_EXPIRED", booking, "EXPIRED", "Payment deadline elapsed");
        try {
            consumer.consume(objectMapper.writeValueAsString(event));
            return ResponseEntity.ok(ApiResponse.success("EXPIRED payment simulation processed successfully", Map.of("bookingCode", bookingCode)));
        } catch (Exception e) {
            log.error("Failed to process expire simulation", e);
            throw new RuntimeException(e);
        }
    }

    private PaymentEvent createEvent(String eventType, Booking booking, String paymentStatus, String errorMsg) {
        String txnCode = booking.getPaymentReference() != null ? booking.getPaymentReference() : "MOCK-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentEventPayload payload = new PaymentEventPayload(
            System.currentTimeMillis(),
            booking.getId(),
            txnCode,
            booking.getPaymentMethodSnapshot() != null ? booking.getPaymentMethodSnapshot() : "MOCK_PAY",
            paymentStatus,
            booking.getFinalAmount(),
            booking.getCurrency(),
            "MOCK-GATEWAY-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            errorMsg != null ? "GATEWAY_ERROR" : null,
            errorMsg
        );

        return new PaymentEvent(
            UUID.randomUUID().toString(),
            eventType,
            String.valueOf(booking.getId()),
            "BOOKING",
            1,
            Instant.now(),
            booking.getPaymentProvider() != null ? booking.getPaymentProvider() : "mock-payment-service",
            "v1.0",
            UUID.randomUUID().toString(),
            payload
        );
    }
}
