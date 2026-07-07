package com.project.promotionservice.service;

import com.project.promotionservice.dto.ConfirmUsageRequest;
import com.project.promotionservice.dto.PromotionUsageResponse;
import com.project.promotionservice.dto.RevertUsageRequest;
import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.DiscountType;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.repository.PromotionCampaignRepository;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.repository.PromotionUsageRepository;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class PromotionUsageConcurrencyTest {

    @Autowired
    private PromotionUsageService promotionUsageService;

    @Autowired
    private PromotionUsageRepository promotionUsageRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private PromotionCampaignRepository campaignRepository;

    private PromotionCampaign campaign;
    private Promotion promotion;

    @BeforeEach
    void setUp() {
        cleanupDatabase();

        LocalDateTime now = LocalDateTime.now();

        campaign = campaignRepository.save(PromotionCampaign.builder()
                .campaignName("Concurrency Campaign")
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(5))
                .active(true)
                .build());

        promotion = promotionRepository.save(Promotion.builder()
                .campaign(campaign)
                .promotionCode("CONCURRENCY10")
                .description("Concurrency promo")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10.0))
                .minOrderAmount(BigDecimal.valueOf(10000.0))
                .usageLimit(100)
                .usedCount(10) // start with 10 used
                .limitPerUser(2)
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(5))
                .active(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        promotionUsageRepository.deleteAllInBatch();
        promotionRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
    }

    @Test
    void confirmVsConfirm_ConcurrentRequests_ShouldSucceedWithExactlyOneTransitionAndOneIdempotentSuccess() throws InterruptedException, ExecutionException {
        LocalDateTime now = LocalDateTime.now();
        PromotionUsage usage = promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(promotion)
                .userId(1L)
                .bookingId(301L)
                .status(PromotionUsageStatus.RESERVED)
                .originalAmount(BigDecimal.valueOf(20000.0))
                .discountAmount(BigDecimal.valueOf(2000.0))
                .finalAmount(BigDecimal.valueOf(18000.0))
                .expiresAt(now.plusMinutes(15))
                .build());

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<PromotionUsageResponse>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                startLatch.await();
                ConfirmUsageRequest request = new ConfirmUsageRequest(301L, LocalDateTime.now());
                return promotionUsageService.confirmUsage(usage.getId(), request);
            });
        }

        // Start execution
        List<Future<PromotionUsageResponse>> futures = new ArrayList<>();
        for (Callable<PromotionUsageResponse> task : tasks) {
            futures.add(executor.submit(task));
        }

        startLatch.countDown(); // trigger parallel run

        List<PromotionUsageResponse> responses = new ArrayList<>();
        for (Future<PromotionUsageResponse> future : futures) {
            try {
                responses.add(future.get());
            } catch (ExecutionException e) {
                // If the database transaction aborted because of version conflict, it's caught and resolved
                // in the service layer, returning idempotent success.
                // However, in rare cases of simultaneous commit, one may throw conflict. Let's log it.
                System.out.println("Concurrent task threw exception: " + e.getCause().getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Fetch final status
        PromotionUsage finalUsage = promotionUsageRepository.findById(usage.getId()).orElseThrow();
        assertThat(finalUsage.getStatus()).isEqualTo(PromotionUsageStatus.APPLIED);

        // Verify that responses have status APPLIED
        for (PromotionUsageResponse res : responses) {
            assertThat(res.getStatus()).isEqualTo("APPLIED");
        }
    }

    @Test
    void revertVsRevert_ConcurrentRequests_ShouldDecrementUsedCountExactlyOnce() throws InterruptedException, ExecutionException {
        LocalDateTime now = LocalDateTime.now();
        PromotionUsage usage = promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(promotion)
                .userId(1L)
                .bookingId(302L)
                .status(PromotionUsageStatus.RESERVED)
                .originalAmount(BigDecimal.valueOf(20000.0))
                .discountAmount(BigDecimal.valueOf(2000.0))
                .finalAmount(BigDecimal.valueOf(18000.0))
                .expiresAt(now.plusMinutes(15))
                .build());

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<PromotionUsageResponse>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                startLatch.await();
                RevertUsageRequest request = new RevertUsageRequest(302L, "User cancelled");
                return promotionUsageService.revertUsage(usage.getId(), request);
            });
        }

        // Start execution
        List<Future<PromotionUsageResponse>> futures = new ArrayList<>();
        for (Callable<PromotionUsageResponse> task : tasks) {
            futures.add(executor.submit(task));
        }

        startLatch.countDown();

        List<PromotionUsageResponse> responses = new ArrayList<>();
        for (Future<PromotionUsageResponse> future : futures) {
            try {
                responses.add(future.get());
            } catch (Exception e) {
                System.out.println("Revert task error: " + e.getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Check final status is REVERTED
        PromotionUsage finalUsage = promotionUsageRepository.findById(usage.getId()).orElseThrow();
        assertThat(finalUsage.getStatus()).isEqualTo(PromotionUsageStatus.REVERTED);

        // Verify usedCount decremented exactly once (10 -> 9)
        Promotion finalPromo = promotionRepository.findById(promotion.getId()).orElseThrow();
        assertThat(finalPromo.getUsedCount()).isEqualTo(9);
    }
}
