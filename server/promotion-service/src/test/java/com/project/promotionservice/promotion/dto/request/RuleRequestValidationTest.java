package com.project.promotionservice.promotion.dto.request;

import com.project.promotionservice.promotion.enums.RuleType;
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

class RuleRequestValidationTest {

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
        RuleCreateRequest request = validCreateRequest();
        request.setEnabled(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void updateRequestRejectsNullRequiredFlags() {
        RuleUpdateRequest request = validUpdateRequest();
        request.setExecutionOrder(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void createRequestAcceptsCompleteProductionShape() {
        assertTrue(validator.validate(validCreateRequest()).isEmpty());
    }

    private RuleCreateRequest validCreateRequest() {
        RuleCreateRequest request = new RuleCreateRequest();
        request.setCampaignPublicId(UUID.randomUUID().toString());
        request.setCode("RULE2026");
        request.setName("Production Rule");
        request.setRuleType(RuleType.DISCOUNT_TICKET);
        request.setPriority(100);
        request.setExecutionOrder(1);
        request.setStackable(false);
        request.setStopFurtherRules(false);
        request.setEnabled(true);
        request.setConditionsJson("{}");
        request.setActionsJson("{\"type\":\"PERCENTAGE_DISCOUNT\",\"value\":10}");
        request.setEffectiveFrom(Instant.now());
        return request;
    }

    private RuleUpdateRequest validUpdateRequest() {
        RuleUpdateRequest request = new RuleUpdateRequest();
        request.setName("Production Rule Updated");
        request.setPriority(100);
        request.setExecutionOrder(1);
        request.setStackable(false);
        request.setStopFurtherRules(false);
        request.setEnabled(true);
        request.setConditionsJson("{}");
        request.setActionsJson("{\"type\":\"PERCENTAGE_DISCOUNT\",\"value\":10}");
        request.setEffectiveFrom(Instant.now());
        return request;
    }
}
