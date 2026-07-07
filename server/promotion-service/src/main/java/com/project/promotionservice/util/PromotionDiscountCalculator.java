package com.project.promotionservice.util;

import com.project.promotionservice.enums.DiscountType;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PromotionDiscountCalculator {

    public static class CalculationResult {
        private final BigDecimal discountAmount;
        private final BigDecimal finalAmount;

        public CalculationResult(BigDecimal discountAmount, BigDecimal finalAmount) {
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public BigDecimal getFinalAmount() {
            return finalAmount;
        }
    }

    public static CalculationResult calculate(BigDecimal bookingAmount, DiscountType discountType, BigDecimal discountValue, BigDecimal maxDiscountAmount) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        
        if (discountType == DiscountType.PERCENTAGE) {
            BigDecimal rawDiscount = bookingAmount.multiply(discountValue).divide(BigDecimal.valueOf(100));
            discountAmount = rawDiscount.setScale(0, RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && discountAmount.compareTo(maxDiscountAmount) > 0) {
                discountAmount = maxDiscountAmount.setScale(0, RoundingMode.HALF_UP);
            }
        } else if (discountType == DiscountType.FIXED_AMOUNT) {
            discountAmount = discountValue.setScale(0, RoundingMode.HALF_UP);
            if (discountAmount.compareTo(bookingAmount) > 0) {
                discountAmount = bookingAmount.setScale(0, RoundingMode.HALF_UP);
            }
        }
        
        BigDecimal finalAmount = bookingAmount.subtract(discountAmount).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
        
        return new CalculationResult(discountAmount, finalAmount);
    }
}
