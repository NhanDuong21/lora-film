-- Additive migration for Demand-Aware Auto Schedule.
-- Apply each section to its service database after a schema backup.
-- New columns are nullable/defaulted so legacy rows remain readable.

USE movie_db;

ALTER TABLE cinemas
    ADD COLUMN auto_schedule_engine VARCHAR(20) NOT NULL DEFAULT 'CP_SAT' AFTER closed_date;

ALTER TABLE showtime_schedule_previews
    ADD COLUMN request_scope_json JSON NULL AFTER request_fingerprint,
    ADD COLUMN policy_version VARCHAR(50) NULL AFTER request_scope_json,
    ADD COLUMN demand_model_version VARCHAR(64) NULL AFTER policy_version,
    ADD COLUMN solver_version VARCHAR(64) NULL AFTER demand_model_version,
    ADD COLUMN solver_status VARCHAR(30) NULL AFTER solver_version,
    ADD COLUMN eligibility_fingerprint CHAR(64) NULL AFTER solver_status,
    ADD COLUMN pricing_fingerprint CHAR(64) NULL AFTER eligibility_fingerprint,
    ADD COLUMN configuration_fingerprint CHAR(64) NULL AFTER pricing_fingerprint,
    ADD COLUMN objective_value DECIMAL(19,3) NULL AFTER configuration_fingerprint,
    ADD COLUMN objective_best_bound DECIMAL(19,3) NULL AFTER objective_value,
    ADD COLUMN solver_duration_millis BIGINT NULL AFTER objective_best_bound,
    ADD COLUMN solver_explanation VARCHAR(500) NULL AFTER solver_duration_millis,
    ADD COLUMN expected_attendance DECIMAL(19,2) NULL AFTER solver_explanation,
    ADD COLUMN expected_occupancy DECIMAL(12,6) NULL AFTER expected_attendance,
    ADD COLUMN expected_revenue DECIMAL(19,2) NULL AFTER expected_occupancy,
    ADD COLUMN expected_contribution DECIMAL(19,2) NULL AFTER expected_revenue;

ALTER TABLE showtime_schedule_preview_items
    ADD COLUMN pricing_snapshot_json JSON NULL AFTER score_breakdown_json,
    ADD COLUMN expected_attendance DECIMAL(12,2) NULL AFTER pricing_snapshot_json,
    ADD COLUMN expected_occupancy DECIMAL(12,6) NULL AFTER expected_attendance,
    ADD COLUMN expected_revenue DECIMAL(19,2) NULL AFTER expected_occupancy,
    ADD COLUMN expected_contribution DECIMAL(19,2) NULL AFTER expected_revenue,
    ADD COLUMN demand_confidence DECIMAL(12,6) NULL AFTER expected_contribution,
    ADD COLUMN demand_explanation VARCHAR(500) NULL AFTER demand_confidence,
    ADD COLUMN demand_model_version VARCHAR(64) NULL AFTER demand_explanation,
    ADD COLUMN prime_time BOOLEAN NOT NULL DEFAULT FALSE AFTER demand_model_version,
    ADD COLUMN risk_flags_json JSON NULL AFTER prime_time;

USE payment_db;

ALTER TABLE payment_analytics_snapshots
    ADD COLUMN auditorium_public_id CHAR(36) NULL AFTER cinema_public_id,
    ADD COLUMN showtime_starts_at DATETIME(6) NULL AFTER auditorium_public_id,
    ADD COLUMN auditorium_capacity INT NULL AFTER showtime_starts_at,
    ADD COLUMN movie_format VARCHAR(30) NULL AFTER auditorium_capacity;

USE analytics_db;

ALTER TABLE fact_booking_metrics
    ADD COLUMN showtime_starts_at DATETIME(6) NULL AFTER showtime_public_id,
    ADD COLUMN movie_format VARCHAR(30) NULL AFTER showtime_starts_at;

CREATE INDEX idx_fact_cinema_showtime_start
    ON fact_booking_metrics (cinema_public_id, showtime_starts_at);
