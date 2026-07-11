package com.project.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.repository.PaymentRepository;
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
import java.util.Date;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void setUp() {
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
    
    private String generateInvalidJwt() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";
    }

    @Test
    void missingJwtShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void invalidJwtShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/payments/1")
                .header("Authorization", "Bearer " + generateInvalidJwt()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void jwtWithoutRequiredAccountIdClaimShouldReturnUnauthorized() throws Exception {
        String token = generateJwt(null, "user@test.com", "CUSTOMER"); // Missing userId claim

        mockMvc.perform(get("/api/payments/1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void validJwtWithCorrectAccountIdShouldReadOwnPayment() throws Exception {
        Payment p = new Payment();
        p.setAccountId(15L);
        p.setBookingId(1001L);
        p.setPaymentTransactionCode("SEC-123");
        p.setAmount(new BigDecimal("100"));
        p.setPaymentMethod(PaymentMethod.MOCK);
        p.setAttemptNumber(1);
        p.setStatus(PaymentStatus.PENDING);
        p.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        p = paymentRepository.save(p);

        String token = generateJwt(15L, "user@test.com", "CUSTOMER");

        mockMvc.perform(get("/api/payments/" + p.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").value(p.getId()));
    }

    @Test
    void jwtFromAnotherAccountShouldReturnForbidden() throws Exception {
        Payment p = new Payment();
        p.setAccountId(15L);
        p.setBookingId(1001L);
        p.setPaymentTransactionCode("SEC-456");
        p.setAmount(new BigDecimal("100"));
        p.setPaymentMethod(PaymentMethod.MOCK);
        p.setAttemptNumber(1);
        p.setStatus(PaymentStatus.PENDING);
        p.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        p = paymentRepository.save(p);

        String wrongToken = generateJwt(99L, "other@test.com", "CUSTOMER");

        mockMvc.perform(get("/api/payments/" + p.getId())
                .header("Authorization", "Bearer " + wrongToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }
}
