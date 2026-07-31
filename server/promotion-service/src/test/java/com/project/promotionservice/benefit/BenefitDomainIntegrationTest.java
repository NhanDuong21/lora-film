package com.project.promotionservice.benefit;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.promotionservice.benefit.dto.request.CompensationRequests.CompensationIssueRequest;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponCreateRequest;
import com.project.promotionservice.benefit.dto.request.RedemptionRequests.BenefitRedeemRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherIssueRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CompensationResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CouponResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.VoucherResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationType;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponType;
import com.project.promotionservice.benefit.enums.BenefitEnums.DistributionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherType;
import com.project.promotionservice.benefit.repository.CouponRepository;
import com.project.promotionservice.benefit.repository.VoucherRepository;
import com.project.promotionservice.benefit.service.CompensationService;
import com.project.promotionservice.benefit.service.CouponService;
import com.project.promotionservice.benefit.service.RedemptionService;
import com.project.promotionservice.benefit.service.VoucherService;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignType;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RefreshRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RuntimeValidationRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import com.project.promotionservice.reservation.idempotency.ReservationIdempotencyExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BenefitDomainIntegrationTest {

    @Autowired
    private PromotionCampaignRepository campaignRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private RedemptionService redemptionService;

    @Autowired
    private CompensationService compensationService;

    @Autowired
    private PromotionReservationService reservationService;

    @Autowired
    private ReservationIdempotencyExecutor idempotencyExecutor;

    @Autowired
    private PromotionReservationRepository reservationRepository;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void exposesThirtyOneCanonicalBenefitAndReservationRuntimeApis() {
        long benefitMappings = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getPackageName()
                        .startsWith("com.project.promotionservice.benefit.controller"))
                .count();
        long reservationMappings = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getPackageName()
                        .startsWith("com.project.promotionservice.reservation.controller"))
                .count();

        assertThat(benefitMappings).isEqualTo(23);
        assertThat(reservationMappings).isEqualTo(8);
        assertThat(benefitMappings + reservationMappings).isEqualTo(31);
    }

    @Test
    void benefitResponsesAreCompatibleWithTheProductionRedisSerializer() {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer()
                        .configure(mapper -> mapper.findAndRegisterModules());
        CouponResponse response = new CouponResponse();
        response.setPublicId(UUID.randomUUID().toString());
        response.setCode("CACHE-CHECK");
        response.setCreatedAt(Instant.now());

        Object restored = serializer.deserialize(serializer.serialize(response));

        assertThat(restored).isInstanceOf(CouponResponse.class);
        assertThat(((CouponResponse) restored).getCode()).isEqualTo("CACHE-CHECK");
    }

    @Test
    void compensationIssuesLinkedVoucherAndApprovalHistory() {
        CompensationIssueRequest request = new CompensationIssueRequest();
        request.setUserPublicId("user-3");
        request.setBookingPublicId("booking-3");
        request.setCompensationType(CompensationType.BOOKING_FAILURE);
        request.setReason("Payment succeeded but booking failed");
        request.setAmount(new BigDecimal("75000"));
        request.setExpiredAt(Instant.now().plus(30, ChronoUnit.DAYS));

        CompensationResponse response = compensationService.issue(request, "admin");

        assertThat(response.getStatus()).isEqualTo(CompensationStatus.ISSUED);
        assertThat(response.getVoucher()).isNotNull();
        assertThat(response.getVoucher().getSource()).isEqualTo(VoucherSource.COMPENSATION);
        assertThat(response.getVoucher().getFaceValue()).isEqualByComparingTo("75000.00");
        assertThat(response.getApprovalHistory()).hasSize(1);
        assertThat(response.getApprovalHistory().getFirst().getAction()).isEqualTo("APPROVE");
    }

    @Test
    void reservationHoldsBudgetAndConfirmCreatesExactlyOneRedemption() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = createActiveCoupon(campaign);
        ReserveRequest reserveRequest = reservationRequest(
                RedemptionType.COUPON, coupon.getCode(), "reservation-user-1");

        ReservationResponse reserved = reservationService.reserve(
                reserveRequest, "reserve-order-1", "booking-service");
        ReservationResponse repeatedReserve = reservationService.reserve(
                reserveRequest, "reserve-order-1", "booking-service");

        assertThat(repeatedReserve.getPublicId()).isEqualTo(reserved.getPublicId());
        assertThat(reserved.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reserved.getDiscountAmount()).isEqualByComparingTo("20000.00");
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getBudgetReserved())
                .isEqualByComparingTo("20000.00");
        assertThat(couponRepository.findByPublicIdAndDeletedAtIsNull(coupon.getPublicId()).orElseThrow()
                .getRedemptionCount()).isZero();

        ConfirmRequest confirmRequest = new ConfirmRequest();
        confirmRequest.setPaymentPublicId(UUID.randomUUID().toString());
        ReservationResponse confirmed = reservationService.confirm(
                reserved.getPublicId(), confirmRequest, "confirm-order-1", "payment-service");
        ReservationResponse repeatedConfirm = reservationService.confirm(
                reserved.getPublicId(), confirmRequest, "confirm-order-1", "payment-service");

        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(confirmed.getRedemption()).isNotNull();
        assertThat(repeatedConfirm.getRedemption().getPublicId())
                .isEqualTo(confirmed.getRedemption().getPublicId());
        PromotionCampaign confirmedCampaign = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(confirmedCampaign.getBudgetReserved()).isEqualByComparingTo("0.00");
        assertThat(confirmedCampaign.getBudgetUsed()).isEqualByComparingTo("20000.00");
        assertThat(confirmedCampaign.getBudgetRemaining()).isEqualByComparingTo("980000.00");
        assertThat(couponRepository.findByPublicIdAndDeletedAtIsNull(coupon.getPublicId()).orElseThrow()
                .getRedemptionCount()).isEqualTo(1);

    }

    @Test
    void confirmConsumesTheReservedPricingSnapshotWhenCouponIsDisabled() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = createActiveCoupon(campaign);
        ReservationResponse reserved = reservationService.reserve(
                reservationRequest(RedemptionType.COUPON, coupon.getCode(), "snapshot-user"),
                "snapshot-reserve-key",
                "booking-service");

        couponService.disable(coupon.getPublicId(), "admin");

        ConfirmRequest confirmRequest = new ConfirmRequest();
        confirmRequest.setPaymentPublicId(UUID.randomUUID().toString());
        ReservationResponse confirmed = reservationService.confirm(
                reserved.getPublicId(), confirmRequest, "snapshot-confirm-key", "payment-service");

        assertThat(confirmed.getDiscountAmount()).isEqualByComparingTo("20000.00");
        assertThat(confirmed.getRedemption().getDiscountAmount()).isEqualByComparingTo("20000.00");
        PromotionCampaign confirmedCampaign =
                campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(confirmedCampaign.getBudgetReserved()).isEqualByComparingTo("0.00");
        assertThat(confirmedCampaign.getBudgetUsed()).isEqualByComparingTo("20000.00");
    }

    @Test
    void reservationReleaseIsIdempotentAndReleasesHeldBudget() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = createActiveCoupon(campaign);
        ReserveRequest reserveRequest = reservationRequest(
                RedemptionType.COUPON, coupon.getCode(), "reservation-user-2");
        ReservationResponse reserved = reservationService.reserve(
                reserveRequest, "reserve-order-2", "booking-service");

        TransitionRequest releaseRequest = new TransitionRequest();
        releaseRequest.setReason("Payment failed");
        ReservationResponse first = reservationService.release(
                reserved.getPublicId(), releaseRequest, "release-order-2", "payment-service");
        ReservationResponse repeated = reservationService.release(
                reserved.getPublicId(), releaseRequest, "release-order-2", "payment-service");

        assertThat(first.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(repeated.getPublicId()).isEqualTo(first.getPublicId());
        assertThat(repeated.getRollbackReason()).isEqualTo("Payment failed");
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getBudgetReserved())
                .isEqualByComparingTo("0.00");
        assertThat(reservationRepository.findByPublicIdAndDeletedAtIsNull(reserved.getPublicId())
                .orElseThrow().getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void multiUsePublicCouponAllowsIndependentActiveHolds() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = createActiveCoupon(campaign);
        ReserveRequest firstRequest = reservationRequest(
                RedemptionType.COUPON, coupon.getCode(), "public-coupon-user-1");
        firstRequest.setCustomerPhone("0900000101");
        ReserveRequest secondRequest = reservationRequest(
                RedemptionType.COUPON, coupon.getCode(), "public-coupon-user-2");
        secondRequest.setCustomerPhone("0900000102");

        ReservationResponse first = reservationService.reserve(
                firstRequest, "public-coupon-hold-1", "booking-service");
        ReservationResponse second = reservationService.reserve(
                secondRequest, "public-coupon-hold-2", "booking-service");

        assertThat(first.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(second.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getBudgetReserved())
                .isEqualByComparingTo("40000.00");
    }

    @Test
    void reservationCanBeCancelledAndAppearsInFilteredHistory() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = createActiveCoupon(campaign);
        ReservationResponse reserved = reservationService.reserve(
                reservationRequest(RedemptionType.COUPON, coupon.getCode(), "cancel-user"),
                "cancel-reserve-key",
                "booking-service");

        TransitionRequest cancelRequest = new TransitionRequest();
        cancelRequest.setReason("Customer cancelled booking");
        ReservationResponse cancelled = reservationService.cancel(
                reserved.getPublicId(), cancelRequest, "cancel-key", "booking-service");
        ReservationResponse replayed = reservationService.cancel(
                reserved.getPublicId(), cancelRequest, "cancel-key", "booking-service");

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(replayed.getPublicId()).isEqualTo(cancelled.getPublicId());
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getBudgetReserved())
                .isEqualByComparingTo("0.00");

        PagedResponse<ReservationResponse> history = reservationService.history(
                RedemptionType.COUPON, ReservationStatus.CANCELLED,
                "cancel-user", null, null, null, null, 0, 20);
        assertThat(history.getContent()).extracting(ReservationResponse::getPublicId)
                .contains(cancelled.getPublicId());
    }

    @Test
    void refreshUsesAbsoluteDeadlineAndRuntimeValidationSeesActiveHold() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = createActiveCoupon(campaign);
        ReserveRequest reserveRequest = reservationRequest(
                RedemptionType.COUPON, coupon.getCode(), "refresh-user");

        RuntimeValidationRequest validationRequest = new RuntimeValidationRequest();
        validationRequest.setBenefitType(RedemptionType.COUPON);
        validationRequest.setCode(reserveRequest.getCode());
        validationRequest.setUserPublicId(reserveRequest.getUserPublicId());
        validationRequest.setCustomerPhone(reserveRequest.getCustomerPhone());
        validationRequest.setOriginalAmount(reserveRequest.getOriginalAmount());
        validationRequest.setBookingPublicId(reserveRequest.getBookingPublicId());
        assertThat(reservationService.validateRuntime(validationRequest).isValid()).isTrue();

        ReservationResponse reserved = reservationService.reserve(
                reserveRequest, "refresh-reserve-key", "booking-service");
        assertThatThrownBy(() -> reservationService.validateRuntime(validationRequest))
                .hasMessageContaining("fully reserved");

        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRequestedExpiredAt(
                reserved.getReservationStartedAt().plus(20, ChronoUnit.MINUTES));
        ReservationResponse refreshed = reservationService.refresh(
                reserved.getPublicId(), refreshRequest, "refresh-key", "booking-service");
        ReservationResponse replayed = reservationService.refresh(
                reserved.getPublicId(), refreshRequest, "refresh-key", "booking-service");

        assertThat(refreshed.getReservationExpiredAt())
                .isEqualTo(refreshRequest.getRequestedExpiredAt());
        assertThat(replayed.getReservationExpiredAt())
                .isEqualTo(refreshed.getReservationExpiredAt());
        assertThat(reservationService.getDetail(reserved.getPublicId(), "booking-service")
                .getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void rejectsUnsafeBenefitConfigurationAndRequiresVerifiedPhoneForCoupons() {
        PromotionCampaign campaign = activeCampaign();
        CouponCreateRequest targeted = couponRequest(campaign.getPublicId());
        targeted.setDistributionType(DistributionType.TARGETED);

        assertThatThrownBy(() -> couponService.create(targeted, "admin"))
                .hasMessageContaining("allowedUserIds");

        CouponCreateRequest illegalPercentage = couponRequest(campaign.getPublicId());
        ObjectNode action = JsonNodeFactory.instance.objectNode();
        action.put("discountType", "PERCENTAGE");
        action.put("discountValue", 60);
        illegalPercentage.setActionsJson(action);

        assertThatThrownBy(() -> couponService.create(illegalPercentage, "admin"))
                .hasMessageContaining("50%");

        CouponResponse coupon = createActiveCoupon(campaign);
        BenefitRedeemRequest validation = redemptionRequest(coupon.getCode(), "phone-required-user");
        validation.setCustomerPhone(null);

        assertThatThrownBy(() -> redemptionService.validateCoupon(validation))
                .hasMessageContaining("customerPhone");
    }

    @Test
    void voucherActionIsCanonicalWhenFaceValueIsAlsoPresent() {
        VoucherIssueRequest request = voucherRequest("percentage-user");
        request.setVoucherType(VoucherType.PERCENTAGE);
        request.setFaceValue(new BigDecimal("50000"));
        ObjectNode action = JsonNodeFactory.instance.objectNode();
        action.put("discountType", "PERCENTAGE");
        action.put("discountValue", 10);
        request.setActionsJson(action);
        VoucherResponse voucher = voucherService.issue(request, "admin");

        BenefitRedeemRequest validation = redemptionRequest(voucher.getCode(), "percentage-user");

        assertThat(redemptionService.validateVoucher(validation).getDiscountAmount())
                .isEqualByComparingTo("10000.00");
    }

    @Test
    void voucherReservationConfirmIsIdempotentAndConsumesVoucherOnce() {
        VoucherResponse voucher = voucherService.issue(voucherRequest("reserved-voucher-user"), "admin");
        ReserveRequest request = reservationRequest(
                RedemptionType.VOUCHER, voucher.getCode(), "reserved-voucher-user");
        ReservationResponse reserved = reservationService.reserve(
                request, "voucher-reserve-key", "booking-service");

        ConfirmRequest confirmRequest = new ConfirmRequest();
        confirmRequest.setPaymentPublicId(UUID.randomUUID().toString());
        ReservationResponse first = reservationService.confirm(
                reserved.getPublicId(), confirmRequest, "voucher-confirm-key", "payment-service");
        ReservationResponse repeated = reservationService.confirm(
                reserved.getPublicId(), confirmRequest, "voucher-confirm-key", "payment-service");

        assertThat(first.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(first.getRedemption().getCode()).isEqualTo(voucher.getCode());
        assertThat(repeated.getRedemption().getPublicId())
                .isEqualTo(first.getRedemption().getPublicId());
        assertThat(voucherRepository.findByPublicIdAndDeletedAtIsNull(voucher.getPublicId())
                .orElseThrow().getUsageCount()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void expirationSelfHealingReleasesStaleReservation() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = createActiveCoupon(campaign);
        ReservationResponse reserved = reservationService.reserve(
                reservationRequest(RedemptionType.COUPON, coupon.getCode(), "expired-user"),
                "expired-reservation-key",
                "booking-service");

        var entity = reservationRepository.findByPublicIdAndDeletedAtIsNull(reserved.getPublicId())
                .orElseThrow();
        entity.setReservationStartedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        entity.setReservationExpiredAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        reservationRepository.saveAndFlush(entity);

        assertThat(reservationService.expireDueReservations("scheduler")).isEqualTo(1);
        assertThat(reservationRepository.findByPublicIdAndDeletedAtIsNull(reserved.getPublicId())
                .orElseThrow().getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getBudgetReserved())
                .isEqualByComparingTo("0.00");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void durableIdempotencyReplaysWithoutExecutingReservationTwice() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = createActiveCoupon(campaign);
        ReserveRequest firstRequest = reservationRequest(
                RedemptionType.COUPON, coupon.getCode(), "idempotent-user");
        String bookingId = firstRequest.getBookingPublicId();
        String orderId = firstRequest.getOrderPublicId();
        AtomicInteger executions = new AtomicInteger();

        ReservationResponse first = idempotencyExecutor.execute(
                "BOOKING_SERVICE",
                "POST /internal/reservations",
                "durable-idempotency-" + UUID.randomUUID(),
                null,
                firstRequest,
                201,
                () -> {
                    executions.incrementAndGet();
                    return reservationService.reserve(
                            firstRequest, "domain-reserve-" + orderId, "BOOKING_SERVICE");
                });

        ReserveRequest retryRequest = reservationRequest(
                RedemptionType.COUPON, coupon.getCode(), "idempotent-user");
        retryRequest.setBookingPublicId(bookingId);
        retryRequest.setOrderPublicId(orderId);
        String replayKey = "durable-replay-" + first.getPublicId();
        // Bind and replay a second durable key around an already idempotent domain command.
        ReservationResponse bound = idempotencyExecutor.execute(
                "BOOKING_SERVICE",
                "POST /internal/reservations",
                replayKey,
                null,
                retryRequest,
                201,
                () -> {
                    executions.incrementAndGet();
                    return reservationService.reserve(
                            retryRequest, "domain-reserve-" + orderId, "BOOKING_SERVICE");
                });
        ReserveRequest replayRequest = reservationRequest(
                RedemptionType.COUPON, coupon.getCode(), "idempotent-user");
        replayRequest.setBookingPublicId(bookingId);
        replayRequest.setOrderPublicId(orderId);
        ReservationResponse replay = idempotencyExecutor.execute(
                "BOOKING_SERVICE",
                "POST /internal/reservations",
                replayKey,
                null,
                replayRequest,
                201,
                () -> {
                    executions.incrementAndGet();
                    throw new AssertionError("Durable replay must not execute the domain command");
                });

        assertThat(first.getPublicId()).isEqualTo(bound.getPublicId());
        assertThat(replay.getPublicId()).isEqualTo(bound.getPublicId());
        assertThat(executions).hasValue(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void checkoutCannotHoldTwoBenefitsAtTheSameTime() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse firstCoupon = createActiveCoupon(campaign);
        CouponResponse secondCoupon = createActiveCoupon(campaign);
        ReserveRequest firstRequest = reservationRequest(
                RedemptionType.COUPON, firstCoupon.getCode(), "checkout-user-1");
        ReserveRequest secondRequest = reservationRequest(
                RedemptionType.COUPON, secondCoupon.getCode(), "checkout-user-2");
        secondRequest.setBookingPublicId(firstRequest.getBookingPublicId());
        secondRequest.setOrderPublicId(firstRequest.getOrderPublicId());
        secondRequest.setCustomerPhone("+84900000002");

        reservationService.reserve(
                firstRequest, "checkout-first-" + UUID.randomUUID(), "BOOKING_SERVICE");

        assertThatThrownBy(() -> reservationService.reserve(
                secondRequest, "checkout-second-" + UUID.randomUUID(), "BOOKING_SERVICE"))
                .hasMessageContaining("checkout already has");
    }

    private PromotionCampaign activeCampaign() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setCode("CAMP-" + suffix);
        campaign.setName("Benefit Test " + suffix);
        campaign.setSlug("benefit-test-" + suffix);
        campaign.setCampaignType(CampaignType.COUPON);
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

    private CouponResponse createActiveCoupon(PromotionCampaign campaign) {
        PromotionCampaign current = campaignRepository.findById(campaign.getId()).orElseThrow();
        current.setStatus(CampaignStatus.DRAFT);
        current.setApprovalStatus(CampaignApprovalStatus.DRAFT);
        current.setLegalStatus(LegalStatus.PENDING);
        campaignRepository.saveAndFlush(current);

        CouponResponse response = couponService.create(
                couponRequest(current.getPublicId()), "admin");
        couponRepository.activateDraftCoupons(
                current.getPublicId(), CouponStatus.DRAFT, CouponStatus.ACTIVE,
                Instant.now(), "admin");

        current = campaignRepository.findById(campaign.getId()).orElseThrow();
        current.setStatus(CampaignStatus.ACTIVE);
        current.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        current.setLegalStatus(LegalStatus.PASSED);
        campaignRepository.saveAndFlush(current);
        response.setStatus(CouponStatus.ACTIVE);
        return response;
    }

    private CouponCreateRequest couponRequest(String campaignPublicId) {
        CouponCreateRequest request = new CouponCreateRequest();
        request.setCampaignPublicId(campaignPublicId);
        request.setCode("CPN-" + UUID.randomUUID().toString().substring(0, 8));
        request.setName("20K off");
        request.setCouponType(CouponType.PUBLIC);
        request.setStatus(CouponStatus.DRAFT);
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

    private VoucherIssueRequest voucherRequest(String ownerPublicId) {
        VoucherIssueRequest request = new VoucherIssueRequest();
        request.setOwnerPublicId(ownerPublicId);
        request.setName("50K voucher");
        request.setVoucherType(VoucherType.FIXED_AMOUNT);
        request.setSource(VoucherSource.MANUAL);
        request.setValidFrom(Instant.now().minus(1, ChronoUnit.HOURS));
        request.setValidTo(Instant.now().plus(10, ChronoUnit.DAYS));
        request.setMaxUsage(1);
        request.setFaceValue(new BigDecimal("50000"));
        request.setMinimumOrderAmount(BigDecimal.ZERO);
        request.setConditionsJson(JsonNodeFactory.instance.objectNode());
        ObjectNode action = JsonNodeFactory.instance.objectNode();
        action.put("discountType", "FIXED_AMOUNT");
        action.put("discountValue", 50_000);
        request.setActionsJson(action);
        return request;
    }

    private BenefitRedeemRequest redemptionRequest(String code, String userPublicId) {
        BenefitRedeemRequest request = new BenefitRedeemRequest();
        request.setCode(code);
        request.setUserPublicId(userPublicId);
        request.setCustomerPhone("+84900000000");
        request.setOriginalAmount(new BigDecimal("100000"));
        request.setBookingPublicId(UUID.randomUUID().toString());
        request.setOrderPublicId(UUID.randomUUID().toString());
        request.setPaymentPublicId(UUID.randomUUID().toString());
        request.setContextJson(JsonNodeFactory.instance.objectNode());
        return request;
    }

    private ReserveRequest reservationRequest(
            RedemptionType benefitType, String code, String userPublicId) {
        ReserveRequest request = new ReserveRequest();
        request.setBenefitType(benefitType);
        request.setCode(code);
        request.setUserPublicId(userPublicId);
        request.setCustomerPhone("0900000099");
        request.setOriginalAmount(new BigDecimal("100000"));
        request.setBookingPublicId(UUID.randomUUID().toString());
        request.setOrderPublicId(UUID.randomUUID().toString());
        request.setContextJson(JsonNodeFactory.instance.objectNode());
        request.setHoldDurationSeconds(900);
        return request;
    }
}
