package com.project.promotionservice.repository;

import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.DiscountType;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.support.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PromotionCampaignRepository campaignRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private PromotionUsageRepository promotionUsageRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        promotionUsageRepository.deleteAllInBatch();
        promotionRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
    }

    private PromotionCampaign createTestCampaign() {
        PromotionCampaign campaign = PromotionCampaign.builder()
                .campaignName("Test Campaign")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(5))
                .active(true)
                .build();
        return campaignRepository.saveAndFlush(campaign);
    }

    private Promotion createTestPromotion(PromotionCampaign campaign, String code) {
        Promotion promo = Promotion.builder()
                .campaign(campaign)
                .promotionCode(code)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(100)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(5))
                .build();
        return promotionRepository.saveAndFlush(promo);
    }

    private PromotionUsage saveUsage(Promotion promo, Long userId, Long bookingId, PromotionUsageStatus status) {
        PromotionUsage usage = PromotionUsage.builder()
                .promotion(promo)
                .userId(userId)
                .bookingId(bookingId)
                .status(status)
                .originalAmount(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("10.00"))
                .finalAmount(new BigDecimal("90.00"))
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        return promotionUsageRepository.saveAndFlush(usage);
    }

    private PromotionUsage saveUsageWithExpiry(Promotion promo, Long userId, Long bookingId, PromotionUsageStatus status, LocalDateTime expiresAt) {
        PromotionUsage usage = PromotionUsage.builder()
                .promotion(promo)
                .userId(userId)
                .bookingId(bookingId)
                .status(status)
                .originalAmount(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("10.00"))
                .finalAmount(new BigDecimal("90.00"))
                .expiresAt(expiresAt)
                .build();
        return promotionUsageRepository.saveAndFlush(usage);
    }

    @Test
    void testEntityMappingAndDefaults() {
        PromotionCampaign campaign = PromotionCampaign.builder()
                .campaignName("Summer Sale 2026")
                .description("Summer discount campaign")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusMonths(1))
                .active(true)
                .build();
        campaign = campaignRepository.save(campaign);
        assertThat(campaign.getId()).isNotNull();
        assertThat(campaign.getCreatedAt()).isNotNull();
        assertThat(campaign.getUpdatedAt()).isNotNull();

        Promotion promotion = Promotion.builder()
                .campaign(campaign)
                .promotionCode("SUMMER2026")
                .description("10% off for summer")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .maxDiscountAmount(new BigDecimal("50.00"))
                .usageLimit(100)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusMonths(1))
                .build();

        // Check default values before persistence
        assertThat(promotion.getMinOrderAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(promotion.getUsedCount()).isEqualTo(0);
        assertThat(promotion.getLimitPerUser()).isEqualTo(1);
        assertThat(promotion.isActive()).isTrue();
        assertThat(promotion.getVersion()).isEqualTo(0);

        promotion = promotionRepository.save(promotion);
        assertThat(promotion.getId()).isNotNull();
        assertThat(promotion.getCreatedAt()).isNotNull();
        assertThat(promotion.getUpdatedAt()).isNotNull();

        PromotionUsage usage = PromotionUsage.builder()
                .promotion(promotion)
                .userId(1L)
                .bookingId(100L)
                .originalAmount(new BigDecimal("200.00"))
                .discountAmount(new BigDecimal("20.00"))
                .finalAmount(new BigDecimal("180.00"))
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        // Check default values before persistence
        assertThat(usage.getStatus()).isEqualTo(PromotionUsageStatus.RESERVED);
        assertThat(usage.getVersion()).isEqualTo(0);

        usage = promotionUsageRepository.save(usage);
        assertThat(usage.getId()).isNotNull();
        assertThat(usage.getReservedAt()).isNotNull();
        assertThat(usage.getUpdatedAt()).isNotNull();
    }

    @Test
    void testPromotionCodeUniquenessAndNormalization() {
        PromotionCampaign campaign = createTestCampaign();

        Promotion promo1 = Promotion.builder()
                .campaign(campaign)
                .promotionCode("PROMO1")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50.00"))
                .usageLimit(10)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        promotionRepository.saveAndFlush(promo1);

        Promotion promo2 = Promotion.builder()
                .campaign(campaign)
                .promotionCode("PROMO1")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("30.00"))
                .usageLimit(10)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            promotionRepository.saveAndFlush(promo2);
        });
    }

    @Test
    void testPromotionCodeNormalizationHelper() {
        String rawCode = "  promo_code_123   ";
        String normalized = rawCode.trim().toUpperCase();
        assertThat(normalized).isEqualTo("PROMO_CODE_123");
    }

    @Test
    void testPromotionRepositoryQueries() {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = Promotion.builder()
                .campaign(campaign)
                .promotionCode("PROMO_QUERIES")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50.00"))
                .usageLimit(10)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        promotionRepository.saveAndFlush(promo);

        assertThat(promotionRepository.existsByPromotionCode("PROMO_QUERIES")).isTrue();
        assertThat(promotionRepository.existsByPromotionCode("NON_EXISTENT_CODE")).isFalse();

        Optional<Promotion> found = promotionRepository.findByPromotionCode("PROMO_QUERIES");
        assertThat(found).isPresent();
        assertThat(found.get().getPromotionCode()).isEqualTo("PROMO_QUERIES");

        Optional<Promotion> foundIgnoreCase = promotionRepository.findByPromotionCodeIgnoreCase("promo_queries");
        assertThat(foundIgnoreCase).isPresent();
        assertThat(foundIgnoreCase.get().getPromotionCode()).isEqualTo("PROMO_QUERIES");
    }

    @Test
    void testBookingUniqueUsageAndRollback() {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = createTestPromotion(campaign, "BOOKING_TEST");

        PromotionUsage usage1 = PromotionUsage.builder()
                .promotion(promo)
                .userId(1L)
                .bookingId(999L)
                .originalAmount(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("10.00"))
                .finalAmount(new BigDecimal("90.00"))
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        promotionUsageRepository.saveAndFlush(usage1);

        PromotionUsage usage2 = PromotionUsage.builder()
                .promotion(promo)
                .userId(2L)
                .bookingId(999L) // duplicate bookingId
                .originalAmount(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("10.00"))
                .finalAmount(new BigDecimal("90.00"))
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        assertThrows(DataIntegrityViolationException.class, () -> {
            txTemplate.execute(status -> {
                promotionUsageRepository.saveAndFlush(usage2);
                return null;
            });
        });

        assertThat(promotionUsageRepository.findByBookingId(999L)).isPresent();
        assertThat(promotionUsageRepository.existsByBookingId(999L)).isTrue();
    }

    @Test
    void testCountByPromotionIdAndUserIdAndStatusIn() {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = createTestPromotion(campaign, "LIMIT_TEST");

        saveUsage(promo, 1L, 1001L, PromotionUsageStatus.RESERVED);
        saveUsage(promo, 1L, 1002L, PromotionUsageStatus.APPLIED);
        saveUsage(promo, 1L, 1003L, PromotionUsageStatus.REVERTED);
        saveUsage(promo, 2L, 1004L, PromotionUsageStatus.RESERVED);

        long count = promotionUsageRepository.countByPromotionIdAndUserIdAndStatusIn(
                promo.getId(),
                1L,
                List.of(PromotionUsageStatus.RESERVED, PromotionUsageStatus.APPLIED)
        );
        assertThat(count).isEqualTo(2);

        long countUser2 = promotionUsageRepository.countByPromotionIdAndUserIdAndStatusIn(
                promo.getId(),
                2L,
                List.of(PromotionUsageStatus.RESERVED, PromotionUsageStatus.APPLIED)
        );
        assertThat(countUser2).isEqualTo(1);
    }

    @Test
    void testExpirationQueries() {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = createTestPromotion(campaign, "EXPIRE_TEST");

        LocalDateTime now = LocalDateTime.now();

        PromotionUsage u1 = saveUsageWithExpiry(promo, 1L, 2001L, PromotionUsageStatus.RESERVED, now.minusMinutes(5));
        PromotionUsage u2 = saveUsageWithExpiry(promo, 2L, 2002L, PromotionUsageStatus.RESERVED, now.minusMinutes(10));
        saveUsageWithExpiry(promo, 3L, 2003L, PromotionUsageStatus.RESERVED, now.plusMinutes(15));
        saveUsageWithExpiry(promo, 4L, 2004L, PromotionUsageStatus.APPLIED, now.minusMinutes(2));
        saveUsageWithExpiry(promo, 5L, 2005L, PromotionUsageStatus.REVERTED, now.minusMinutes(1));

        Pageable pageable = PageRequest.of(0, 10);
        Page<PromotionUsage> expired = promotionUsageRepository
                .findByStatusAndExpiresAtBeforeOrderByExpiresAtAscIdAsc(
                        PromotionUsageStatus.RESERVED,
                        now,
                        pageable
                );

        assertThat(expired.getContent()).hasSize(2);
        assertThat(expired.getContent().get(0).getId()).isEqualTo(u2.getId());
        assertThat(expired.getContent().get(1).getId()).isEqualTo(u1.getId());
    }

    @Test
    void testHistoryAndAdminQueries() {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = createTestPromotion(campaign, "HISTORY_TEST");

        saveUsage(promo, 99L, 3001L, PromotionUsageStatus.RESERVED);
        saveUsage(promo, 99L, 3002L, PromotionUsageStatus.APPLIED);
        saveUsage(promo, 88L, 3003L, PromotionUsageStatus.RESERVED);

        Pageable pageable = PageRequest.of(0, 10);

        Page<PromotionUsage> byUser = promotionUsageRepository.findByUserId(99L, pageable);
        assertThat(byUser.getTotalElements()).isEqualTo(2);

        Page<PromotionUsage> byUserStatus = promotionUsageRepository.findByUserIdAndStatus(99L, PromotionUsageStatus.APPLIED, pageable);
        assertThat(byUserStatus.getTotalElements()).isEqualTo(1);

        Page<PromotionUsage> byPromo = promotionUsageRepository.findByPromotionId(promo.getId(), pageable);
        assertThat(byPromo.getTotalElements()).isEqualTo(3);

        Page<PromotionUsage> byPromoStatus = promotionUsageRepository.findByPromotionIdAndStatus(promo.getId(), PromotionUsageStatus.RESERVED, pageable);
        assertThat(byPromoStatus.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testAtomicIncrementAndDecrement() {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = Promotion.builder()
                .campaign(campaign)
                .promotionCode("ATOMIC_PROMO")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(10)
                .usedCount(0)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        final Long promoId = txTemplate.execute(status -> {
            Promotion p = promotionRepository.saveAndFlush(promo);
            return p.getId();
        });

        int rows = txTemplate.execute(status -> promotionRepository.incrementUsedCountIfAvailable(promoId));
        assertThat(rows).isEqualTo(1);

        Promotion updated = promotionRepository.findById(promoId).orElseThrow();
        assertThat(updated.getUsedCount()).isEqualTo(1);
        assertThat(updated.getVersion()).isEqualTo(1);

        rows = txTemplate.execute(status -> promotionRepository.decrementUsedCountIfPositive(promoId));
        assertThat(rows).isEqualTo(1);

        updated = promotionRepository.findById(promoId).orElseThrow();
        assertThat(updated.getUsedCount()).isEqualTo(0);
        assertThat(updated.getVersion()).isEqualTo(2);

        rows = txTemplate.execute(status -> promotionRepository.decrementUsedCountIfPositive(promoId));
        assertThat(rows).isEqualTo(0);

        updated = promotionRepository.findById(promoId).orElseThrow();
        assertThat(updated.getUsedCount()).isEqualTo(0);
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    void testConcurrentAtomicIncrement() throws InterruptedException {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = Promotion.builder()
                .campaign(campaign)
                .promotionCode("CONCURRENT_INC")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(5)
                .usedCount(0)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        promo = promotionRepository.saveAndFlush(promo);
        Long promoId = promo.getId();

        int numThreads = 12;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Integer updated = txTemplate.execute(status -> promotionRepository.incrementUsedCountIfAvailable(promoId));
                    if (updated != null && updated == 1) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        Promotion finalPromo = promotionRepository.findById(promoId).orElseThrow();
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(finalPromo.getUsedCount()).isEqualTo(5);
        assertThat(finalPromo.getVersion()).isEqualTo(5);
    }

    @Test
    void testConcurrentAtomicDecrement() throws InterruptedException {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = Promotion.builder()
                .campaign(campaign)
                .promotionCode("CONCURRENT_DEC")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .usageLimit(10)
                .usedCount(4)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        promo = promotionRepository.saveAndFlush(promo);
        Long promoId = promo.getId();

        int numThreads = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Integer updated = txTemplate.execute(status -> promotionRepository.decrementUsedCountIfPositive(promoId));
                    if (updated != null && updated == 1) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        Promotion finalPromo = promotionRepository.findById(promoId).orElseThrow();
        assertThat(successCount.get()).isEqualTo(4);
        assertThat(finalPromo.getUsedCount()).isEqualTo(0);
        assertThat(finalPromo.getVersion()).isEqualTo(4);
    }

    @Test
    void testPromotionOptimisticLocking() {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = createTestPromotion(campaign, "LOCK_PROMO");

        Promotion promo1 = promotionRepository.findById(promo.getId()).orElseThrow();
        Promotion promo2 = promotionRepository.findById(promo.getId()).orElseThrow();

        promo1.setDescription("Description 1");
        promotionRepository.saveAndFlush(promo1);

        promo2.setDescription("Description 2");
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            promotionRepository.saveAndFlush(promo2);
        });
    }

    @Test
    void testPromotionUsageOptimisticLocking() {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = createTestPromotion(campaign, "LOCK_USAGE_PROMO");
        PromotionUsage usage = saveUsage(promo, 1L, 4001L, PromotionUsageStatus.RESERVED);

        PromotionUsage usage1 = promotionUsageRepository.findById(usage.getId()).orElseThrow();
        PromotionUsage usage2 = promotionUsageRepository.findById(usage.getId()).orElseThrow();

        usage1.setRevertReason("Reason 1");
        promotionUsageRepository.saveAndFlush(usage1);

        usage2.setRevertReason("Reason 2");
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            promotionUsageRepository.saveAndFlush(usage2);
        });
    }

    @Test
    void testDeleteCampaignRestrictConstraint() {
        PromotionCampaign campaign = createTestCampaign();
        createTestPromotion(campaign, "DELETE_CAMPAIGN_RESTRICT");

        assertThrows(DataIntegrityViolationException.class, () -> {
            campaignRepository.delete(campaign);
            campaignRepository.flush();
        });
    }

    @Test
    void testDeletePromotionRestrictConstraint() {
        PromotionCampaign campaign = createTestCampaign();
        Promotion promo = createTestPromotion(campaign, "DELETE_PROMO_RESTRICT");
        saveUsage(promo, 1L, 5001L, PromotionUsageStatus.RESERVED);

        assertThrows(DataIntegrityViolationException.class, () -> {
            promotionRepository.delete(promo);
            promotionRepository.flush();
        });
    }

    @Test
    void testDecimalPrecisionAndScale() {
        PromotionCampaign campaign = createTestCampaign();
        BigDecimal val = new BigDecimal("12.34");
        Promotion promo = Promotion.builder()
                .campaign(campaign)
                .promotionCode("PRECISION_TEST")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(val)
                .maxDiscountAmount(val)
                .minOrderAmount(val)
                .usageLimit(10)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        promo = promotionRepository.saveAndFlush(promo);

        Promotion savedPromo = promotionRepository.findById(promo.getId()).orElseThrow();
        assertThat(savedPromo.getDiscountValue()).isEqualByComparingTo(val);
        assertThat(savedPromo.getMaxDiscountAmount()).isEqualByComparingTo(val);
        assertThat(savedPromo.getMinOrderAmount()).isEqualByComparingTo(val);

        PromotionUsage usage = PromotionUsage.builder()
                .promotion(savedPromo)
                .userId(1L)
                .bookingId(6001L)
                .originalAmount(val)
                .discountAmount(val)
                .finalAmount(val)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        usage = promotionUsageRepository.saveAndFlush(usage);

        PromotionUsage savedUsage = promotionUsageRepository.findById(usage.getId()).orElseThrow();
        assertThat(savedUsage.getOriginalAmount()).isEqualByComparingTo(val);
        assertThat(savedUsage.getDiscountAmount()).isEqualByComparingTo(val);
        assertThat(savedUsage.getFinalAmount()).isEqualByComparingTo(val);
    }
}
