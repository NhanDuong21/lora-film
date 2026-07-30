package com.project.promotionservice.benefit.specification;

import com.project.promotionservice.benefit.entity.CompensationVoucher;
import com.project.promotionservice.benefit.entity.Coupon;
import com.project.promotionservice.benefit.entity.CouponRedemption;
import com.project.promotionservice.benefit.entity.Voucher;
import com.project.promotionservice.benefit.entity.VoucherRedemption;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationType;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class BenefitSpecifications {

    private BenefitSpecifications() {
    }

    public static Specification<Coupon> coupons(
            String keyword, String campaignPublicId, CouponStatus status, Instant validAt) {
        return (root, query, builder) -> {
            var predicate = builder.isNull(root.get("deletedAt"));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("code")), pattern),
                        builder.like(builder.lower(root.get("name")), pattern)));
            }
            if (campaignPublicId != null && !campaignPublicId.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("campaignPublicId"), campaignPublicId));
            }
            if (status != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            }
            if (validAt != null) {
                predicate = builder.and(predicate,
                        builder.lessThanOrEqualTo(root.get("validFrom"), validAt),
                        builder.greaterThan(root.get("validTo"), validAt));
            }
            return predicate;
        };
    }

    public static Specification<Voucher> vouchers(
            String keyword, String ownerPublicId, String campaignPublicId,
            VoucherStatus status, VoucherSource source) {
        return (root, query, builder) -> {
            var predicate = builder.isNull(root.get("deletedAt"));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("code")), pattern),
                        builder.like(builder.lower(root.get("name")), pattern)));
            }
            if (ownerPublicId != null && !ownerPublicId.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("ownerPublicId"), ownerPublicId));
            }
            if (campaignPublicId != null && !campaignPublicId.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("campaignPublicId"), campaignPublicId));
            }
            if (status != null) {
                if (status == VoucherStatus.EXPIRED) {
                    predicate = builder.and(predicate, builder.or(
                            builder.equal(root.get("status"), VoucherStatus.EXPIRED),
                            builder.and(
                                    builder.lessThanOrEqualTo(root.get("validTo"), Instant.now()),
                                    root.get("status").in(
                                            VoucherStatus.ACTIVE,
                                            VoucherStatus.ISSUED,
                                            VoucherStatus.LOCKED))));
                } else if (status == VoucherStatus.ACTIVE) {
                    Instant now = Instant.now();
                    predicate = builder.and(predicate,
                            root.get("status").in(VoucherStatus.ACTIVE, VoucherStatus.ISSUED),
                            builder.lessThanOrEqualTo(root.get("validFrom"), now),
                            builder.greaterThan(root.get("validTo"), now));
                } else {
                    predicate = builder.and(predicate, builder.equal(root.get("status"), status));
                }
            }
            if (source != null) {
                predicate = builder.and(predicate, builder.equal(root.get("source"), source));
            }
            return predicate;
        };
    }

    public static Specification<CouponRedemption> couponRedemptions(
            String userPublicId, String bookingPublicId, RedemptionStatus status,
            Instant from, Instant to) {
        return (root, query, builder) -> {
            var predicate = builder.isNull(root.get("deletedAt"));
            if (userPublicId != null && !userPublicId.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("userPublicId"), userPublicId));
            }
            if (bookingPublicId != null && !bookingPublicId.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("bookingPublicId"), bookingPublicId));
            }
            if (status != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicate = builder.and(predicate, builder.lessThan(root.get("createdAt"), to));
            }
            return predicate;
        };
    }

    public static Specification<VoucherRedemption> voucherRedemptions(
            String userPublicId, String bookingPublicId, RedemptionStatus status,
            Instant from, Instant to) {
        return (root, query, builder) -> {
            var predicate = builder.isNull(root.get("deletedAt"));
            if (userPublicId != null && !userPublicId.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("redeemedBy"), userPublicId));
            }
            if (bookingPublicId != null && !bookingPublicId.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("bookingPublicId"), bookingPublicId));
            }
            if (status != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicate = builder.and(predicate, builder.lessThan(root.get("createdAt"), to));
            }
            return predicate;
        };
    }

    public static Specification<CompensationVoucher> compensations(
            String userPublicId, CompensationType type, CompensationStatus status,
            Instant from, Instant to) {
        return (root, query, builder) -> {
            var predicate = builder.isNull(root.get("deletedAt"));
            if (userPublicId != null && !userPublicId.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("userPublicId"), userPublicId));
            }
            if (type != null) {
                predicate = builder.and(predicate, builder.equal(root.get("compensationType"), type));
            }
            if (status != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("issuedAt"), from));
            }
            if (to != null) {
                predicate = builder.and(predicate, builder.lessThan(root.get("issuedAt"), to));
            }
            return predicate;
        };
    }
}
