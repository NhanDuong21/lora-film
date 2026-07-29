package com.project.promotionservice.benefit.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponCreateRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherIssueRequest;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponType;
import com.project.promotionservice.benefit.enums.BenefitEnums.DistributionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenefitRequestValidationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void couponCreateRejectsMalformedCampaignId() {
        CouponCreateRequest request = validCouponCreateRequest();
        request.setCampaignPublicId("campaign-123");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void couponCreateRejectsNullRequiredFlags() {
        CouponCreateRequest request = validCouponCreateRequest();
        request.setAutoApply(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void voucherIssueRejectsMalformedOwnerId() {
        VoucherIssueRequest request = validVoucherIssueRequest();
        request.setOwnerPublicId("owner-123");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void voucherIssueAcceptsCompleteProductionShape() {
        assertTrue(validator.validate(validVoucherIssueRequest()).isEmpty());
    }

    private CouponCreateRequest validCouponCreateRequest() {
        CouponCreateRequest request = new CouponCreateRequest();
        request.setCampaignPublicId(UUID.randomUUID().toString());
        request.setCode("COUPON2026");
        request.setName("Production Coupon");
        request.setCouponType(CouponType.PUBLIC);
        request.setDistributionType(DistributionType.PUBLIC);
        request.setStackable(false);
        request.setTransferable(false);
        request.setReusable(false);
        request.setAutoApply(false);
        request.setPriority(100);
        request.setMaxRedemptionsPerUser(1);
        request.setValidFrom(Instant.now());
        request.setValidTo(Instant.now().plusSeconds(3600));
        request.setConditionsJson(objectNode());
        request.setActionsJson(objectNode());
        return request;
    }

    private VoucherIssueRequest validVoucherIssueRequest() {
        VoucherIssueRequest request = new VoucherIssueRequest();
        request.setCampaignPublicId(UUID.randomUUID().toString());
        request.setOwnerPublicId(UUID.randomUUID().toString());
        request.setCode("VOUCHER2026");
        request.setName("Production Voucher");
        request.setVoucherType(VoucherType.FIXED_AMOUNT);
        request.setSource(VoucherSource.MANUAL);
        request.setValidFrom(Instant.now());
        request.setValidTo(Instant.now().plusSeconds(3600));
        request.setTransferable(false);
        request.setStackable(false);
        request.setReusable(false);
        request.setMaxUsage(1);
        request.setConditionsJson(objectNode());
        request.setActionsJson(objectNode());
        return request;
    }

    private JsonNode objectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }
}
