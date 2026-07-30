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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ScoreConcurrencyIntegrationTest {

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
    }

    @Test
    void testConcurrentInitialization() throws InterruptedException {
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    ScoreEarnRequest req = new ScoreEarnRequest(
                            999L,
                            ThreadLocalRandom.current().nextLong(10000, 99999),
                            new BigDecimal("100000.00"), // 5 points
                            "EVT-INIT-" + Thread.currentThread().getId(),
                            "IDEM-INIT-" + ThreadLocalRandom.current().nextInt()
                    );
                    scoreService.earnPoints(req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Ignore or log in verbose mode
                }
            }));
        }

        latch.countDown();
        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Ignore timeout or execution exception
            }
        }
        executor.shutdown();

        assertEquals(numThreads, successCount.get(), "All concurrent initializations and earns should succeed without constraint violation");
        List<UserScore> allScores = userScoreRepository.findAll();
        assertEquals(1, allScores.size(), "Exactly 1 UserScore should be created for userId 999");
        assertEquals(5 * numThreads, allScores.get(0).getCurrentPoints(), "Final balance should equal total earned points");
    }

    @Test
    void testConcurrentEarning() throws InterruptedException {
        UserScore us = UserScore.builder()
                .userId(1000L)
                .currentPoints(0)
                .heldPoints(0)
                .accumulatedPoints(0)
                .currentTier(bronzeTier)
                .status(UserScoreStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userScoreRepository.save(us);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            final long bookingId = 2000L + i;
            final String eventId = "EVT-EARN-" + i;
            final String idemKey = "IDEM-EARN-" + i;
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    ScoreEarnRequest req = new ScoreEarnRequest(
                            1000L,
                            bookingId,
                            new BigDecimal("1000000.00"), // 50 points
                            eventId,
                            idemKey
                    );
                    scoreService.earnPoints(req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Log
                }
            }));
        }

        latch.countDown();
        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Ignore
            }
        }
        executor.shutdown();

        assertEquals(numThreads, successCount.get());
        UserScore updated = userScoreRepository.findByUserId(1000L).orElseThrow();
        assertEquals(500, updated.getCurrentPoints(), "10 threads * 50 points = 500 total points");
        assertEquals(500, updated.getAccumulatedPoints());
    }

    @Test
    void testConcurrentRedeeming() throws InterruptedException {
        UserScore us = UserScore.builder()
                .userId(1001L)
                .currentPoints(500)
                .heldPoints(0)
                .accumulatedPoints(500)
                .currentTier(bronzeTier)
                .status(UserScoreStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userScoreRepository.save(us);

        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            final long bookingId = 3000L + i;
            final String eventId = "EVT-REDEEM-" + i;
            final String idemKey = "IDEM-REDEEM-" + i;
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    ScoreRedeemRequest req = new ScoreRedeemRequest(
                            1001L,
                            bookingId,
                            200, // each trying to redeem 200 points
                            eventId,
                            idemKey
                    );
                    scoreService.redeemPoints(req);
                    successCount.incrementAndGet();
                } catch (BusinessException be) {
                    if ("SCORE_INSUFFICIENT_BALANCE".equals(be.getErrorCode())) {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Other error
                }
            }));
        }

        latch.countDown();
        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Ignore
            }
        }
        executor.shutdown();

        assertEquals(2, successCount.get(), "Exactly 2 threads should succeed in redeeming 200 points from 500 balance");
        assertEquals(3, failCount.get(), "3 threads should fail with SCORE_INSUFFICIENT_BALANCE");
        UserScore updated = userScoreRepository.findByUserId(1001L).orElseThrow();
        assertEquals(100, updated.getCurrentPoints(), "500 - (2 * 200) = 100 points remaining");
    }
}
