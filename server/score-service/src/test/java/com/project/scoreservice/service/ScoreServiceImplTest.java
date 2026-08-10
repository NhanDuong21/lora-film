package com.project.scoreservice.service;

import com.project.scoreservice.dto.*;
import com.project.scoreservice.client.BookingContext;
import com.project.scoreservice.client.BookingInternalClient;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.enumtype.UserScoreStatus;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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

    @MockBean
    private BookingInternalClient bookingInternalClient;

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
        membershipTierHistoryRepository.deleteAll();
        userScoreRepository.deleteAll();
        bronzeTier = membershipTierRepository
                .findFirstByIsActiveTrueOrderByMinAccumulatedPointsAsc()
                .orElseThrow();

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
        assertEquals(bronzeTier.getTierCode(), response.getCurrentTier().getTierCode());
    }

    @Test
    void getUserScore_WhenUserDoesNotExist_ShouldInitializeNewScore() {
        UserScoreResponse response = scoreService.getUserScore(101L);

        assertNotNull(response);
        assertEquals(101L, response.getUserId());
        assertEquals(0, response.getCurrentPoints());
        assertEquals(bronzeTier.getTierCode(), response.getCurrentTier().getTierCode());

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

    @Test
    void previewRedeem_WithPublicBookingId_ShouldUseAuthoritativeBookingAmount() {
        String bookingPublicId = "550e8400-e29b-41d4-a716-446655440000";
        when(bookingInternalClient.getBookingContext(bookingPublicId))
                .thenReturn(new BookingContext(
                        300L,
                        bookingPublicId,
                        100L,
                        "PENDING_PAYMENT",
                        Instant.now().plusSeconds(600),
                        true,
                        new BigDecimal("300000")));

        RedeemPreviewResponse response = scoreService.previewRedeem(
                100L,
                new RedeemPreviewRequest(null, bookingPublicId, 200));

        assertTrue(response.eligible());
        assertEquals(299, response.maxRedeemablePoints());
        assertEquals(new BigDecimal("200000.00"), response.discountAmount());
        assertEquals(new BigDecimal("100000.00"), response.remainingAmount());
    }

    @Test
    void holdPoints_ShouldReturnTheAuthoritativeDiscountValue() {
        ScoreHoldResponse response = scoreService.holdPoints(new ScoreHoldRequest(
                100L,
                300L,
                50,
                900,
                "EVT-HOLD-1",
                "HOLD-IDEM-1",
                new BigDecimal("300000")));

        assertEquals(50, response.pointsHeld());
        assertEquals(new BigDecimal("50000.00"), response.discountAmount());
        assertEquals(new BigDecimal("1000"), response.valuePerPoint());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void holdPoints_ShouldNotAllowScoreToPayTheWholeBooking() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                scoreService.holdPoints(new ScoreHoldRequest(
                        100L,
                        300L,
                        300,
                        900,
                        "EVT-HOLD-2",
                        "HOLD-IDEM-2",
                        new BigDecimal("300000"))));

        assertEquals("SCORE_DISCOUNT_EXCEEDS_BOOKING_AMOUNT", exception.getErrorCode());
    }
}
