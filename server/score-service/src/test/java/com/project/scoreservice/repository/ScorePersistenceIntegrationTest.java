package com.project.scoreservice.repository;

import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.entity.ScoreHistory;
import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class ScorePersistenceIntegrationTest {

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            // Nullify self-references first to avoid constraint issues during clean-up
            entityManager.createQuery("UPDATE ScoreHistory s SET s.referenceHistory = null").executeUpdate();
            pointExpirationBucketRepository.deleteAll();
            membershipTierHistoryRepository.deleteAll();
            scoreHistoryRepository.deleteAll();
            userScoreRepository.deleteAll();
            membershipTierRepository.deleteAll();
            return null;
        });
    }


    private MembershipTier createAndSaveTier(String tierName, int minPoints, double earningRate) {
        return transactionTemplate.execute(status -> {
            MembershipTier tier = MembershipTier.builder()
                    .tierName(tierName)
                    .minPoints(minPoints)
                    .earningRate(BigDecimal.valueOf(earningRate))
                    .description("Test description for " + tierName)
                    .build();
            return membershipTierRepository.save(tier);
        });
    }

    private UserScore createAndSaveUserScore(Long userId, int currentPoints, MembershipTier tier) {
        return transactionTemplate.execute(status -> {
            UserScore score = UserScore.builder()
                    .userId(userId)
                    .currentPoints(currentPoints)
                    .accumulatedPoints(currentPoints)
                    .currentTier(tier)
                    .build();
            return userScoreRepository.save(score);
        });
    }

    @Test
    void testEntityMapping() {
        // 1. Arrange
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        UserScore score = createAndSaveUserScore(100L, 100, tier);

        transactionTemplate.execute(status -> {
            ScoreHistory parentHistory = ScoreHistory.builder()
                    .userScore(score)
                    .bookingId(1L)
                    .eventId("event-parent")
                    .pointChange(10)
                    .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                    .balanceBefore(90)
                    .balanceAfter(100)
                    .accumulatedBefore(90)
                    .accumulatedAfter(100)
                    .idempotencyKey("idemp-parent")
                    .requestId("req-parent")
                    .reason("Earn points")
                    .description("Parent description")
                    .requestedPointChange(10)
                    .outstandingPoints(0)
                    .reconciliationStatus(ReconciliationStatus.NONE)
                    .build();
            ScoreHistory savedParent = scoreHistoryRepository.save(parentHistory);

            ScoreHistory childHistory = ScoreHistory.builder()
                    .userScore(score)
                    .bookingId(2L)
                    .eventId("event-child")
                    .pointChange(-20)
                    .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                    .balanceBefore(100)
                    .balanceAfter(80)
                    .accumulatedBefore(100)
                    .accumulatedAfter(100)
                    .idempotencyKey("idemp-child")
                    .referenceHistory(savedParent)
                    .requestId("req-child")
                    .reason("Redeem points")
                    .description("Child description")
                    .requestedPointChange(-20)
                    .outstandingPoints(0)
                    .reconciliationStatus(ReconciliationStatus.NONE)
                    .build();
            scoreHistoryRepository.save(childHistory);
            return null;
        });

        // Clear persistence context
        transactionTemplate.execute(status -> {
            entityManager.clear();
            return null;
        });

        // 2. Act & Assert
        transactionTemplate.execute(status -> {
            // Verify MembershipTier
            Optional<MembershipTier> loadedTierOpt = membershipTierRepository.findByTierName("SILVER");
            assertThat(loadedTierOpt).isPresent();
            MembershipTier loadedTier = loadedTierOpt.get();
            assertThat(loadedTier.getMinPoints()).isEqualTo(0);
            assertThat(loadedTier.getEarningRate().compareTo(BigDecimal.valueOf(0.05))).isEqualTo(0);
            assertThat(loadedTier.getCreatedAt()).isNotNull();
            assertThat(loadedTier.getUpdatedAt()).isNotNull();

            // Verify UserScore
            Optional<UserScore> loadedScoreOpt = userScoreRepository.findByUserId(100L);
            assertThat(loadedScoreOpt).isPresent();
            UserScore loadedScore = loadedScoreOpt.get();
            assertThat(loadedScore.getCurrentPoints()).isEqualTo(100);
            assertThat(loadedScore.getCurrentTier().getId()).isEqualTo(loadedTier.getId());

            // Verify ScoreHistory
            Optional<ScoreHistory> childOpt = scoreHistoryRepository.findByIdempotencyKey("idemp-child");
            assertThat(childOpt).isPresent();
            ScoreHistory child = childOpt.get();
            assertThat(child.getPointChange()).isEqualTo(-20);
            assertThat(child.getTransactionType()).isEqualTo(ScoreTransactionType.REDEEM_FOR_BOOKING);
            assertThat(child.getReferenceHistory()).isNotNull();
            assertThat(child.getReferenceHistory().getIdempotencyKey()).isEqualTo("idemp-parent");
            assertThat(child.getCreatedAt()).isNotNull();
            return null;
        });
    }

    @Test
    void testTierQueries() {
        // Arrange
        createAndSaveTier("SILVER", 0, 0.05);
        createAndSaveTier("GOLD", 400, 0.07);
        createAndSaveTier("DIAMOND", 1000, 0.10);

        // Act & Assert Lowest Tier
        Optional<MembershipTier> lowestTier = membershipTierRepository.findFirstByOrderByMinPointsAsc();
        assertThat(lowestTier).isPresent();
        assertThat(lowestTier.get().getTierName()).isEqualTo("SILVER");

        // Act & Assert Eligible Tiers based on Accumulated Points
        // 0 points -> SILVER
        Optional<MembershipTier> tier0 = membershipTierRepository
                .findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(0);
        assertThat(tier0).isPresent();
        assertThat(tier0.get().getTierName()).isEqualTo("SILVER");

        // 399 points -> SILVER
        Optional<MembershipTier> tier399 = membershipTierRepository
                .findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(399);
        assertThat(tier399).isPresent();
        assertThat(tier399.get().getTierName()).isEqualTo("SILVER");

        // 400 points -> GOLD
        Optional<MembershipTier> tier400 = membershipTierRepository
                .findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(400);
        assertThat(tier400).isPresent();
        assertThat(tier400.get().getTierName()).isEqualTo("GOLD");

        // 450 points -> GOLD
        Optional<MembershipTier> tier450 = membershipTierRepository
                .findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(450);
        assertThat(tier450).isPresent();
        assertThat(tier450.get().getTierName()).isEqualTo("GOLD");

        // 1000 points -> DIAMOND
        Optional<MembershipTier> tier1000 = membershipTierRepository
                .findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(1000);
        assertThat(tier1000).isPresent();
        assertThat(tier1000.get().getTierName()).isEqualTo("DIAMOND");

        // 1500 points -> DIAMOND
        Optional<MembershipTier> tier1500 = membershipTierRepository
                .findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(1500);
        assertThat(tier1500).isPresent();
        assertThat(tier1500.get().getTierName()).isEqualTo("DIAMOND");

        // Act & Assert Next Tiers
        // Next tier for SILVER (0 points) -> GOLD (400 points)
        Optional<MembershipTier> nextTier0 = membershipTierRepository
                .findFirstByMinPointsGreaterThanOrderByMinPointsAsc(0);
        assertThat(nextTier0).isPresent();
        assertThat(nextTier0.get().getTierName()).isEqualTo("GOLD");

        // Next tier for GOLD (450 points) -> DIAMOND (1000 points)
        Optional<MembershipTier> nextTier450 = membershipTierRepository
                .findFirstByMinPointsGreaterThanOrderByMinPointsAsc(450);
        assertThat(nextTier450).isPresent();
        assertThat(nextTier450.get().getTierName()).isEqualTo("DIAMOND");

        // Next tier for DIAMOND (1000 points) -> None
        Optional<MembershipTier> nextTier1000 = membershipTierRepository
                .findFirstByMinPointsGreaterThanOrderByMinPointsAsc(1000);
        assertThat(nextTier1000).isEmpty();
    }

    @Test
    void testThresholdValidation() {
        // Verify we can retrieve correct thresholds
        MembershipTier silver = createAndSaveTier("SILVER", 0, 0.05);
        MembershipTier gold = createAndSaveTier("GOLD", 400, 0.07);
        MembershipTier diamond = createAndSaveTier("DIAMOND", 1000, 0.10);

        assertThat(silver.getMinPoints()).isEqualTo(0);
        assertThat(gold.getMinPoints()).isEqualTo(400);
        assertThat(diamond.getMinPoints()).isEqualTo(1000);
    }

    @Test
    void testIdempotencyKeyUniqueConstraint() {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        UserScore score = createAndSaveUserScore(200L, 100, tier);

        transactionTemplate.execute(status -> {
            ScoreHistory h1 = ScoreHistory.builder()
                    .userScore(score)
                    .pointChange(10)
                    .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                    .balanceBefore(100)
                    .balanceAfter(110)
                    .accumulatedBefore(100)
                    .accumulatedAfter(110)
                    .idempotencyKey("dup-idemp")
                    .build();
            scoreHistoryRepository.save(h1);
            return null;
        });

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                ScoreHistory h2 = ScoreHistory.builder()
                        .userScore(score)
                        .pointChange(20)
                        .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                        .balanceBefore(110)
                        .balanceAfter(130)
                        .accumulatedBefore(110)
                        .accumulatedAfter(130)
                        .idempotencyKey("dup-idemp")
                        .build();
                scoreHistoryRepository.save(h2);
                return null;
            });
        });
    }

    @Test
    void testNullableUniqueConstraints() {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        UserScore score = createAndSaveUserScore(300L, 100, tier);

        // Multiple NULL values for event_id and request_id must be allowed
        transactionTemplate.execute(status -> {
            ScoreHistory h1 = ScoreHistory.builder()
                    .userScore(score)
                    .pointChange(10)
                    .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                    .balanceBefore(100)
                    .balanceAfter(110)
                    .accumulatedBefore(100)
                    .accumulatedAfter(110)
                    .idempotencyKey("key-null-1")
                    .eventId(null)
                    .requestId(null)
                    .build();
            scoreHistoryRepository.save(h1);

            ScoreHistory h2 = ScoreHistory.builder()
                    .userScore(score)
                    .pointChange(10)
                    .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                    .balanceBefore(110)
                    .balanceAfter(120)
                    .accumulatedBefore(110)
                    .accumulatedAfter(120)
                    .idempotencyKey("key-null-2")
                    .eventId(null)
                    .requestId(null)
                    .build();
            scoreHistoryRepository.save(h2);
            return null;
        });

        // Duplicate event_id must fail
        transactionTemplate.execute(status -> {
            ScoreHistory h = ScoreHistory.builder()
                    .userScore(score)
                    .pointChange(10)
                    .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                    .balanceBefore(120)
                    .balanceAfter(130)
                    .accumulatedBefore(120)
                    .accumulatedAfter(130)
                    .idempotencyKey("key-ev-dup-1")
                    .eventId("event-123")
                    .build();
            scoreHistoryRepository.save(h);
            return null;
        });

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                ScoreHistory h = ScoreHistory.builder()
                        .userScore(score)
                        .pointChange(10)
                        .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                        .balanceBefore(130)
                        .balanceAfter(140)
                        .accumulatedBefore(130)
                        .accumulatedAfter(140)
                        .idempotencyKey("key-ev-dup-2")
                        .eventId("event-123")
                        .build();
                scoreHistoryRepository.save(h);
                return null;
            });
        });
    }

    @Test
    void testAtomicDeduction() throws Exception {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        final Long userId = 400L;
        createAndSaveUserScore(userId, 100, tier);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger firstThreadResult = new AtomicInteger();
        AtomicInteger secondThreadResult = new AtomicInteger();

        executor.submit(() -> {
            try {
                startLatch.await();
                int rows = transactionTemplate.execute(status ->
                        userScoreRepository.deductPointsAtomic(userId, 80)
                );
                firstThreadResult.set(rows);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                int rows = transactionTemplate.execute(status ->
                        userScoreRepository.deductPointsAtomic(userId, 80)
                );
                secondThreadResult.set(rows);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        int totalSuccess = firstThreadResult.get() + secondThreadResult.get();
        assertThat(totalSuccess).isEqualTo(1); // Only one should succeed

        Optional<UserScore> scoreOpt = userScoreRepository.findByUserId(userId);
        assertThat(scoreOpt).isPresent();
        assertThat(scoreOpt.get().getCurrentPoints()).isEqualTo(20); // 100 - 80 = 20

        executor.shutdown();
    }

    @Test
    void testPessimisticLocking() throws Exception {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        final Long userId = 500L;
        createAndSaveUserScore(userId, 100, tier);

        final AtomicBoolean threadAHasLock = new AtomicBoolean(false);
        final AtomicBoolean threadBAcquiredLockAfterA = new AtomicBoolean(false);
        final CountDownLatch threadAStarted = new CountDownLatch(1);
        final CountDownLatch threadBFinished = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            transactionTemplate.execute(status -> {
                UserScore u = userScoreRepository.findWithLockByUserId(userId).orElseThrow();
                threadAHasLock.set(true);
                threadAStarted.countDown();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                threadAHasLock.set(false);
                return null;
            });
        });

        threadAStarted.await();

        executor.submit(() -> {
            transactionTemplate.execute(status -> {
                UserScore u = userScoreRepository.findWithLockByUserId(userId).orElseThrow();
                // When thread B successfully acquires the lock, thread A's transaction must have completed,
                // meaning threadAHasLock must be false.
                threadBAcquiredLockAfterA.set(!threadAHasLock.get());
                threadBFinished.countDown();
                return null;
            });
        });

        boolean completed = threadBFinished.await(3, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(threadBAcquiredLockAfterA.get()).isTrue();

        executor.shutdown();
    }

    @Test
    void testTransactionRollbackOnHistoryFailure() {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        final Long userId = 600L;
        UserScore score = createAndSaveUserScore(userId, 100, tier);

        // Pre-save history with eventId "dup-event"
        transactionTemplate.execute(status -> {
            ScoreHistory h = ScoreHistory.builder()
                    .userScore(score)
                    .pointChange(10)
                    .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                    .balanceBefore(100)
                    .balanceAfter(110)
                    .accumulatedBefore(100)
                    .accumulatedAfter(110)
                    .idempotencyKey("idemp-pre")
                    .eventId("dup-event")
                    .build();
            scoreHistoryRepository.save(h);
            return null;
        });

        // Atomic block updating points and writing duplicate history
        assertThrows(Exception.class, () -> {
            transactionTemplate.execute(status -> {
                UserScore u = userScoreRepository.findByUserId(userId).orElseThrow();
                u.setCurrentPoints(200);
                userScoreRepository.save(u);

                ScoreHistory h = ScoreHistory.builder()
                        .userScore(u)
                        .pointChange(10)
                        .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                        .balanceBefore(110)
                        .balanceAfter(120)
                        .accumulatedBefore(110)
                        .accumulatedAfter(120)
                        .idempotencyKey("idemp-new")
                        .eventId("dup-event") // Trigger unique constraint violation on eventId
                        .build();
                scoreHistoryRepository.save(h);
                return null;
            });
        });

        // Verify balance rolled back to 100, not 200
        Optional<UserScore> scoreOpt = userScoreRepository.findByUserId(userId);
        assertThat(scoreOpt).isPresent();
        assertThat(scoreOpt.get().getCurrentPoints()).isEqualTo(100);
    }

    @Test
    void testDeleteRestrict() {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        final Long userId = 700L;
        UserScore score = createAndSaveUserScore(userId, 100, tier);

        // 1. Cannot delete MembershipTier currently used by UserScore
        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                membershipTierRepository.delete(tier);
                return null;
            });
        });

        // 2. Cannot delete referenced ScoreHistory
        transactionTemplate.execute(status -> {
            ScoreHistory parent = ScoreHistory.builder()
                    .userScore(score)
                    .pointChange(10)
                    .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                    .balanceBefore(100)
                    .balanceAfter(110)
                    .accumulatedBefore(100)
                    .accumulatedAfter(110)
                    .idempotencyKey("parent-key")
                    .build();
            ScoreHistory savedParent = scoreHistoryRepository.save(parent);

            ScoreHistory child = ScoreHistory.builder()
                    .userScore(score)
                    .pointChange(-20)
                    .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                    .balanceBefore(110)
                    .balanceAfter(90)
                    .accumulatedBefore(110)
                    .accumulatedAfter(110)
                    .idempotencyKey("child-key")
                    .referenceHistory(savedParent)
                    .build();
            scoreHistoryRepository.save(child);
            return null;
        });

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                ScoreHistory parent = scoreHistoryRepository.findByIdempotencyKey("parent-key").orElseThrow();
                scoreHistoryRepository.delete(parent);
                return null;
            });
        });
    }

    @Test
    void testAtomicAddition() {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        final Long userId = 800L;
        createAndSaveUserScore(userId, 100, tier);

        int rows = transactionTemplate.execute(status ->
                userScoreRepository.addPointsAtomic(userId, 50)
        );
        assertThat(rows).isEqualTo(1);

        Optional<UserScore> scoreOpt = userScoreRepository.findByUserId(userId);
        assertThat(scoreOpt).isPresent();
        assertThat(scoreOpt.get().getCurrentPoints()).isEqualTo(150);
        assertThat(scoreOpt.get().getAccumulatedPoints()).isEqualTo(150);
    }

    @Test
    void testAtomicAdditionCurrentPointsOnly() {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        final Long userId = 900L;
        createAndSaveUserScore(userId, 100, tier);

        int rows = transactionTemplate.execute(status ->
                userScoreRepository.addCurrentPointsOnlyAtomic(userId, 50)
        );
        assertThat(rows).isEqualTo(1);

        Optional<UserScore> scoreOpt = userScoreRepository.findByUserId(userId);
        assertThat(scoreOpt).isPresent();
        assertThat(scoreOpt.get().getCurrentPoints()).isEqualTo(150);
        assertThat(scoreOpt.get().getAccumulatedPoints()).isEqualTo(100); // remains unchanged
    }

    @Test
    void testFindByBookingId() {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        UserScore score = createAndSaveUserScore(1000L, 100, tier);

        transactionTemplate.execute(status -> {
            ScoreHistory h1 = ScoreHistory.builder()
                    .userScore(score)
                    .bookingId(888L)
                    .pointChange(10)
                    .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                    .balanceBefore(100)
                    .balanceAfter(110)
                    .accumulatedBefore(100)
                    .accumulatedAfter(110)
                    .idempotencyKey("idemp-b1")
                    .build();
            scoreHistoryRepository.save(h1);

            ScoreHistory h2 = ScoreHistory.builder()
                    .userScore(score)
                    .bookingId(888L)
                    .pointChange(-20)
                    .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                    .balanceBefore(110)
                    .balanceAfter(90)
                    .accumulatedBefore(110)
                    .accumulatedAfter(110)
                    .idempotencyKey("idemp-b2")
                    .build();
            scoreHistoryRepository.save(h2);

            ScoreHistory h3 = ScoreHistory.builder()
                    .userScore(score)
                    .bookingId(999L)
                    .pointChange(5)
                    .transactionType(ScoreTransactionType.EARN_BY_BOOKING)
                    .balanceBefore(90)
                    .balanceAfter(95)
                    .accumulatedBefore(110)
                    .accumulatedAfter(115)
                    .idempotencyKey("idemp-b3")
                    .build();
            scoreHistoryRepository.save(h3);

            return null;
        });

        List<ScoreHistory> histories = scoreHistoryRepository.findByBookingId(888L);
        assertThat(histories).hasSize(2);
        assertThat(histories).extracting(ScoreHistory::getIdempotencyKey)
                .containsExactlyInAnyOrder("idemp-b1", "idemp-b2");
    }

    @Test
    void testFindByReconciliationStatusOrderByCreatedAtAsc() throws Exception {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        UserScore score = createAndSaveUserScore(1100L, 100, tier);

        transactionTemplate.execute(status -> {
            ScoreHistory h1 = ScoreHistory.builder()
                    .userScore(score)
                    .pointChange(-50)
                    .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                    .balanceBefore(100)
                    .balanceAfter(50)
                    .accumulatedBefore(100)
                    .accumulatedAfter(100)
                    .idempotencyKey("idemp-rec-1")
                    .reconciliationStatus(ReconciliationStatus.PENDING)
                    .build();
            scoreHistoryRepository.save(h1);
            return null;
        });

        // sleep brief moment to guarantee distinct createdAt timestamps
        Thread.sleep(100);

        transactionTemplate.execute(status -> {
            ScoreHistory h2 = ScoreHistory.builder()
                    .userScore(score)
                    .pointChange(-30)
                    .transactionType(ScoreTransactionType.REDEEM_FOR_BOOKING)
                    .balanceBefore(50)
                    .balanceAfter(20)
                    .accumulatedBefore(100)
                    .accumulatedAfter(100)
                    .idempotencyKey("idemp-rec-2")
                    .reconciliationStatus(ReconciliationStatus.PENDING)
                    .build();
            scoreHistoryRepository.save(h2);
            return null;
        });

        List<ScoreHistory> pendingHistories = scoreHistoryRepository.findByReconciliationStatusOrderByCreatedAtAsc(ReconciliationStatus.PENDING);
        assertThat(pendingHistories).hasSize(2);
        assertThat(pendingHistories.get(0).getIdempotencyKey()).isEqualTo("idemp-rec-1");
        assertThat(pendingHistories.get(1).getIdempotencyKey()).isEqualTo("idemp-rec-2");
    }

    @Test
    void testDeductAccumulatedPointsAtomic() {
        MembershipTier tier = createAndSaveTier("SILVER", 0, 0.05);
        final Long userId = 1200L;
        createAndSaveUserScore(userId, 100, tier);

        // Deduct 40 points -> should succeed (returns 1 row)
        int rows1 = transactionTemplate.execute(status ->
                userScoreRepository.deductAccumulatedPointsAtomic(userId, 40)
        );
        assertThat(rows1).isEqualTo(1);

        Optional<UserScore> scoreOpt1 = userScoreRepository.findByUserId(userId);
        assertThat(scoreOpt1).isPresent();
        assertThat(scoreOpt1.get().getAccumulatedPoints()).isEqualTo(60);

        // Deduct 70 points -> should fail because 60 < 70 (returns 0 rows)
        int rows2 = transactionTemplate.execute(status ->
                userScoreRepository.deductAccumulatedPointsAtomic(userId, 70)
        );
        assertThat(rows2).isEqualTo(0);

        Optional<UserScore> scoreOpt2 = userScoreRepository.findByUserId(userId);
        assertThat(scoreOpt2).isPresent();
        assertThat(scoreOpt2.get().getAccumulatedPoints()).isEqualTo(60); // remains 60
    }

    @Test
    void testUserScoreLazyInitialization() {
        // Create only the MembershipTier records (do not manually insert a UserScore)
        createAndSaveTier("SILVER", 0, 0.05);
        createAndSaveTier("GOLD", 400, 0.07);
        createAndSaveTier("DIAMOND", 1000, 0.10);

        final Long brandNewUserId = 1300L;

        // Trigger the persistence initialization flow exactly as the application would on first access
        UserScore initializedScore = transactionTemplate.execute(status -> {
            Optional<UserScore> existingScore = userScoreRepository.findByUserId(brandNewUserId);
            if (existingScore.isPresent()) {
                return existingScore.get();
            } else {
                MembershipTier lowestTier = membershipTierRepository.findFirstByOrderByMinPointsAsc()
                        .orElseThrow(() -> new IllegalStateException("No membership tiers configured"));
                
                UserScore newScore = UserScore.builder()
                        .userId(brandNewUserId)
                        .currentPoints(0)
                        .accumulatedPoints(0)
                        .currentTier(lowestTier)
                        .build();
                return userScoreRepository.save(newScore);
            }
        });

        // Verify that the created record has correct values
        assertThat(initializedScore).isNotNull();
        assertThat(initializedScore.getUserId()).isEqualTo(brandNewUserId);
        assertThat(initializedScore.getCurrentPoints()).isEqualTo(0);
        assertThat(initializedScore.getAccumulatedPoints()).isEqualTo(0);
        
        MembershipTier expectedLowestTier = membershipTierRepository.findFirstByOrderByMinPointsAsc().orElseThrow();
        assertThat(initializedScore.getCurrentTier().getId()).isEqualTo(expectedLowestTier.getId());
        assertThat(initializedScore.getCurrentTier().getTierName()).isEqualTo(expectedLowestTier.getTierName());
    }
}
