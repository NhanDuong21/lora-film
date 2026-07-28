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
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignType;
import com.project.promotionservice.promotion.enums.FundingSource;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ReserveRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RollbackRequest;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

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
    private PromotionReservationRepository reservationRepository;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void exposesTwentySixCanonicalBenefitAndCheckoutApis() {
        long benefitMappings = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getPackageName()
                        .startsWith("com.project.promotionservice.benefit.controller"))
                .count();
        long reservationMappings = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getPackageName()
                        .startsWith("com.project.promotionservice.reservation.controller"))
                .count();

        assertThat(benefitMappings).isEqualTo(23);
        assertThat(reservationMappings).isEqualTo(3);
        assertThat(benefitMappings + reservationMappings).isEqualTo(26);
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
        CouponResponse coupon = couponService.create(couponRequest(campaign.getPublicId()), "admin");
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
        confirmRequest.setReservationPublicId(reserved.getPublicId());
        confirmRequest.setPaymentPublicId(UUID.randomUUID().toString());
        ReservationResponse confirmed = reservationService.confirm(
                confirmRequest, "confirm-order-1", "payment-service");
        ReservationResponse repeatedConfirm = reservationService.confirm(
                confirmRequest, "confirm-order-1", "payment-service");

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
    void reservationRollbackIsIdempotentAndReleasesHeldBudget() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = couponService.create(couponRequest(campaign.getPublicId()), "admin");
        ReserveRequest reserveRequest = reservationRequest(
                RedemptionType.COUPON, coupon.getCode(), "reservation-user-2");
        ReservationResponse reserved = reservationService.reserve(
                reserveRequest, "reserve-order-2", "booking-service");

        RollbackRequest rollbackRequest = new RollbackRequest();
        rollbackRequest.setReservationPublicId(reserved.getPublicId());
        rollbackRequest.setReason("Payment failed");
        ReservationResponse first = reservationService.rollback(
                rollbackRequest, "rollback-order-2", "payment-service");
        ReservationResponse repeated = reservationService.rollback(
                rollbackRequest, "rollback-order-2", "payment-service");

        assertThat(first.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(repeated.getPublicId()).isEqualTo(first.getPublicId());
        assertThat(repeated.getRollbackReason()).isEqualTo("Payment failed");
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getBudgetReserved())
                .isEqualByComparingTo("0.00");
        assertThat(reservationRepository.findByPublicIdAndDeletedAtIsNull(reserved.getPublicId())
                .orElseThrow().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
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

        CouponResponse coupon = couponService.create(couponRequest(campaign.getPublicId()), "admin");
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
        confirmRequest.setReservationPublicId(reserved.getPublicId());
        confirmRequest.setPaymentPublicId(UUID.randomUUID().toString());
        ReservationResponse first = reservationService.confirm(
                confirmRequest, "voucher-confirm-key", "payment-service");
        ReservationResponse repeated = reservationService.confirm(
                confirmRequest, "voucher-confirm-key", "payment-service");

        assertThat(first.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(first.getRedemption().getCode()).isEqualTo(voucher.getCode());
        assertThat(repeated.getRedemption().getPublicId())
                .isEqualTo(first.getRedemption().getPublicId());
        assertThat(voucherRepository.findByPublicIdAndDeletedAtIsNull(voucher.getPublicId())
                .orElseThrow().getUsageCount()).isEqualTo(1);
    }

    @Test
    void expirationSelfHealingReleasesStaleReservation() {
        PromotionCampaign campaign = activeCampaign();
        CouponResponse coupon = couponService.create(couponRequest(campaign.getPublicId()), "admin");
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

    private PromotionCampaign activeCampaign() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setCode("CAMP-" + suffix);
        campaign.setName("Benefit Test " + suffix);
        campaign.setSlug("benefit-test-" + suffix);
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
        request.setCode("CPN-" + UUID.randomUUID().toString().substring(0, 8));
        request.setName("20K off");
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
        request.setCustomerPhone("0900000000-" + userPublicId.charAt(userPublicId.length() - 1));
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
