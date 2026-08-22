package com.project.promotionservice.promotion.enums;

/**
 * Operational delivery contract. This is deliberately independent from the
 * discount template type: a coupon-shaped benefit can still be owned by an
 * automation and assigned directly to a wallet.
 */
public enum PromotionDistributionMode {
    AUTO_APPLY,
    CLAIMABLE_WALLET,
    ASSIGNED_WALLET,
    PERSONAL_CODE,
    AUTOMATION_ONLY
}
