package com.project.promotionservice.reservation;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponCreateRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CouponResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponType;
import com.project.promotionservice.benefit.enums.BenefitEnums.DistributionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.benefit.service.CouponService;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignType;
import com.project.promotionservice.promotion.enums.FundingSource;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PromotionReservationConcurrencyTest {

    @Autowired
    private PromotionCampaignRepository campaignRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private PromotionReservationService reservationService;

    @Test
    void onlyOneConcurrentTransactionCanHoldTheSameBenefit() throws Exception {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = couponService.create(couponRequest(campaign.getPublicId()), "admin");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Object>> futures = new ArrayList<>();
            for (int index = 0; index < 2; index++) {
                int attempt = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return reservationService.reserve(
                                reservationRequest(coupon.getCode(), "concurrent-user-" + attempt),
                                "concurrent-reserve-" + UUID.randomUUID(),
                                "booking-service");
                    } catch (BusinessException exception) {
                        return exception;
                    }
                }));
            }
            ready.await();
            start.countDown();

            List<Object> results = List.of(futures.get(0).get(), futures.get(1).get());
            List<String> diagnostics = results.stream()
                    .map(result -> result instanceof BusinessException exception
                            ? exception.getErrorCode() + ": " + exception.getMessage()
                            : result.getClass().getSimpleName())
                    .toList();
            assertThat(results.stream().filter(ReservationResponse.class::isInstance))
                    .as("Concurrent results: %s", diagnostics)
                    .hasSize(1);
            assertThat(results.stream().filter(BusinessException.class::isInstance)).hasSize(1);
            ReservationResponse winner = results.stream()
                    .filter(ReservationResponse.class::isInstance)
                    .map(ReservationResponse.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertThat(winner.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        }
    }

    @Test
    void concurrentReuseOfOneIdempotencyKeyCannotCreateDifferentReservations() throws Exception {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse firstCoupon = couponService.create(couponRequest(campaign.getPublicId()), "admin");
        CouponResponse secondCoupon = couponService.create(couponRequest(campaign.getPublicId()), "admin");
        List<ReserveRequest> requests = List.of(
                reservationRequest(firstCoupon.getCode(), "idempotent-user-1"),
                reservationRequest(secondCoupon.getCode(), "idempotent-user-2"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Object>> futures = new ArrayList<>();
            for (ReserveRequest request : requests) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return reservationService.reserve(
                                request, "same-idempotency-key", "booking-service");
                    } catch (BusinessException exception) {
                        return exception;
                    }
                }));
            }
            ready.await();
            start.countDown();

            List<Object> results = List.of(futures.get(0).get(), futures.get(1).get());
            assertThat(results.stream().filter(ReservationResponse.class::isInstance)).hasSize(1);
            assertThat(results.stream().filter(BusinessException.class::isInstance)).hasSize(1);
        }
    }

    private PromotionCampaign activeCampaign() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setCode("CONCURRENT-" + suffix);
        campaign.setName("Concurrency Test " + suffix);
        campaign.setSlug("concurrency-test-" + suffix);
        campaign.setCampaignType(CampaignType.COUPON);
        campaign.setFundingSource(FundingSource.SYSTEM);
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        campaign.setLegalStatus(LegalStatus.PASSED);
        campaign.setStartAt(Instant.now().minus(1, ChronoUnit.DAYS));
        campaign.setEndAt(Instant.now().plus(10, ChronoUnit.DAYS));
        campaign.setBudgetAmount(new BigDecimal("1000000"));
        campaign.setBudgetRemaining(new BigDecimal("1000000"));
        campaign.setBudgetUsed(BigDecimal.ZERO);
        campaign.setBudgetReserved(BigDecimal.ZERO);
        campaign.setMaxRedemptions(100);
        campaign.setCreatedBy("admin");
        campaign.setUpdatedBy("admin");
        return campaignRepository.save(campaign);
    }

    private CouponCreateRequest couponRequest(String campaignPublicId) {
        CouponCreateRequest request = new CouponCreateRequest();
        request.setCampaignPublicId(campaignPublicId);
        request.setCode("CONCURRENT-CPN-" + UUID.randomUUID().toString().substring(0, 8));
        request.setName("Concurrent coupon");
        request.setCouponType(CouponType.PUBLIC);
        request.setStatus(CouponStatus.ACTIVE);
        request.setDistributionType(DistributionType.PUBLIC);
        request.setMaxRedemptions(10);
        request.setMaxRedemptionsPerUser(1);
        request.setValidFrom(Instant.now().minus(1, ChronoUnit.DAYS));
        request.setValidTo(Instant.now().plus(1, ChronoUnit.DAYS));
        request.setConditionsJson(JsonNodeFactory.instance.objectNode());
        ObjectNode action = JsonNodeFactory.instance.objectNode();
        action.put("discountType", "FIXED_AMOUNT");
        action.put("discountValue", 20_000);
        request.setActionsJson(action);
        return request;
    }

    private ReserveRequest reservationRequest(String code, String userPublicId) {
        ReserveRequest request = new ReserveRequest();
        request.setBenefitType(RedemptionType.COUPON);
        request.setCode(code);
        request.setUserPublicId(userPublicId);
        request.setCustomerPhone("0900000000");
        request.setOriginalAmount(new BigDecimal("100000"));
        request.setBookingPublicId(UUID.randomUUID().toString());
        request.setOrderPublicId(UUID.randomUUID().toString());
        request.setContextJson(JsonNodeFactory.instance.objectNode());
        request.setHoldDurationSeconds(900);
        return request;
    }
}
