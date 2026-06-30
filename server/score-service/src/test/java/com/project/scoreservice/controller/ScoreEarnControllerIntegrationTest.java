package com.project.scoreservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.dto.ScoreEarnRequest;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.ScoreHistory;
import com.project.scoreservice.entity.UserScore;
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
public class ScoreEarnControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        scoreHistoryRepository.deleteAll();
        userScoreRepository.deleteAll();
    }

    @Test
    public void testEarnScore_SecurityMissingToken_ReturnsUnauthorized() throws Exception {
        ScoreEarnRequest request = new ScoreEarnRequest(
                100L, 200L, new BigDecimal("100000"), "evt-001", "idem-001"
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    public void testEarnScore_SecurityInvalidToken_ReturnsUnauthorized() throws Exception {
        ScoreEarnRequest request = new ScoreEarnRequest(
                100L, 200L, new BigDecimal("100000"), "evt-001", "idem-001"
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @WithMockUser(username = "100")
    public void testEarnScore_SecurityCustomerJWTOnly_ReturnsUnauthorized() throws Exception {
        // Even with valid JWT, if missing internal token, /internal/** is rejected
        ScoreEarnRequest request = new ScoreEarnRequest(
                100L, 200L, new BigDecimal("100000"), "evt-001", "idem-001"
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testEarnScore_ValidationInvalidRequest_ReturnsValidationError() throws Exception {
        // Negative amount, null userId, blank eventId
        ScoreEarnRequest request = new ScoreEarnRequest(
                null, 200L, new BigDecimal("-100"), "", "idem-001"
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    public void testEarnScore_SuccessfulEarnWithLazyInitialization_AwardPoints() throws Exception {
        Long userId = 301L;
        Long bookingId = 401L;
        BigDecimal eligibleAmount = new BigDecimal("100000"); // 100,000 eligible amount
        String eventId = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();

        ScoreEarnRequest request = new ScoreEarnRequest(
                userId, bookingId, eligibleAmount, eventId, idempotencyKey
        );

        // Verify user score account does not exist
        assertFalse(userScoreRepository.findByUserId(userId).isPresent());

        // Perform request
        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.pointChange", is(5))) // 100000 * 0.05 / 1000 = 5 points
                .andExpect(jsonPath("$.data.balanceBefore", is(0)))
                .andExpect(jsonPath("$.data.balanceAfter", is(5)))
                .andExpect(jsonPath("$.data.accumulatedBefore", is(0)))
                .andExpect(jsonPath("$.data.accumulatedAfter", is(5)))
                .andExpect(jsonPath("$.data.previousTier", is("SILVER")))
                .andExpect(jsonPath("$.data.currentTier", is("SILVER")))
                .andExpect(jsonPath("$.data.tierChanged", is(false)))
                .andExpect(jsonPath("$.data.idempotent", is(false)));

        // Verify DB updates
        Optional<UserScore> userScoreOpt = userScoreRepository.findByUserId(userId);
        assertTrue(userScoreOpt.isPresent());
        UserScore userScore = userScoreOpt.get();
        assertEquals(5, userScore.getCurrentPoints());
        assertEquals(5, userScore.getAccumulatedPoints());

        // Verify History insertion
        Optional<ScoreHistory> historyOpt = scoreHistoryRepository.findByEventId(eventId);
        assertTrue(historyOpt.isPresent());
        ScoreHistory history = historyOpt.get();
        assertEquals(userId, history.getUserScore().getUserId());
        assertEquals(bookingId, history.getBookingId());
        assertEquals(5, history.getPointChange());
        assertEquals(0, history.getBalanceBefore());
        assertEquals(5, history.getBalanceAfter());
        assertEquals(0, history.getAccumulatedBefore());
        assertEquals(5, history.getAccumulatedAfter());
        assertEquals(idempotencyKey, history.getIdempotencyKey());
    }

    @Test
    public void testEarnScore_FloorRounding_CorrectRoundingApplied() throws Exception {
        Long userId = 302L;
        // Eligible amount 15,000 * 0.05 = 750 / 1000 = 0.75 -> floor to 0
        ScoreEarnRequest request1 = new ScoreEarnRequest(
                userId, 401L, new BigDecimal("15000"), UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointChange", is(0)))
                .andExpect(jsonPath("$.data.balanceAfter", is(0)));

        // Eligible amount 20,000 * 0.05 = 1000 / 1000 = 1.0 -> floor to 1
        ScoreEarnRequest request2 = new ScoreEarnRequest(
                userId, 401L, new BigDecimal("20000"), UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointChange", is(1)))
                .andExpect(jsonPath("$.data.balanceAfter", is(1)));
    }

    @Test
    public void testEarnScore_TierUpgrade_SuccessfullyCalculatesTierUpgrade() throws Exception {
        Long userId = 303L;
        // User starts with 0 points
        // Earn enough points to cross Gold threshold (400 points)
        // Silver earning rate: 0.05
        // Needs 400 * 1000 / 0.05 = 8,000,000 eligible amount to get exactly 400 points
        ScoreEarnRequest request = new ScoreEarnRequest(
                userId, 401L, new BigDecimal("8000000"), UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointChange", is(400)))
                .andExpect(jsonPath("$.data.previousTier", is("SILVER")))
                .andExpect(jsonPath("$.data.currentTier", is("GOLD")))
                .andExpect(jsonPath("$.data.tierChanged", is(true)));

        // Earn again on the updated GOLD tier (rate: 0.07)
        // Eligible amount: 100,000
        // Points: floor(100000 * 0.07 / 1000) = 7
        ScoreEarnRequest request2 = new ScoreEarnRequest(
                userId, 402L, new BigDecimal("100000"), UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointChange", is(7)))
                .andExpect(jsonPath("$.data.previousTier", is("GOLD")))
                .andExpect(jsonPath("$.data.currentTier", is("GOLD")))
                .andExpect(jsonPath("$.data.tierChanged", is(false)));
    }

    @Test
    public void testEarnScore_IdempotencySameRequest_ReturnsSameResult() throws Exception {
        Long userId = 304L;
        String eventId = "evt-idem-test";
        String idempotencyKey = "key-idem-test";

        ScoreEarnRequest request = new ScoreEarnRequest(
                userId, 401L, new BigDecimal("100000"), eventId, idempotencyKey
        );

        // First request - awards points
        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointChange", is(5)))
                .andExpect(jsonPath("$.data.idempotent", is(false)));

        // Second request - returns same cached response
        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointChange", is(5)))
                .andExpect(jsonPath("$.data.idempotent", is(true)));

        // Verify DB only has 1 history record
        List<ScoreHistory> historyList = scoreHistoryRepository.findByBookingId(401L);
        assertEquals(1, historyList.size());
    }

    @Test
    public void testEarnScore_IdempotencyConflict_ReturnsConflict() throws Exception {
        Long userId = 305L;
        String eventId = "evt-conflict-test";
        String idempotencyKey = "key-conflict-test";

        ScoreEarnRequest request1 = new ScoreEarnRequest(
                userId, 401L, new BigDecimal("100000"), eventId, idempotencyKey
        );

        // Success first
        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Same event ID/Key, but different user/booking context (Conflict)
        ScoreEarnRequest request2 = new ScoreEarnRequest(
                306L, 402L, new BigDecimal("100000"), eventId, idempotencyKey
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("SCORE_IDEMPOTENCY_CONFLICT")));
    }

    @Test
    public void testEarnScore_PointOverflow_ThrowsBadRequest() throws Exception {
        Long userId = 307L;
        // Make existing balance huge first
        MembershipTier silverTier = membershipTierRepository.findByTierName("SILVER")
                .orElseThrow(() -> new RuntimeException("Default tier SILVER not found"));
        UserScore userScore = UserScore.builder()
                .userId(userId)
                .currentPoints(Integer.MAX_VALUE - 10)
                .accumulatedPoints(Integer.MAX_VALUE - 10)
                .currentTier(silverTier)
                .build();
        userScoreRepository.save(userScore);

        // Request earning that exceeds Integer.MAX_VALUE points
        ScoreEarnRequest request = new ScoreEarnRequest(
                userId, 401L, new BigDecimal("1000000"), UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/internal/scores/earn")
                        .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("SCORE_POINT_OVERFLOW")));
    }

    @Test
    public void testEarnScore_ConcurrencyFirstInitialization_OnlyOneRowCreated() throws Exception {
        Long userId = 308L;
        int numThreads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger conflictCounter = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final String eventId = "evt-concurrent-" + i;
            final String idempotencyKey = "idem-concurrent-" + i;
            executor.submit(() -> {
                try {
                    latch.await();
                    ScoreEarnRequest request = new ScoreEarnRequest(
                            userId, 401L, new BigDecimal("100000"), eventId, idempotencyKey
                    );
                    int status = mockMvc.perform(post("/internal/scores/earn")
                                    .header(INTERNAL_TOKEN_HEADER, VALID_INTERNAL_TOKEN)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn().getResponse().getStatus();

                    if (status == 200) {
                        successCounter.incrementAndGet();
                    } else {
                        conflictCounter.incrementAndGet();
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

        // Verify database contains exactly 1 row for this user
        List<UserScore> userScores = userScoreRepository.findAll().stream()
                .filter(us -> us.getUserId().equals(userId))
                .toList();

        assertEquals(1, userScores.size());
        assertEquals(numThreads, successCounter.get() + conflictCounter.get());
    }
}
