package com.project.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.dto.request.CashCancelRequest;
import com.project.paymentservice.dto.request.CashCollectRequest;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.repository.CashPaymentDetailRepository;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.repository.PaymentLogRepository;
import com.project.paymentservice.repository.PaymentIdempotencyRecordRepository;
import com.project.paymentservice.repository.PaymentOutboxEventRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CashPaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CashPaymentDetailRepository cashPaymentDetailRepository;

    @Autowired
    private PaymentLogRepository paymentLogRepository;

    @Autowired
    private PaymentIdempotencyRecordRepository paymentIdempotencyRecordRepository;

    @Autowired
    private PaymentOutboxEventRepository paymentOutboxEventRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void setUp() {
        cashPaymentDetailRepository.deleteAllInBatch();
        paymentLogRepository.deleteAllInBatch();
        paymentIdempotencyRecordRepository.deleteAllInBatch();
        paymentOutboxEventRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
    }

    private String generateJwt(Long userId, String email, String role) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        var builder = Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key);

        if (userId != null) {
            builder.claim("userId", userId);
        }
        if (email != null) {
            builder.subject(email);
        }
        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    @Test
    void collectCashPayment_AsAdmin_Success() throws Exception {
        Payment p = new Payment();
        p.setAccountId(1001L);
        p.setBookingId(2002L);
        p.setPaymentTransactionCode(UUID.randomUUID().toString());
        p.setAmount(new BigDecimal("250000.00"));
        p.setPaymentMethod(PaymentMethod.CASH);
        p.setAttemptNumber(1);
        p.setStatus(PaymentStatus.PENDING);
        p.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        p = paymentRepository.save(p);

        String adminToken = generateJwt(9999L, "admin@test.com", "ADMIN");

        CashCollectRequest request = new CashCollectRequest();
        request.setReceivedAmount(new BigDecimal("300000.00"));
        request.setNote("Thu tiền khách");

        mockMvc.perform(post("/api/employee/payments/" + p.getId() + "/cash/collect")
                .header("Authorization", "Bearer " + adminToken)
                .header("Idempotency-Key", "collect-" + p.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.changeAmount").value(50000.0));
    }

    @Test
    void collectCashPayment_AsCustomer_Forbidden() throws Exception {
        Payment p = new Payment();
        p.setAccountId(1001L);
        p.setBookingId(2002L);
        p.setPaymentTransactionCode(UUID.randomUUID().toString());
        p.setAmount(new BigDecimal("250000.00"));
        p.setPaymentMethod(PaymentMethod.CASH);
        p.setAttemptNumber(1);
        p.setStatus(PaymentStatus.PENDING);
        p.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        p = paymentRepository.save(p);

        String customerToken = generateJwt(1001L, "customer@test.com", "CUSTOMER");

        CashCollectRequest request = new CashCollectRequest();
        request.setReceivedAmount(new BigDecimal("300000.00"));
        request.setNote("Thu tiền khách");

        mockMvc.perform(post("/api/employee/payments/" + p.getId() + "/cash/collect")
                .header("Authorization", "Bearer " + customerToken)
                .header("Idempotency-Key", "collect-" + p.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelCashPayment_AsAdmin_Success() throws Exception {
        Payment p = new Payment();
        p.setAccountId(1001L);
        p.setBookingId(2002L);
        p.setPaymentTransactionCode(UUID.randomUUID().toString());
        p.setAmount(new BigDecimal("250000.00"));
        p.setPaymentMethod(PaymentMethod.CASH);
        p.setAttemptNumber(1);
        p.setStatus(PaymentStatus.PENDING);
        p.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        p = paymentRepository.save(p);

        String adminToken = generateJwt(9999L, "admin@test.com", "ADMIN");

        CashCancelRequest request = new CashCancelRequest();
        request.setReason("Khách không mang tiền mặt");

        mockMvc.perform(post("/api/employee/payments/" + p.getId() + "/cash/cancel")
                .header("Authorization", "Bearer " + adminToken)
                .header("Idempotency-Key", "cancel-" + p.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
