-- Partner-funded promotions and financial settlements are outside the service scope.

DELIMITER $$

DROP PROCEDURE IF EXISTS remove_partner_promotion_features_20260731$$
CREATE PROCEDURE remove_partner_promotion_features_20260731()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'coupons' AND column_name = 'partner_public_id') THEN
        ALTER TABLE coupons DROP COLUMN partner_public_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'vouchers' AND column_name = 'partner_public_id') THEN
        ALTER TABLE vouchers DROP COLUMN partner_public_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'partner_public_id') THEN
        ALTER TABLE promotion_campaigns DROP COLUMN partner_public_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'funding_source') THEN
        ALTER TABLE promotion_campaigns DROP COLUMN funding_source;
    END IF;

    DROP TABLE IF EXISTS partner_settlements;
    DROP TABLE IF EXISTS partners;
END$$

CALL remove_partner_promotion_features_20260731()$$
DROP PROCEDURE remove_partner_promotion_features_20260731$$

DELIMITER ;
