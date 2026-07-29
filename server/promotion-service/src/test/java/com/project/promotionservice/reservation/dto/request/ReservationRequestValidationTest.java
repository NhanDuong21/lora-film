package com.project.promotionservice.reservation.dto.request;

import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.RuntimeValidationRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType.COUPON;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationRequestValidationTest {

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
    void runtimeValidationAcceptsUuidIdentifiers() {
        RuntimeValidationRequest request = validRuntimeRequest();
        request.setBookingPublicId(UUID.randomUUID().toString());
        request.setOrderPublicId(UUID.randomUUID().toString());

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void runtimeValidationRejectsMalformedPublicIdentifiers() {
        RuntimeValidationRequest request = validRuntimeRequest();
        request.setUserPublicId("user-123");
        request.setBookingPublicId("booking-123");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void confirmRejectsMalformedPaymentIdentifier() {
        ConfirmRequest request = new ConfirmRequest();
        request.setPaymentPublicId("payment-123");

        assertFalse(validator.validate(request).isEmpty());
    }

    private RuntimeValidationRequest validRuntimeRequest() {
        RuntimeValidationRequest request = new RuntimeValidationRequest();
        request.setCode("SUMMER2026");
        request.setUserPublicId(UUID.randomUUID().toString());
        request.setOriginalAmount(new BigDecimal("100000.00"));
        request.setBenefitType(COUPON);
        return request;
    }
}
