package com.project.promotionservice.dto;

import com.project.promotionservice.enums.DiscountType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class DiscountConstraintValidator implements ConstraintValidator<DiscountConstraint, Object> {

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) return true;

        DiscountType discountType = null;
        BigDecimal discountValue = null;
        BigDecimal maxDiscountAmount = null;

        if (obj instanceof CreatePromotionRequest) {
            CreatePromotionRequest req = (CreatePromotionRequest) obj;
            discountType = req.getDiscountType();
            discountValue = req.getDiscountValue();
            maxDiscountAmount = req.getMaxDiscountAmount();
        } else if (obj instanceof UpdatePromotionRequest) {
            UpdatePromotionRequest req = (UpdatePromotionRequest) obj;
            discountType = req.getDiscountType();
            discountValue = req.getDiscountValue();
            maxDiscountAmount = req.getMaxDiscountAmount();
        }

        if (discountType == null) return true;

        if (discountType == DiscountType.PERCENTAGE) {
            if (maxDiscountAmount == null || maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("maxDiscountAmount is required and must be greater than 0 for PERCENTAGE discount type")
                       .addPropertyNode("maxDiscountAmount")
                       .addConstraintViolation();
                return false;
            }
        } else if (discountType == DiscountType.FIXED_AMOUNT) {
            if (maxDiscountAmount != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("maxDiscountAmount must be null for FIXED_AMOUNT discount type")
                       .addPropertyNode("maxDiscountAmount")
                       .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
