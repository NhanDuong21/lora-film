-- Align existing databases with the canonical movie-service schema.
ALTER TABLE cinema_closure_periods
    ADD COLUMN service_date DATE NULL
        COMMENT 'Authoritative cinema business/service day'
        AFTER end_time;

-- MySQL CONVERT_TZ requires timezone tables for IANA zone names. Rows whose
-- timezone cannot be resolved are intentionally left NULL so the assertion
-- below blocks an unsafe migration instead of inventing a service date.
UPDATE cinema_closure_periods cp
JOIN cinemas c ON c.id = cp.cinema_id
SET cp.service_date = DATE(CONVERT_TZ(cp.start_time, '+00:00', c.timezone))
WHERE cp.service_date IS NULL;

DROP PROCEDURE IF EXISTS assert_cinema_closure_service_date_ready;
DELIMITER $$
CREATE PROCEDURE assert_cinema_closure_service_date_ready()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM cinema_closure_periods
        WHERE service_date IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cinema closure service_date backfill failed; verify cinema IANA timezones and MySQL timezone tables';
    END IF;
END$$
DELIMITER ;

CALL assert_cinema_closure_service_date_ready();
DROP PROCEDURE assert_cinema_closure_service_date_ready;

ALTER TABLE cinema_closure_periods
    MODIFY COLUMN service_date DATE NOT NULL
        COMMENT 'Authoritative cinema business/service day';
