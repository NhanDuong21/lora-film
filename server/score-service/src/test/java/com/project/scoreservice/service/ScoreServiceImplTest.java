package com.project.scoreservice.service;

import com.project.scoreservice.dto.*;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.enumtype.UserScoreStatus;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ScoreServiceImplTest {

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private UserScoreRepository userScoreRepository;

    @Autowired
    private MembershipTierRepository membershipTierRepository;

    @Autowired
    private ScoreHistoryRepository scoreHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ReconciliationRunRepository reconciliationRunRepository;

    @Autowired
    private ReconciliationDetailRepository reconciliationDetailRepository;

    @Autowired
    private ScoreHoldRepository scoreHoldRepository;

    @Autowired
    private PointExpirationBucketRepository pointExpirationBucketRepository;

    @Autowired
    private MembershipTierHistoryRepository membershipTierHistoryRepository;

    private MembershipTier bronzeTier;
    private UserScore userScore;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        reconciliationDetailRepository.deleteAll();
        reconciliationRunRepository.deleteAll();
        pointExpirationBucketRepository.deleteAll();
        scoreHoldRepository.deleteAll();
        scoreHistoryRepository.deleteAll();
        userScoreRepository.deleteAll();
        membershipTierHistoryRepository.deleteAll();
        membershipTierRepository.deleteAll();

        bronzeTier = MembershipTier.builder()
                .tierCode("BRONZE")
                .tierName("Bronze Member")
                .minAccumulatedPoints(0)
                .earningRate(new BigDecimal("0.05"))
                .priority(1)
                .isActive(true)
                .build();
        membershipTierRepository.save(bronzeTier);

        userScore = UserScore.builder()
                .userId(100L)
                .currentPoints(500)
                .heldPoints(0)
                .accumulatedPoints(500)
                .currentTier(bronzeTier)
                .status(UserScoreStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userScoreRepository.save(userScore);
    }

    @Test
    void getUserScore_WhenUserExists_ShouldReturnCorrectBalanceAndTier() {
        UserScoreResponse response = scoreService.getUserScore(100L);

        assertNotNull(response);
        assertEquals(100L, response.getUserId());
        assertEquals(500, response.getCurrentPoints());
        assertEquals(500, response.getAccumulatedPoints());
        assertEquals("BRONZE", response.getCurrentTier().getTierCode());
    }

    @Test
    void getUserScore_WhenUserDoesNotExist_ShouldInitializeNewScore() {
        UserScoreResponse response = scoreService.getUserScore(101L);

        assertNotNull(response);
        assertEquals(101L, response.getUserId());
        assertEquals(0, response.getCurrentPoints());
        assertEquals("BRONZE", response.getCurrentTier().getTierCode());

        assertTrue(userScoreRepository.findByUserId(101L).isPresent());
    }

    @Test
    void earnPoints_ShouldAwardPointsBasedOnTierRate() {
        ScoreEarnRequest request = new ScoreEarnRequest(
                100L,
                200L,
                new BigDecimal("1000000.00"), // 1,000,000 * 0.05 / 1000 = 50 points
                "EVT-001",
                "EARN-IDEM-01"
        );

        ScoreEarnResponse response = scoreService.earnPoints(request);

        assertNotNull(response);
        assertEquals(50, response.pointChange());
        assertEquals(500, response.balanceBefore());
        assertEquals(550, response.balanceAfter());
        assertFalse(response.idempotent());

        UserScore updated = userScoreRepository.findByUserId(100L).orElseThrow();
        assertEquals(550, updated.getCurrentPoints());
        assertEquals(550, updated.getAccumulatedPoints());
    }

    @Test
    void redeemPoints_WhenInsufficientPoints_ShouldThrowException() {
        ScoreRedeemRequest request = new ScoreRedeemRequest(
                100L,
                300L,
                1000, // trying to redeem 1000 points
                "EVT-002",
                "REDEEM-IDEM-01"
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> scoreService.redeemPoints(request));
        assertEquals("SCORE_INSUFFICIENT_BALANCE", exception.getErrorCode());
    }

    @Test
    void redeemPoints_WhenSufficientPoints_ShouldDeductBalance() {
        ScoreRedeemRequest request = new ScoreRedeemRequest(
                100L,
                300L,
                200, // redeem 200 points
                "EVT-003",
                "REDEEM-IDEM-02"
        );

        ScoreRedeemResponse response = scoreService.redeemPoints(request);

        assertNotNull(response);
        assertEquals(100L, response.userId());
        assertEquals(200, response.redeemedPoints());
        assertEquals(300, response.currentPoints());
        assertFalse(response.idempotent());

        UserScore updated = userScoreRepository.findByUserId(100L).orElseThrow();
        assertEquals(300, updated.getCurrentPoints());
    }
}
