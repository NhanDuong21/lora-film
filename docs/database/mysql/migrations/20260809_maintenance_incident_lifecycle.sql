-- Bổ sung vòng đời vận hành cho lịch bảo trì phòng chiếu.
-- Chạy trên movie_db trước khi khởi động phiên bản movie-service mới.

ALTER TABLE auditorium_maintenance_windows
    ADD COLUMN maintenance_type VARCHAR(30) NOT NULL DEFAULT 'PLANNED'
        COMMENT 'PLANNED: bảo trì có kế hoạch, EMERGENCY: sự cố khẩn cấp'
        AFTER reason,
    ADD COLUMN actual_end_time TIMESTAMP NULL
        COMMENT 'Thời điểm thực tế phòng hoạt động trở lại'
        AFTER status,
    ADD COLUMN resolved_by BIGINT NULL
        AFTER actual_end_time,
    ADD COLUMN resolution_note VARCHAR(500) NULL
        AFTER resolved_by,
    ADD COLUMN extension_note VARCHAR(500) NULL
        AFTER resolution_note;

UPDATE auditorium_maintenance_windows
SET maintenance_type = 'PLANNED'
WHERE maintenance_type IS NULL OR maintenance_type = '';
