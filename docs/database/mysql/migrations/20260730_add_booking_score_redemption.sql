USE booking_db;

ALTER TABLE bookings
    ADD COLUMN score_points_used INT NOT NULL DEFAULT 0
        COMMENT 'Points held/redeemed for this Booking'
        AFTER voucher_discount,
    ADD COLUMN score_discount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Discount funded by Score Service'
        AFTER score_points_used,
    ADD COLUMN score_hold_code VARCHAR(80) NULL
        COMMENT 'Score Service hold reference'
        AFTER score_discount;
