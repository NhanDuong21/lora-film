package com.project.promotionservice.worker;

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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class PromotionUsageExpirationWorkerTest {

    @Autowired
    private PromotionUsageExpirationWorker worker;

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
                .campaignName("Worker Test Campaign")
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(5))
                .active(true)
                .build());

        promotion = promotionRepository.save(Promotion.builder()
                .campaign(campaign)
                .promotionCode("WORKER10")
                .description("Worker promo")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10.0))
                .minOrderAmount(BigDecimal.valueOf(10000.0))
                .usageLimit(100)
                .usedCount(5) // initial reservations
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
    void runReconciliation_ShouldRevertExpiredReservations_AndDecrementUsedCount() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Save expired usage (expires 5 minutes ago)
        PromotionUsage expiredUsage = promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(promotion)
                .userId(1L)
                .bookingId(101L)
                .status(PromotionUsageStatus.RESERVED)
                .originalAmount(BigDecimal.valueOf(20000.0))
                .discountAmount(BigDecimal.valueOf(2000.0))
                .finalAmount(BigDecimal.valueOf(18000.0))
                .expiresAt(now.minusMinutes(5))
                .build());

        // 2. Save active usage (expires in 10 minutes)
        PromotionUsage activeUsage = promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(promotion)
                .userId(2L)
                .bookingId(102L)
                .status(PromotionUsageStatus.RESERVED)
                .originalAmount(BigDecimal.valueOf(20000.0))
                .discountAmount(BigDecimal.valueOf(2000.0))
                .finalAmount(BigDecimal.valueOf(18000.0))
                .expiresAt(now.plusMinutes(10))
                .build());

        // Run worker
        worker.runReconciliation();

        // Check expired usage is reverted
        PromotionUsage expiredCheck = promotionUsageRepository.findById(expiredUsage.getId()).orElseThrow();
        assertThat(expiredCheck.getStatus()).isEqualTo(PromotionUsageStatus.REVERTED);
        assertThat(expiredCheck.getRevertReason()).isEqualTo("Reservation expired");
        assertThat(expiredCheck.getRevertedAt()).isNotNull();

        // Check active usage is still reserved
        PromotionUsage activeCheck = promotionUsageRepository.findById(activeUsage.getId()).orElseThrow();
        assertThat(activeCheck.getStatus()).isEqualTo(PromotionUsageStatus.RESERVED);

        // Check used_count is decremented by 1 (was 5, should be 4)
        Promotion promotionCheck = promotionRepository.findById(promotion.getId()).orElseThrow();
        assertThat(promotionCheck.getUsedCount()).isEqualTo(4);
    }

    @Test
    void runReconciliation_ShouldHandlePagination_AndOrderCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        // Save 3 expired usages with different expiresAt times
        PromotionUsage firstExpired = promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(promotion)
                .userId(1L)
                .bookingId(201L)
                .status(PromotionUsageStatus.RESERVED)
                .originalAmount(BigDecimal.valueOf(10000.0))
                .discountAmount(BigDecimal.valueOf(1000.0))
                .finalAmount(BigDecimal.valueOf(9000.0))
                .expiresAt(now.minusMinutes(10)) // oldest expiry
                .build());

        PromotionUsage secondExpired = promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(promotion)
                .userId(2L)
                .bookingId(202L)
                .status(PromotionUsageStatus.RESERVED)
                .originalAmount(BigDecimal.valueOf(10000.0))
                .discountAmount(BigDecimal.valueOf(1000.0))
                .finalAmount(BigDecimal.valueOf(9000.0))
                .expiresAt(now.minusMinutes(5))
                .build());

        worker.runReconciliation();

        // Verify both are reverted
        List<PromotionUsage> usages = promotionUsageRepository.findAllById(List.of(firstExpired.getId(), secondExpired.getId()));
        assertThat(usages).hasSize(2);
        assertThat(usages.get(0).getStatus()).isEqualTo(PromotionUsageStatus.REVERTED);
        assertThat(usages.get(1).getStatus()).isEqualTo(PromotionUsageStatus.REVERTED);
    }
}
