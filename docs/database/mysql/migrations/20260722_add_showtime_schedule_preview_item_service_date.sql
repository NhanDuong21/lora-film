ALTER TABLE showtime_schedule_preview_items
    ADD COLUMN service_date DATE NULL
    COMMENT 'Authoritative operating service date from the originating operating window';
