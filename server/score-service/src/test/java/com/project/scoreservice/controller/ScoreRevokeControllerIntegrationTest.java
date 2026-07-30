package com.project.scoreservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.dto.ScoreRevokeRequest;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.junit.jupiter.api.Disabled;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ScoreRevokeControllerIntegrationTest {

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

    @Autowired
    private com.project.scoreservice.repository.PointExpirationBucketRepository pointExpirationBucketRepository;

    @Autowired
    private com.project.scoreservice.repository.MembershipTierHistoryRepository membershipTierHistoryRepository;

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String VALID_INTERNAL_TOKEN = "secret-internal-token";

    private MembershipTier silverTier;
    private MembershipTier goldTier;
    private MembershipTier diamondTier;

    @BeforeEach
    void setUp() {
        pointExpirationBucketRepository.deleteAll();
        membershipTierHistoryRepository.deleteAll();
        scoreHistoryRepository.deleteAll();
        userScoreRepository.deleteAll();


        silverTier = membershipTierRepository.findAll().stream()
                .filter(t -> t.getTierName().equals("SILVER"))
                .findFirst()
                .orElseGet(() -> membershipTierRepository.save(new MembershipTier(null, "SILVER", 0, new BigDecimal("0.05"), "Silver tier", null, null)));

        goldTier = membershipTierRepository.findAll().stream()
                .filter(t -> t.getTierName().equals("GOLD"))
                .findFirst()
                .orElseGet(() -> membershipTierRepository.save(new MembershipTier(null, "GOLD", 400, new BigDecimal("0.07"), "Gold tier", null, null)));

        diamondTier = membershipTierRepository.findAll().stream()
                .filter(t -> t.getTierName().equals("DIAMOND"))
                .findFirst()
                .orElseGet(() -> membershipTierRepository.save(new MembershipTier(null, "DIAMOND", 1000, new BigDecimal("0.10"), "Diamond tier", null, null)));
    }

    @Test
    public void testRevokeEarn_SecurityMissingToken_ReturnsUnauthorized() throws Exception {
        ScoreRevokeRequest request = new ScoreRevokeRequest(1L, 1001L, 10, "evt-earn-1", "evt-revoke-1", "idem-revoke-1", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    public void testRevokeEarn_SecurityInvalidToken_ReturnsForbidden() throws Exception {
        ScoreRevokeRequest request = new ScoreRevokeRequest(1L, 1001L, 10, "evt-earn-1", "evt-revoke-1", "idem-revoke-1", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN")));
    }

    @Test
    @WithMockUser(username = "1")
    public void testRevokeEarn_SecurityCustomerJWTOnly_ReturnsUnauthorized() throws Exception {
        ScoreRevokeRequest request = new ScoreRevokeRequest(1L, 1001L, 10, "evt-earn-1", "evt-revoke-1", "idem-revoke-1", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRevokeEarn_ValidationInvalidRequest_ReturnsBadRequest() throws Exception {
        ScoreRevokeRequest request = new ScoreRevokeRequest(null, 1001L, -10, "", "evt-revoke-1", "idem-revoke-1", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    public void testRevokeEarn_OriginalNotFound_ReturnsConflict() throws Exception {
        Long userId = 101L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1001L, 10, "evt-non-existent", "evt-revoke-1", "idem-revoke-1", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_ORIGINAL_TRANSACTION_INVALID")));
    }

    @Test
    public void testRevokeEarn_WrongTransactionType_ReturnsBadRequest() throws Exception {
        Long userId = 102L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        // Pre-create a REDEEM history record (not EARN)
        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1002L)
                .eventId("evt-redeem-orig")
                .pointChange(-50)
                .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                .balanceBefore(150)
                .balanceAfter(100)
                .accumulatedBefore(100)
                .accumulatedAfter(100)
                .idempotencyKey("idem-redeem-orig")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1002L, 10, "evt-redeem-orig", "evt-revoke-2", "idem-revoke-2", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_ORIGINAL_TRANSACTION_INVALID")));
    }

    @Test
    public void testRevokeEarn_UserMismatch_ReturnsBadRequest() throws Exception {
        Long userId = 103L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1003L)
                .eventId("evt-earn-orig-3")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-3")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        // Call with wrong userId (e.g. 999L instead of 103L)
        ScoreRevokeRequest request = new ScoreRevokeRequest(999L, 1003L, 10, "evt-earn-orig-3", "evt-revoke-3", "idem-revoke-3", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_ORIGINAL_TRANSACTION_MISMATCH")));
    }

    @Test
    public void testRevokeEarn_BookingMismatch_ReturnsBadRequest() throws Exception {
        Long userId = 104L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1004L)
                .eventId("evt-earn-orig-4")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-4")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        // Call with wrong bookingId (e.g. 9999L instead of 1004L)
        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 9999L, 10, "evt-earn-orig-4", "evt-revoke-4", "idem-revoke-4", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_ORIGINAL_TRANSACTION_MISMATCH")));
    }

    @Test
    public void testRevokeEarn_RevokeExceedsOriginal_ReturnsBadRequest() throws Exception {
        Long userId = 105L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1005L)
                .eventId("evt-earn-orig-5")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-5")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        // Call with points = 60 (exceeds original 50 points)
        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1005L, 60, "evt-earn-orig-5", "evt-revoke-5", "idem-revoke-5", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_REVOKE_AMOUNT_EXCEEDS_ORIGINAL")));
    }

    @Test
    public void testRevokeEarn_AlreadyFullyRevoked_ReturnsBadRequest() throws Exception {
        Long userId = 106L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1006L)
                .eventId("evt-earn-orig-6")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-6")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        // Pre-create fully revoked history (50 points requested)
        ScoreHistory previousRevoke = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1006L)
                .eventId("evt-revoke-prev-6")
                .pointChange(-50)
                .transactionType(ScoreTransactionType.REVOKE_EARN_BY_REFUND)
                .balanceBefore(100)
                .balanceAfter(50)
                .accumulatedBefore(100)
                .accumulatedAfter(50)
                .idempotencyKey("idem-revoke-prev-6")
                .referenceHistory(original)
                .requestedPointChange(-50)
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(previousRevoke);

        // Call again with point = 10
        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1006L, 10, "evt-earn-orig-6", "evt-revoke-6", "idem-revoke-6", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_REVOKE_ALREADY_PROCESSED")));
    }

    @Test
    public void testRevokeEarn_FullRevoke_Succeeds() throws Exception {
        Long userId = 107L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1007L)
                .eventId("evt-earn-orig-7")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-7")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1007L, 30, "evt-earn-orig-7", "evt-revoke-7", "idem-revoke-7", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(userId.intValue())))
                .andExpect(jsonPath("$.data.requestedPoints", is(30)))
                .andExpect(jsonPath("$.data.deductedPoints", is(30)))
                .andExpect(jsonPath("$.data.outstandingPoints", is(0)))
                .andExpect(jsonPath("$.data.currentPoints", is(70)))
                .andExpect(jsonPath("$.data.accumulatedPoints", is(70)))
                .andExpect(jsonPath("$.data.previousTier", is("SILVER")))
                .andExpect(jsonPath("$.data.currentTier", is("SILVER")))
                .andExpect(jsonPath("$.data.tierChanged", is(false)))
                .andExpect(jsonPath("$.data.reconciliationStatus", is("NONE")))
                .andExpect(jsonPath("$.data.requiresManualReconciliation", is(false)))
                .andExpect(jsonPath("$.data.idempotent", is(false)));

        // Verify Database
        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertEquals(70, updated.getCurrentPoints());
        assertEquals(70, updated.getAccumulatedPoints());
    }

    @Test
    public void testRevokeEarn_PartialRevoke_SucceedsWithPendingReconciliation() throws Exception {
        Long userId = 108L;
        // User balance has only 10 points left, but we want to revoke 40 points
        UserScore userScore = new UserScore(userId, 10, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1008L)
                .eventId("evt-earn-orig-8")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-8")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1008L, 40, "evt-earn-orig-8", "evt-revoke-8", "idem-revoke-8", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.requestedPoints", is(40)))
                .andExpect(jsonPath("$.data.deductedPoints", is(10)))
                .andExpect(jsonPath("$.data.outstandingPoints", is(30)))
                .andExpect(jsonPath("$.data.currentPoints", is(0)))
                .andExpect(jsonPath("$.data.accumulatedPoints", is(60))) // Deducted by requested revoke points (100 - 40)
                .andExpect(jsonPath("$.data.previousTier", is("SILVER")))
                .andExpect(jsonPath("$.data.currentTier", is("SILVER")))
                .andExpect(jsonPath("$.data.tierChanged", is(false)))
                .andExpect(jsonPath("$.data.reconciliationStatus", is("PENDING")))
                .andExpect(jsonPath("$.data.requiresManualReconciliation", is(true)));

        // Verify Database
        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertEquals(0, updated.getCurrentPoints());
        assertEquals(60, updated.getAccumulatedPoints());
    }

    @Test
    public void testRevokeEarn_ZeroBalanceRevoke_SucceedsWithPendingReconciliation() throws Exception {
        Long userId = 109L;
        // User has 0 points left
        UserScore userScore = new UserScore(userId, 0, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1009L)
                .eventId("evt-earn-orig-9")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-9")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1009L, 40, "evt-earn-orig-9", "evt-revoke-9", "idem-revoke-9", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.requestedPoints", is(40)))
                .andExpect(jsonPath("$.data.deductedPoints", is(0)))
                .andExpect(jsonPath("$.data.outstandingPoints", is(40)))
                .andExpect(jsonPath("$.data.currentPoints", is(0)))
                .andExpect(jsonPath("$.data.accumulatedPoints", is(60)))
                .andExpect(jsonPath("$.data.previousTier", is("SILVER")))
                .andExpect(jsonPath("$.data.currentTier", is("SILVER")))
                .andExpect(jsonPath("$.data.tierChanged", is(false)))
                .andExpect(jsonPath("$.data.reconciliationStatus", is("PENDING")))
                .andExpect(jsonPath("$.data.requiresManualReconciliation", is(true)));
    }

    @Test
    public void testRevokeEarn_TierDowngrade_Succeeds() throws Exception {
        Long userId = 110L;
        // User has 500 accumulated points (GOLD tier) and 100 current points
        UserScore userScore = new UserScore(userId, 100, 500, goldTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1010L)
                .eventId("evt-earn-orig-10")
                .pointChange(150)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(0)
                .balanceAfter(150)
                .accumulatedBefore(350)
                .accumulatedAfter(500)
                .idempotencyKey("idem-earn-orig-10")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        // Revoke 150 points (accumulated will drop from 500 to 350, which downgrades user to SILVER)
        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1010L, 150, "evt-earn-orig-10", "evt-revoke-10", "idem-revoke-10", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.previousTier", is("GOLD")))
                .andExpect(jsonPath("$.data.currentTier", is("SILVER")))
                .andExpect(jsonPath("$.data.tierChanged", is(true)));

        // Verify Database
        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertEquals(350, updated.getAccumulatedPoints());
        String actualTierName = membershipTierRepository.findById(updated.getCurrentTier().getId()).orElseThrow().getTierName();
        assertEquals("SILVER", actualTierName);
    }

    @Test
    public void testRevokeEarn_IdempotencySameRequest_ReturnsSameResult() throws Exception {
        Long userId = 111L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1011L)
                .eventId("evt-earn-orig-11")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-11")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1011L, 30, "evt-earn-orig-11", "evt-revoke-11", "idem-revoke-11", "Refund");
        
        // 1st request
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.idempotent", is(false)));

        // 2nd retry
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idempotent", is(true)));
    }

    @Test
    public void testRevokeEarn_IdempotencyConflict_ReturnsConflict() throws Exception {
        Long userId = 112L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1012L)
                .eventId("evt-earn-orig-12")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-12")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        ScoreRevokeRequest request1 = new ScoreRevokeRequest(userId, 1012L, 30, "evt-earn-orig-12", "evt-revoke-12", "idem-revoke-12", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Same idempotencyKey/eventId but different points
        ScoreRevokeRequest request2 = new ScoreRevokeRequest(userId, 1012L, 40, "evt-earn-orig-12", "evt-revoke-12", "idem-revoke-12", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_IDEMPOTENCY_CONFLICT")));
    }

    @Test
    public void testRevokeEarn_ConcurrencyRevokes_RevokesOnlyOnce() throws Exception {
        Long userId = 113L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1013L)
                .eventId("evt-earn-orig-13")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-13")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final String eventId = "evt-concurrent-rev-" + i;
            final String idempotencyKey = "idem-concurrent-rev-" + i;
            executor.submit(() -> {
                try {
                    latch.await();
                    ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1013L, 30, "evt-earn-orig-13", eventId, idempotencyKey, "Refund");
                    int status = mockMvc.perform(post("/internal/scores/revoke-earn")
                                    .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn().getResponse().getStatus();
                    if (status == 200 || status == 201) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        latch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Exactly 1 must succeed because 30 points are deducted, leaving 20. Other threads requesting 30 will exceed 20 and fail.
        assertEquals(1, successCount.get());
        assertEquals(9, failureCount.get());

        // Verify balance is only deducted once
        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertEquals(70, updated.getCurrentPoints());
        assertEquals(70, updated.getAccumulatedPoints());
    }

    @Test
    public void testRevokeEarn_ConcurrencyRedeemAndRevoke_DoesNotGoNegative() throws Exception {
        Long userId = 114L;
        // User has exactly 50 points
        UserScore userScore = new UserScore(userId, 50, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1014L)
                .eventId("evt-earn-orig-14")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-14")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        // We run a redeem of 40 points and a revoke of 40 points concurrently.
        // Since initial balance is 50, one transaction should succeed and reduce balance to 10.
        // The other transaction should find only 10 points left.
        // - If redeem runs first: redeem deducts 40 (balance -> 10). Then revoke runs: actualDeducted = 10, outstanding = 30 (balance -> 0).
        // - If revoke runs first: revoke deducts 40 (balance -> 10). Then redeem runs: fails with SCORE_INSUFFICIENT_BALANCE.
        // In both cases, the final balance must never go below zero!
        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        executor.submit(() -> {
            try {
                latch.await();
                mockMvc.perform(post("/internal/scores/redeem")
                                .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new com.project.scoreservice.dto.ScoreRedeemRequest(userId, 2014L, 40, "evt-red-14", "idem-red-14"))));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                latch.await();
                mockMvc.perform(post("/internal/scores/revoke-earn")
                                .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ScoreRevokeRequest(userId, 1014L, 40, "evt-earn-orig-14", "evt-rev-14", "idem-rev-14", "Refund"))));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        });

        latch.countDown();
        finishLatch.await();
        executor.shutdown();

        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertTrue(updated.getCurrentPoints() >= 0);
    }

    @Test
    public void testRevokeEarn_TransactionRollback_DoesNotCommitState() throws Exception {
        Long userId = 115L;
        UserScore userScore = new UserScore(userId, 100, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory original = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1015L)
                .eventId("evt-earn-orig-15")
                .pointChange(50)
                .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                .balanceBefore(50)
                .balanceAfter(100)
                .accumulatedBefore(50)
                .accumulatedAfter(100)
                .idempotencyKey("idem-earn-orig-15")
                .reconciliationStatus(ReconciliationStatus.NONE)
                .outstandingPoints(0)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(original);

        // Attempt a revoke request that fails (exceeds amount), check that points are NOT updated
        ScoreRevokeRequest request = new ScoreRevokeRequest(userId, 1015L, 100, "evt-earn-orig-15", "evt-revoke-15", "idem-revoke-15", "Refund");
        mockMvc.perform(post("/internal/scores/revoke-earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Verify balance remains 100
        UserScore updated = userScoreRepository.findByUserId(userId).orElseThrow();
        assertEquals(100, updated.getCurrentPoints());
        assertEquals(100, updated.getAccumulatedPoints());
    }

    @Test
    public void testRevokeEarn_PendingReconciliationQuery_ReturnsMatchingHistories() {
        // Prepare query foundation test
        Long userId = 116L;
        UserScore userScore = new UserScore(userId, 0, 100, silverTier, null, null);
        userScoreRepository.saveAndFlush(userScore);

        ScoreHistory pendingHistory = ScoreHistory.builder()
                .userScore(userScore)
                .bookingId(1016L)
                .eventId("evt-rev-pending")
                .pointChange(0)
                .transactionType(ScoreTransactionType.REVOKE_EARN_BY_REFUND)
                .balanceBefore(0)
                .balanceAfter(0)
                .accumulatedBefore(100)
                .accumulatedAfter(60)
                .idempotencyKey("idem-rev-pending")
                .requestedPointChange(40)
                .outstandingPoints(40)
                .reconciliationStatus(ReconciliationStatus.PENDING)
                .requestId(UUID.randomUUID().toString())
                .build();
        scoreHistoryRepository.saveAndFlush(pendingHistory);

        List<ScoreHistory> pendingItems = scoreHistoryRepository.findByReconciliationStatusOrderByCreatedAtAsc(ReconciliationStatus.PENDING);
        assertFalse(pendingItems.isEmpty());
        assertTrue(pendingItems.stream().anyMatch(h -> h.getEventId().equals("evt-rev-pending")));
    }
}
