package com.project.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
import com.project.paymentservice.dto.request.MockCallbackRequest;
import com.project.paymentservice.entity.BookingPaymentGuard;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.repository.BookingPaymentGuardRepository;
import com.project.paymentservice.repository.PaymentIdempotencyRecordRepository;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.repository.PaymentAnalyticsSnapshotRepository;
import com.project.paymentservice.repository.PaymentLogRepository;
import com.project.paymentservice.security.CurrentUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller testing
@ActiveProfiles("test")
public class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingPaymentGuardRepository guardRepository;

    @Autowired
    private PaymentIdempotencyRecordRepository idempotencyRepository;

    @Autowired
    private PaymentAnalyticsSnapshotRepository snapshotRepository;

    @Autowired
    private PaymentLogRepository paymentLogRepository;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @MockBean
    private BookingPaymentClient bookingClient;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(15L);
        snapshotRepository.deleteAllInBatch();
        paymentLogRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        idempotencyRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    @AfterEach
    void tearDown() {
        snapshotRepository.deleteAllInBatch();
        paymentLogRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        idempotencyRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    private BookingPaymentContext mockValidContext() {
        BookingPaymentContext context = new BookingPaymentContext();
        context.setBookingId(1001L);
        context.setAccountId(15L);
        context.setBookingStatus("RESERVED");
        context.setPayable(true);
        context.setAmount(new BigDecimal("150000"));
        context.setCurrency("VND");
        context.setExpiresAt(java.time.Instant.now().plusSeconds(900));

        BookingPaymentContext.AnalyticsSnapshotData snapshot = new BookingPaymentContext.AnalyticsSnapshotData();
        snapshot.setMovieId(1L);
        snapshot.setMovieTitle("Dune 2");
        snapshot.setTicketCount(2);
        context.setAnalyticsSnapshot(snapshot);
        return context;
    }

    @Test
    void createPayment_Success() throws Exception {
        when(bookingClient.getPaymentContext(1001L)).thenReturn(mockValidContext());

        CreatePaymentRequest req = new CreatePaymentRequest(1001L, "MOCK");

        mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentMethod").value("MOCK"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertEquals(1, paymentRepository.count());
        assertEquals(1, guardRepository.count());
        assertEquals(1, idempotencyRepository.count());
    }

    @Test
    void createPayment_IdempotentReplay() throws Exception {
        when(bookingClient.getPaymentContext(1001L)).thenReturn(mockValidContext());
        CreatePaymentRequest req = new CreatePaymentRequest(1001L, "MOCK");

        // First call
        String resp1 = mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Second call with same key
        String resp2 = mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals(resp1, resp2);
        assertEquals(1, paymentRepository.count(), "Should not create duplicate payment");
    }

    @Test
    void createPayment_IdempotencyKeyReused() throws Exception {
        when(bookingClient.getPaymentContext(anyLong())).thenReturn(mockValidContext());

        mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreatePaymentRequest(1001L, "MOCK"))))
                .andExpect(status().isCreated());

        // Same key, different request (booking 1002)
        mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreatePaymentRequest(1002L, "MOCK"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void mockCallback_Success() throws Exception {
        when(bookingClient.getPaymentContext(1001L)).thenReturn(mockValidContext());
        CreatePaymentRequest req = new CreatePaymentRequest(1001L, "MOCK");

        String resp = mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "key-cb")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long paymentId = objectMapper.readTree(resp).get("data").get("paymentId").asLong();

        MockCallbackRequest cbReq = new MockCallbackRequest(paymentId, "SUCCESS");

        mockMvc.perform(post("/api/payments/callback/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cbReq)))
                .andExpect(status().isOk());

        Payment p = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, p.getStatus());

        BookingPaymentGuard g = guardRepository.findByBookingId(1001L).orElseThrow();
        assertNull(g.getActivePaymentId());
        assertEquals(paymentId, g.getSuccessfulPaymentId());
    }

    @Test
    void getPayment_Success() throws Exception {
        Payment p = new Payment();
        p.setAccountId(15L);
        p.setBookingId(1001L);
        p.setPaymentTransactionCode("TXN-123");
        p.setAmount(new BigDecimal("100"));
        p.setPaymentMethod(PaymentMethod.MOCK);
        p.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        p.setAttemptNumber(1);
        p = paymentRepository.save(p);

        mockMvc.perform(get("/api/payments/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").value(p.getId()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void cancelPayment_Success() throws Exception {
        Payment p = new Payment();
        p.setAccountId(15L);
        p.setBookingId(1001L);
        p.setPaymentTransactionCode("TXN-456");
        p.setAmount(new BigDecimal("100"));
        p.setPaymentMethod(PaymentMethod.MOCK);
        p.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        p.setAttemptNumber(1);
        p = paymentRepository.save(p);

        BookingPaymentGuard g = new BookingPaymentGuard();
        g.setBookingId(1001L);
        g.setActivePaymentId(p.getId());
        guardRepository.save(g);

        mockMvc.perform(post("/api/payments/" + p.getId() + "/cancel")
                .header("Idempotency-Key", "cancel-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        Payment updated = paymentRepository.findById(p.getId()).orElseThrow();
        assertEquals(PaymentStatus.CANCELLED, updated.getStatus());

        BookingPaymentGuard updatedGuard = guardRepository.findByBookingId(1001L).orElseThrow();
        assertNull(updatedGuard.getActivePaymentId());
    }
}
