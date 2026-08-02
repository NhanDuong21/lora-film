-- All legacy rows were copied by V6 and active reservation references were
-- converted by V7. The application no longer maps these tables.

DELETE FROM approval_histories
WHERE target_type = 'COMPENSATION';

DROP TABLE IF EXISTS compensation_vouchers;
DROP TABLE IF EXISTS coupon_redemptions;
DROP TABLE IF EXISTS voucher_redemptions;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS vouchers;
DROP TABLE IF EXISTS promotion_rules;
