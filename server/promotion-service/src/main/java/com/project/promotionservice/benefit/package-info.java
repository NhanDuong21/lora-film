/**
 * Owns coupons, customer vouchers, coupon/voucher redemptions and compensation vouchers.
 *
 * <p>The persistence model follows {@code promotion-service-schema.sql}. Campaign data is
 * referenced by public ID only; booking, order, payment and user identifiers remain logical
 * cross-service references and are deliberately not mapped as database foreign keys.</p>
 */
package com.project.promotionservice.benefit;
