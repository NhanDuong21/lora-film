package com.project.promotionservice.promotion.dto.request;

import com.project.promotionservice.promotion.enums.CampaignType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignRequestValidationTest {

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
    void createRequestRejectsNullRequiredFlags() {
        CampaignCreateRequest request = validCreateRequest();
        request.setAutoPauseWhenBudgetExceeded(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void updateRequestRejectsNullRequiredFlags() {
        CampaignUpdateRequest request = validUpdateRequest();
        request.setStackable(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void updateRequestAcceptsCompleteProductionShape() {
        assertTrue(validator.validate(validUpdateRequest()).isEmpty());
    }

    private CampaignCreateRequest validCreateRequest() {
        CampaignCreateRequest request = new CampaignCreateRequest();
        request.setCode("PROD2026");
        request.setName("Production Campaign");
        request.setCampaignType(CampaignType.COUPON);
        request.setPriority(100);
        request.setStackable(false);
        request.setExclusiveCampaign(false);
        request.setAutoActivate(true);
        request.setAutoComplete(true);
        request.setAutoPauseWhenBudgetExceeded(true);
        request.setTimezone("Asia/Ho_Chi_Minh");
        request.setStartAt(Instant.now());
        request.setEndAt(Instant.now().plusSeconds(3600));
        request.setBudgetAmount(new BigDecimal("100000.00"));
        request.setMaxRedemptionsPerUser(1);
        return request;
    }

    private CampaignUpdateRequest validUpdateRequest() {
        CampaignUpdateRequest request = new CampaignUpdateRequest();
        request.setName("Production Campaign Updated");
        request.setPriority(100);
        request.setStackable(false);
        request.setExclusiveCampaign(false);
        request.setAutoActivate(true);
        request.setAutoComplete(true);
        request.setAutoPauseWhenBudgetExceeded(true);
        request.setTimezone("Asia/Ho_Chi_Minh");
        request.setStartAt(Instant.now());
        request.setEndAt(Instant.now().plusSeconds(3600));
        request.setBudgetAmount(new BigDecimal("100000.00"));
        request.setMaxRedemptionsPerUser(1);
        return request;
    }
}
