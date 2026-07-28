package com.project.promotionservice.benefit.enums;

public final class BenefitEnums {

    private BenefitEnums() {
    }

    public enum CouponType {
        PUBLIC,
        PRIVATE,
        SYSTEM,
        PARTNER,
        COMPENSATION,
        SINGLE_USE
    }

    public enum CouponStatus {
        DRAFT,
        ACTIVE,
        DISABLED,
        USED,
        LOCKED,
        EXPIRED,
        CANCELLED
    }

    public enum DistributionType {
        PUBLIC,
        PRIVATE,
        TARGETED,
        AUTO
    }

    public enum VoucherType {
        FIXED_AMOUNT,
        PERCENTAGE,
        FREE_TICKET,
        FREE_COMBO,
        CASHBACK,
        REWARD,
        MEMBERSHIP,
        COMPENSATION,
        PARTNER
    }

    public enum VoucherSource {
        CAMPAIGN,
        MANUAL,
        BIRTHDAY,
        TIER_UPGRADE,
        POINT_REDEEM,
        PARTNER,
        COMPENSATION,
        SYSTEM
    }

    public enum VoucherStatus {
        ISSUED,
        ACTIVE,
        USED,
        REVOKED,
        EXPIRED,
        CANCELLED,
        LOCKED
    }

    public enum RedemptionStatus {
        SUCCESS,
        CONFIRMED,
        ROLLED_BACK,
        REFUNDED,
        CANCELLED
    }

    public enum RedemptionType {
        COUPON,
        VOUCHER
    }

    public enum CompensationType {
        PAYMENT_FAILURE,
        BOOKING_FAILURE,
        SHOW_CANCELLED,
        SYSTEM_ERROR,
        CUSTOMER_SERVICE,
        MANUAL
    }

    public enum CompensationStatus {
        ISSUED,
        CANCELLED
    }
}
