package com.project.scoreservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.dto.ScoreRedeemRequest;
import com.project.scoreservice.dto.ScoreRefundRequest;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.ScoreHistory;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.repository.ScoreHistoryRepository;
import com.project.scoreservice.repository.UserScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ScoreRedeemRefundControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipTierRepository membershipTierRepository;

    @Autowired
    private UserScoreRepository userScoreRepository;

    @Autowired
    private ScoreHistoryRepository scoreHistoryRepository;

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String VALID_INTERNAL_TOKEN = "secret-internal-token";

    private MembershipTier silverTier;

    @BeforeEach
    void setUp() {
        scoreHistoryRepository.deleteAll();
        userScoreRepository.deleteAll();
        silverTier = membershipTierRepository.findAll().stream()
                .filter(t -> t.getTierName().equals("SILVER"))
                .findFirst()
                .orElseGet(() -> membershipTierRepository.save(new MembershipTier(null, "SILVER", 0, new java.math.BigDecimal("0.05"), "Silver tier", null, null)));
    }

    @Test
    public void testRedeemScore_SecurityMissingToken_ReturnsUnauthorized() throws Exception {
        ScoreRedeemRequest request = new ScoreRedeemRequest(1L, 1001L, 10, "evt-1", "idem-1");
        mockMvc.perform(post("/internal/scores/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    public void testRedeemScore_SecurityInvalidToken_ReturnsForbidden() throws Exception {
        ScoreRedeemRequest request = new ScoreRedeemRequest(1L, 1001L, 10, "evt-1", "idem-1");
        mockMvc.perform(post("/internal/scores/redeem")
                        .header(INTERNAL_TOKEN_HEADER, "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN")));
    }

    @Test
    @WithMockUser(username = "1")
    public void testRedeemScore_SecurityCustomerJWTOnly_ReturnsUnauthorized() throws Exception {
        ScoreRedeemRequest request = new ScoreRedeemRequest(1L, 1001L, 10, "evt-1", "idem-1");
        mockMvc.perform(post("/internal/scores/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRedeemScore_ValidationInvalidRequest_ReturnsValidationError() throws Exception {
        // Null user, null booking, negative/zero points, blank eventId
        ScoreRedeemRequest request = new ScoreRedeemRequest(null, null, 0, "", "  ");
        mockMvc.perform(post("/internal/scores/redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    public void testRedeemScore_SuccessfulRedeem_DeductsPoints() throws Exception {
        Long userId = 999L;
        Long bookingId = 2001L;
        
        // Setup initial user score balance: 100 points
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreRedeemRequest request = new ScoreRedeemRequest(userId, bookingId, 30, "evt-redeem-1", "idem-redeem-1");

        mockMvc.perform(post("/internal/scores/redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Score redeemed successfully")))
                .andExpect(jsonPath("$.data.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.data.bookingId", is(bookingId.intValue())))
                .andExpect(jsonPath("$.data.redeemedPoints", is(30)))
                .andExpect(jsonPath("$.data.redeemValue", is(30000))) // 30 * 1000 = 30000
                .andExpect(jsonPath("$.data.currentPoints", is(70)))  // 100 - 30 = 70
                .andExpect(jsonPath("$.data.accumulatedPoints", is(100))) // accumulatedPoints remains unchanged
                .andExpect(jsonPath("$.data.idempotent", is(false)));

        // Verify user balance in repository
        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertEquals(70, updated.getCurrentPoints());
        assertEquals(100, updated.getAccumulatedPoints());

        // Verify history row
        Optional<ScoreHistory> historyOpt = scoreHistoryRepository.findByEventId("evt-redeem-1");
        assertTrue(historyOpt.isPresent());
        ScoreHistory history = historyOpt.get();
        assertEquals(-30, history.getPointChange());
        assertEquals(ScoreTransactionType.REDEEM_FOR_BOOKING, history.getTransactionType());
        assertEquals(100, history.getBalanceBefore());
        assertEquals(70, history.getBalanceAfter());
        assertEquals("idem-redeem-1", history.getIdempotencyKey());
    }

    @Test
    public void testRedeemScore_InsufficientBalance_ReturnsConflictAndData() throws Exception {
        Long userId = 998L;
        // Seed user score with 20 points
        UserScore userScore = new UserScore(userId, 20, 20, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreRedeemRequest request = new ScoreRedeemRequest(userId, 2002L, 50, "evt-redeem-2", "idem-redeem-2");

        mockMvc.perform(post("/internal/scores/redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_INSUFFICIENT_BALANCE")))
                .andExpect(jsonPath("$.data.availablePoints", is(20)))
                .andExpect(jsonPath("$.data.requestedPoints", is(50)));
    }

    @Test
    public void testRedeemScore_IdempotencySameRequest_ReturnsSameResult() throws Exception {
        Long userId = 997L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreRedeemRequest request = new ScoreRedeemRequest(userId, 2003L, 40, "evt-redeem-3", "idem-redeem-3");

        // First call
        mockMvc.perform(post("/internal/scores/redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idempotent", is(false)));

        // Second call (idempotent retry)
        mockMvc.perform(post("/internal/scores/redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Score redeem event was already processed")))
                .andExpect(jsonPath("$.data.idempotent", is(true)))
                .andExpect(jsonPath("$.data.redeemedPoints", is(40)))
                .andExpect(jsonPath("$.data.currentPoints", is(60)));
    }

    @Test
    public void testRedeemScore_IdempotencyConflict_ReturnsConflict() throws Exception {
        Long userId = 996L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreRedeemRequest request1 = new ScoreRedeemRequest(userId, 2004L, 40, "evt-redeem-4", "idem-redeem-4");
        // Same key, different booking ID
        ScoreRedeemRequest request2 = new ScoreRedeemRequest(userId, 2005L, 40, "evt-redeem-4", "idem-redeem-4");

        mockMvc.perform(post("/internal/scores/redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/scores/redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("SCORE_IDEMPOTENCY_CONFLICT")));
    }

    @Test
    public void testRefundRedeem_SuccessfulRefund_AddsPointsAndReferencesOriginal() throws Exception {
        Long userId = 995L;
        Long bookingId = 2006L;
        
        // Seed user score with 50 points
        UserScore userScore = new UserScore(userId, 50, 150, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        // Pre-create original redeem history entry
        ScoreHistory originalRedeem = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(bookingId)
                .eventId("evt-redeem-orig")
                .pointChange(-50)
                .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                .balanceBefore(100)
                .balanceAfter(50)
                .accumulatedBefore(150)
                .accumulatedAfter(150)
                .idempotencyKey("idem-redeem-orig")
                .description("Redeemed points")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .build();
        scoreHistoryRepository.saveAndFlush(originalRedeem);

        ScoreRefundRequest request = new ScoreRefundRequest(
                userId, bookingId, 50, "evt-redeem-orig", "evt-refund-1", "idem-refund-1", "Customer cancelled booking"
        );

        mockMvc.perform(post("/internal/scores/refund-redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Redeemed score refunded successfully")))
                .andExpect(jsonPath("$.data.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.data.bookingId", is(bookingId.intValue())))
                .andExpect(jsonPath("$.data.refundedPoints", is(50)))
                .andExpect(jsonPath("$.data.currentPoints", is(100)))  // 50 + 50 = 100
                .andExpect(jsonPath("$.data.accumulatedPoints", is(150))) // remains unchanged
                .andExpect(jsonPath("$.data.originalHistoryId", is(originalRedeem.getId().intValue())))
                .andExpect(jsonPath("$.data.idempotent", is(false)));

        // Verify balance in repository
        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertEquals(100, updated.getCurrentPoints());
        assertEquals(150, updated.getAccumulatedPoints());

        // Verify history row
        Optional<ScoreHistory> refundHistOpt = scoreHistoryRepository.findByEventId("evt-refund-1");
        assertTrue(refundHistOpt.isPresent());
        ScoreHistory refundHist = refundHistOpt.get();
        assertEquals(50, refundHist.getPointChange());
        assertEquals(ScoreTransactionType.REFUND_REDEEM, refundHist.getTransactionType());
        assertEquals(originalRedeem.getId(), refundHist.getReferenceHistory().getId());
        assertEquals("Customer cancelled booking", refundHist.getReason());
    }

    @Test
    public void testRefundRedeem_OriginalRedeemNotFound_ReturnsNotFound() throws Exception {
        ScoreRefundRequest request = new ScoreRefundRequest(
                994L, 2007L, 50, "nonexistent-event", "evt-refund-2", "idem-refund-2", "Cancel"
        );

        mockMvc.perform(post("/internal/scores/refund-redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("SCORE_ORIGINAL_TRANSACTION_NOT_FOUND")));
    }

    @Test
    public void testRefundRedeem_MismatchedUserOrBooking_ReturnsBadRequest() throws Exception {
        Long userId = 993L;
        UserScore userScore = new UserScore(userId, 50, 150, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory originalRedeem = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(2008L)
                .eventId("evt-redeem-mismatch")
                .pointChange(-50)
                .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                .balanceBefore(100)
                .balanceAfter(50)
                .accumulatedBefore(150)
                .accumulatedAfter(150)
                .idempotencyKey("idem-redeem-mismatch")
                .description("Redeemed points")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .build();
        scoreHistoryRepository.saveAndFlush(originalRedeem);

        // Mismatched booking ID (2009 instead of 2008)
        ScoreRefundRequest request = new ScoreRefundRequest(
                userId, 2009L, 50, "evt-redeem-mismatch", "evt-refund-3", "idem-refund-3", "Cancel"
        );

        mockMvc.perform(post("/internal/scores/refund-redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("SCORE_TRANSACTION_MISMATCH")));
    }

    @Test
    public void testRefundRedeem_NotRedeemTransactionType_ReturnsBadRequest() throws Exception {
        Long userId = 992L;
        UserScore userScore = new UserScore(userId, 50, 150, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory originalEarn = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(2010L)
                .eventId("evt-earn-not-redeem")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING) // Earn, not Redeem
                .balanceBefore(100)
                .balanceAfter(150)
                .accumulatedBefore(150)
                .accumulatedAfter(150)
                .idempotencyKey("idem-earn-not-redeem")
                .description("Earned points")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .build();
        scoreHistoryRepository.saveAndFlush(originalEarn);

        ScoreRefundRequest request = new ScoreRefundRequest(
                userId, 2010L, 50, "evt-earn-not-redeem", "evt-refund-4", "idem-refund-4", "Cancel"
        );

        mockMvc.perform(post("/internal/scores/refund-redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("SCORE_INVALID_TRANSACTION_TYPE")));
    }

    @Test
    public void testRefundRedeem_RefundExceedsRedeemed_ReturnsBadRequest() throws Exception {
        Long userId = 991L;
        UserScore userScore = new UserScore(userId, 50, 150, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory originalRedeem = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(2011L)
                .eventId("evt-redeem-exceed")
                .pointChange(-30) // Redeemed 30 points
                .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                .balanceBefore(80)
                .balanceAfter(50)
                .accumulatedBefore(150)
                .accumulatedAfter(150)
                .idempotencyKey("idem-redeem-exceed")
                .description("Redeemed points")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .build();
        scoreHistoryRepository.saveAndFlush(originalRedeem);

        // Attempting to refund 50 points (exceeding original 30)
        ScoreRefundRequest request = new ScoreRefundRequest(
                userId, 2011L, 50, "evt-redeem-exceed", "evt-refund-5", "idem-refund-5", "Cancel"
        );

        mockMvc.perform(post("/internal/scores/refund-redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("SCORE_INVALID_REFUND_AMOUNT")));
    }

    @Test
    public void testRefundRedeem_DoubleRefundPrevention_ReturnsConflict() throws Exception {
        Long userId = 990L;
        UserScore userScore = new UserScore(userId, 50, 150, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory originalRedeem = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(2012L)
                .eventId("evt-redeem-double")
                .pointChange(-50)
                .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                .balanceBefore(100)
                .balanceAfter(50)
                .accumulatedBefore(150)
                .accumulatedAfter(150)
                .idempotencyKey("idem-redeem-double")
                .description("Redeemed points")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .build();
        scoreHistoryRepository.saveAndFlush(originalRedeem);

        // First refund call
        ScoreRefundRequest request1 = new ScoreRefundRequest(
                userId, 2012L, 50, "evt-redeem-double", "evt-refund-6", "idem-refund-6", "Cancel"
        );
        mockMvc.perform(post("/internal/scores/refund-redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Second refund call for the same original redeem
        ScoreRefundRequest request2 = new ScoreRefundRequest(
                userId, 2012L, 50, "evt-redeem-double", "evt-refund-7", "idem-refund-7", "Cancel"
        );
        mockMvc.perform(post("/internal/scores/refund-redeem")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("SCORE_ALREADY_REFUNDED")));
    }

    @Test
    public void testRedeemScore_ConcurrencyRedeemExceedsBalance_BalanceNeverNegative() throws Exception {
        Long userId = 989L;
        // User starts with 50 points
        UserScore userScore = new UserScore(userId, 50, 50, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        int numThreads = 10;
        // Each thread wants to redeem 10 points. With a balance of 50, exactly 5 threads must succeed, 5 must fail.
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failureCounter = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final String eventId = "evt-concurrent-redeem-" + i;
            final String idempotencyKey = "idem-concurrent-redeem-" + i;
            executor.submit(() -> {
                try {
                    latch.await();
                    ScoreRedeemRequest request = new ScoreRedeemRequest(userId, 3001L, 10, eventId, idempotencyKey);
                    int status = mockMvc.perform(post("/internal/scores/redeem")
                                    .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn().getResponse().getStatus();

                    if (status == 200) {
                        successCounter.incrementAndGet();
                    } else if (status == 409) {
                        failureCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads concurrently
        latch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Verify exactly 5 threads succeeded and 5 failed
        assertEquals(5, successCounter.get());
        assertEquals(5, failureCounter.get());

        // Verify balance is exactly 0 in DB
        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertEquals(0, updated.getCurrentPoints());
    }

    @Test
    public void testRefundRedeem_ConcurrencyRefunds_RefundsExactlyOnce() throws Exception {
        Long userId = 988L;
        UserScore userScore = new UserScore(userId, 50, 150, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        // Pre-create original redeem transaction
        ScoreHistory originalRedeem = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(4001L)
                .eventId("evt-redeem-orig-concurrent")
                .pointChange(-50)
                .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                .balanceBefore(100)
                .balanceAfter(50)
                .accumulatedBefore(150)
                .accumulatedAfter(150)
                .idempotencyKey("idem-redeem-orig-concurrent")
                .description("Redeemed points")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .build();
        scoreHistoryRepository.saveAndFlush(originalRedeem);

        int numThreads = 10;
        // Each thread wants to refund 50 points for the SAME redeem transaction. Exactly 1 must succeed.
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failureCounter = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final String eventId = "evt-concurrent-refund-" + i;
            final String idempotencyKey = "idem-concurrent-refund-" + i;
            executor.submit(() -> {
                try {
                    latch.await();
                    ScoreRefundRequest request = new ScoreRefundRequest(
                            userId, 4001L, 50, "evt-redeem-orig-concurrent", eventId, idempotencyKey, "Cancel booking"
                    );
                    int status = mockMvc.perform(post("/internal/scores/refund-redeem")
                                    .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn().getResponse().getStatus();

                    if (status == 200) {
                        successCounter.incrementAndGet();
                    } else {
                        failureCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads concurrently
        latch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Verify exactly 1 succeeded and others failed
        assertEquals(1, successCounter.get());
        assertEquals(9, failureCounter.get());

        // Verify balance is exactly 100 in DB (50 + 50)
        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertEquals(100, updated.getCurrentPoints());
    }
}
