package com.project.promotionservice.benefit.exception;

public final class BenefitErrorCode {

    public static final String COUPON_NOT_FOUND = "COUPON_NOT_FOUND";
    public static final String COUPON_DUPLICATE = "COUPON_DUPLICATE";
    public static final String COUPON_INACTIVE = "COUPON_INACTIVE";
    public static final String COUPON_EXPIRED = "COUPON_EXPIRED";
    public static final String COUPON_EXHAUSTED = "COUPON_EXHAUSTED";
    public static final String COUPON_USER_LIMIT_REACHED = "COUPON_USER_LIMIT_REACHED";
    public static final String VOUCHER_NOT_FOUND = "VOUCHER_NOT_FOUND";
    public static final String VOUCHER_DUPLICATE = "VOUCHER_DUPLICATE";
    public static final String VOUCHER_INVALID = "VOUCHER_INVALID";
    public static final String VOUCHER_EXPIRED = "VOUCHER_EXPIRED";
    public static final String VOUCHER_ALREADY_USED = "VOUCHER_ALREADY_USED";
    public static final String VOUCHER_OWNER_MISMATCH = "VOUCHER_OWNER_MISMATCH";
    public static final String CAMPAIGN_NOT_ACTIVE = "CAMPAIGN_NOT_ACTIVE";
    public static final String BENEFIT_CONDITION_NOT_MET = "BENEFIT_CONDITION_NOT_MET";
    public static final String BENEFIT_CONFIGURATION_INVALID = "BENEFIT_CONFIGURATION_INVALID";
    public static final String REDEMPTION_NOT_FOUND = "REDEMPTION_NOT_FOUND";
    public static final String REDEMPTION_ALREADY_ROLLED_BACK = "REDEMPTION_ALREADY_ROLLED_BACK";
    public static final String COMPENSATION_NOT_FOUND = "COMPENSATION_NOT_FOUND";
    public static final String COMPENSATION_IMMUTABLE = "COMPENSATION_IMMUTABLE";
    public static final String IMPORT_INVALID = "IMPORT_INVALID";

    private BenefitErrorCode() {
    }
}
