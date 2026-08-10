USE booking_db;

ALTER TABLE bookings
    MODIFY COLUMN voucher_discount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Unified Promotion Engine discount (AUTO, VOUCHER and COUPON)',
    ADD COLUMN promotion_reservation_public_id VARCHAR(36) NULL
        COMMENT 'Active or confirmed reservation owned by Promotion Service'
        AFTER voucher_discount,
    ADD COLUMN promotion_selection_fingerprint VARCHAR(64) NULL
        COMMENT 'Immutable checkout promotion selection after amount lock'
        AFTER promotion_reservation_public_id,
    ADD COLUMN applied_promotions_json JSON NULL
        COMMENT 'Booking snapshot of promotions selected by the engine'
        AFTER promotion_selection_fingerprint,
    ADD CONSTRAINT uk_booking_promotion_reservation
        UNIQUE (promotion_reservation_public_id);
