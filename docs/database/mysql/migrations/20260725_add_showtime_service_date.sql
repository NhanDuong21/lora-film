-- Persist the authoritative cinema business day on each Showtime.
-- Historical rows are reconstructed once because the field did not previously exist.
-- Requires MySQL timezone tables to contain the IANA zones stored in cinemas.timezone.

ALTER TABLE showtimes
    ADD COLUMN service_date DATE NULL
        COMMENT 'Authoritative cinema business/service day'
        AFTER end_time;

DROP TEMPORARY TABLE IF EXISTS invalid_showtime_service_date_backfill;
CREATE TEMPORARY TABLE invalid_showtime_service_date_backfill AS
SELECT
    s.public_id AS showtime_public_id,
    c.public_id AS cinema_public_id,
    c.timezone
FROM showtimes s
JOIN cinemas c ON c.id = s.cinema_id
WHERE c.timezone IS NULL
   OR TRIM(c.timezone) = ''
   OR CONVERT_TZ(s.start_time, '+00:00', c.timezone) IS NULL;

-- Stop here when this returns any rows. Correct the cinema timezone or load the
-- MySQL timezone tables, then rerun the migration from a clean transaction.
SELECT * FROM invalid_showtime_service_date_backfill;

DELIMITER //
CREATE PROCEDURE assert_showtime_service_date_backfill_ready()
BEGIN
    IF EXISTS (SELECT 1 FROM invalid_showtime_service_date_backfill) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Showtime service_date backfill blocked by invalid or unavailable cinema IANA timezone';
    END IF;
END//
DELIMITER ;

CALL assert_showtime_service_date_backfill_ready();
DROP PROCEDURE assert_showtime_service_date_backfill_ready;

UPDATE showtimes s
JOIN cinemas c ON c.id = s.cinema_id
SET s.service_date = DATE(CONVERT_TZ(s.start_time, '+00:00', c.timezone))
WHERE s.service_date IS NULL;

ALTER TABLE showtimes
    MODIFY COLUMN service_date DATE NOT NULL
        COMMENT 'Authoritative cinema business/service day';

CREATE INDEX idx_showtimes_customer_service_date
    ON showtimes (service_date, status, movie_id, cinema_id);

DROP TEMPORARY TABLE invalid_showtime_service_date_backfill;
