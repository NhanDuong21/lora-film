-- Repair data written by the legacy/demo seed so it matches the Java enum contracts.
-- Run once against the existing service databases before restarting the services.

USE movie_db;

UPDATE auditoriums
SET screen_type = 'STANDARD'
WHERE screen_type = 'PREMIUM';

UPDATE auditoriums
SET sound_type = 'STANDARD'
WHERE sound_type = 'DOLBY_7_1';

ALTER TABLE auditoriums
    ADD CONSTRAINT chk_auditorium_screen_type
    CHECK (screen_type IN ('STANDARD', 'IMAX', '4DX', 'SCREENX'));

ALTER TABLE auditoriums
    ADD CONSTRAINT chk_auditorium_sound_type
    CHECK (sound_type IN ('STANDARD', 'DOLBY_ATMOS'));

USE payment_db;

UPDATE payment_outbox_events
SET destination = 'BOOKING_SERVICE_REST'
WHERE destination = 'booking-payment-results';

ALTER TABLE payment_outbox_events
    ADD CONSTRAINT chk_payment_outbox_destination
    CHECK (destination IN ('BOOKING_SERVICE_REST', 'ANALYTICS_KAFKA'));

USE notification_db;

UPDATE notification_requests
SET status = 'ACCEPTED'
WHERE status = 'SCHEDULED';

ALTER TABLE notification_requests
    ADD CONSTRAINT ck_notification_request_status
    CHECK (status IN (
        'ACCEPTED', 'PROCESSING', 'COMPLETED', 'PARTIALLY_FAILED', 'FAILED', 'CANCELLED'
    ));

USE promotion_db;

-- The legacy seed used COMPLIANT, while LegalStatus in the service exposes PASSED.
UPDATE promotion_campaigns
SET legal_status = 'PASSED'
WHERE legal_status = 'COMPLIANT';

ALTER TABLE promotion_campaigns
    ADD CONSTRAINT chk_promotion_campaign_legal_status
    CHECK (legal_status IN ('PENDING', 'PASSED', 'FAILED'));
