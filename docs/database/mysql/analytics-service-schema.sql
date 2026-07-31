-- Analytics Service manual bootstrap schema.
--
-- This project intentionally does not use Flyway. Apply this file manually
-- after dropping/recreating analytics_db. All KPI, forecast, anomaly, health
-- score, root-cause and recommendation calculations remain in Java; this file
-- only defines persistence structures, integrity constraints and indexes.
--
-- No table in this schema contains a cross-service foreign key, calculation
-- trigger, generated KPI expression, stored procedure, or Booking DB reference.
CREATE DATABASE IF NOT EXISTS `analytics_db`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `analytics_db`;

CREATE TABLE `business_alerts` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `insight_id` bigint NOT NULL,
  `entity_type` varchar(30) NOT NULL,
  `entity_key` varchar(100) NOT NULL,
  `severity` varchar(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `acknowledged` bit(1) NOT NULL,
  `acknowledged_by` varchar(100),
  `acknowledged_at` datetime(6),
  `resolved` bit(1) NOT NULL DEFAULT b'0',
  `resolved_at` datetime(6),
  `created_at` datetime(6) NOT NULL,
  UNIQUE KEY `uk_alert_insight` (`insight_id`),
  INDEX `idx_alert_created` (`created_at`),
  INDEX `idx_alert_active` (`resolved`, `acknowledged`, `severity`, `created_at`)
);

CREATE TABLE `business_insights` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `fingerprint` varchar(180) NOT NULL UNIQUE,
  `stat_date` date NOT NULL,
  `entity_type` varchar(30) NOT NULL,
  `entity_key` varchar(100) NOT NULL,
  `severity` varchar(20) NOT NULL,
  `category` varchar(100) NOT NULL,
  `title` varchar(255) NOT NULL,
  `summary` text NOT NULL,
  `root_cause` text NOT NULL,
  `evidence_json` json,
  `baseline_start_date` date,
  `baseline_end_date` date,
  `expected_value` decimal(19,6),
  `actual_value` decimal(19,6),
  `deviation_rate` decimal(12,6),
  `analysis_version` varchar(64),
  `confidence_score` decimal(12,6) NOT NULL,
  `resolved` bit(1) NOT NULL,
  `resolved_at` datetime(6),
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6),
  INDEX `idx_insight_date` (`stat_date`),
  INDEX `idx_insight_severity` (`severity`),
  INDEX `idx_insight_active_date` (`resolved`, `stat_date`, `created_at`)
);

CREATE TABLE `cinema_performance_daily` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `cinema_key` varchar(100) NOT NULL,
  `cinema_name` varchar(255),
  `stat_date` date NOT NULL,
  `gross_revenue` decimal(19,2) NOT NULL,
  `discount_amount` decimal(19,2) NOT NULL,
  `refund_amount` decimal(19,2) NOT NULL,
  `net_revenue` decimal(19,2) NOT NULL,
  `booking_count` bigint NOT NULL,
  `ticket_count` bigint NOT NULL,
  `occupancy_rate` decimal(12,6) NOT NULL,
  `average_booking_value` decimal(19,2) NOT NULL,
  `refund_rate` decimal(12,6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint,
  UNIQUE KEY `uk_cinema_date` (`cinema_key`, `stat_date`),
  INDEX `idx_cinema_kpi_date` (`stat_date`)
);

CREATE TABLE `customer_segment_daily` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `membership_tier` varchar(50) NOT NULL,
  `active_users` bigint NOT NULL,
  `new_users` bigint NOT NULL,
  `returning_users` bigint NOT NULL,
  `total_spending` decimal(19,2) NOT NULL,
  `average_spending` decimal(19,2) NOT NULL,
  `customer_lifetime_value` decimal(19,2) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint,
  UNIQUE KEY `uk_segment_date` (`membership_tier`, `stat_date`),
  INDEX `idx_segment_kpi_date` (`stat_date`)
);

CREATE TABLE `daily_business_kpis` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `stat_date` date NOT NULL UNIQUE,
  `gross_revenue` decimal(19,2) NOT NULL,
  `discount_amount` decimal(19,2) NOT NULL,
  `refund_amount` decimal(19,2) NOT NULL,
  `net_revenue` decimal(19,2) NOT NULL,
  `booking_count` bigint NOT NULL,
  `refund_booking_count` bigint NOT NULL,
  `cancelled_booking_count` bigint NOT NULL,
  `ticket_count` bigint NOT NULL,
  `new_customer_count` bigint NOT NULL,
  `returning_customer_count` bigint NOT NULL,
  `average_booking_value` decimal(19,2) NOT NULL,
  `average_ticket_price` decimal(19,2) NOT NULL,
  `refund_rate` decimal(12,6) NOT NULL,
  `cancel_rate` decimal(12,6) NOT NULL,
  `promotion_usage_rate` decimal(12,6) NOT NULL,
  `occupancy_rate` decimal(12,6) NOT NULL,
  `data_completeness` decimal(12,6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint
);

CREATE TABLE `fact_booking_cancellations` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` varchar(150) NOT NULL UNIQUE,
  `booking_key` varchar(100) NOT NULL,
  `previous_status` varchar(50) NOT NULL,
  `reason` varchar(50) NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `business_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  INDEX `idx_fact_cancel_date` (`business_date`),
  INDEX `idx_fact_cancel_booking` (`booking_key`)
);

CREATE TABLE `fact_booking_metrics` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` varchar(150) NOT NULL UNIQUE,
  `payment_public_id` varchar(64) NOT NULL,
  `booking_public_id` varchar(64) NOT NULL,
  `user_public_id` varchar(64),
  `movie_id` bigint,
  `movie_key` varchar(100) NOT NULL,
  `movie_public_id` varchar(64),
  `movie_title` varchar(255) NOT NULL,
  `cinema_public_id` varchar(100),
  `cinema_name` varchar(255),
  `auditorium_public_id` varchar(100),
  `showtime_public_id` varchar(100),
  `promotion_public_id` varchar(100),
  `promotion_name` varchar(255),
  `membership_tier` varchar(50),
  `payment_method` varchar(50),
  `currency` varchar(3) NOT NULL,
  `gross_amount` decimal(19,2) NOT NULL,
  `discount_amount` decimal(19,2) NOT NULL,
  `net_revenue` decimal(19,2) NOT NULL,
  `ticket_count` int NOT NULL,
  `available_seats` int,
  `booking_status` varchar(30) NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `business_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  INDEX `idx_fact_booking_date` (`business_date`),
  INDEX `idx_fact_booking_key` (`booking_public_id`, `occurred_at`),
  INDEX `idx_fact_movie_date` (`movie_key`, `business_date`),
  INDEX `idx_fact_cinema_date` (`cinema_public_id`, `business_date`)
);

CREATE TABLE `fact_payment_refunds` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` varchar(150) NOT NULL UNIQUE,
  `payment_public_id` varchar(64) NOT NULL,
  `booking_public_id` varchar(64) NOT NULL,
  `refund_amount` decimal(19,2) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `refund_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  INDEX `idx_fact_refund_date` (`refund_date`),
  INDEX `idx_fact_refund_booking` (`booking_public_id`, `refund_date`)
);

CREATE TABLE `forecast_results` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_type` varchar(30) NOT NULL,
  `entity_key` varchar(100) NOT NULL,
  `forecast_date` date NOT NULL,
  `forecast_type` varchar(30) NOT NULL,
  `as_of_date` date,
  `predicted_value` decimal(19,6) NOT NULL,
  `prediction_lower_bound` decimal(19,6),
  `prediction_upper_bound` decimal(19,6),
  `confidence_score` decimal(12,6) NOT NULL,
  `algorithm` varchar(100) NOT NULL,
  `model_version` varchar(64),
  `training_start_date` date NOT NULL,
  `training_end_date` date NOT NULL,
  `generated_at` datetime(6) NOT NULL,
  `version` bigint,
  UNIQUE KEY `uk_forecast_result` (`entity_type`, `entity_key`, `forecast_date`, `forecast_type`),
  INDEX `idx_forecast_date` (`forecast_date`)
);

CREATE TABLE `kpi_calculation_runs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `run_id` varchar(64) NOT NULL UNIQUE,
  `stat_date` date NOT NULL,
  `status` varchar(20) NOT NULL,
  `completed_stage` varchar(60),
  `error_message` varchar(1000),
  `started_at` datetime(6) NOT NULL,
  `completed_at` datetime(6),
  INDEX `idx_kpi_run_date` (`stat_date`, `started_at`),
  INDEX `idx_kpi_run_started` (`started_at`)
);

CREATE TABLE `movie_performance_daily` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `movie_key` varchar(100) NOT NULL,
  `movie_id` bigint,
  `movie_title` varchar(255) NOT NULL,
  `stat_date` date NOT NULL,
  `gross_revenue` decimal(19,2) NOT NULL,
  `discount_amount` decimal(19,2) NOT NULL,
  `refund_amount` decimal(19,2) NOT NULL,
  `net_revenue` decimal(19,2) NOT NULL,
  `booking_count` bigint NOT NULL,
  `ticket_count` bigint NOT NULL,
  `occupancy_rate` decimal(12,6) NOT NULL,
  `refund_rate` decimal(12,6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint,
  UNIQUE KEY `uk_movie_date` (`movie_key`, `stat_date`),
  INDEX `idx_movie_kpi_date` (`stat_date`)
);

CREATE TABLE `processed_analytics_events` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` varchar(150) NOT NULL UNIQUE,
  `event_type` varchar(100) NOT NULL,
  `source_service` varchar(100) NOT NULL,
  `aggregate_key` varchar(150) NOT NULL,
  `schema_version` varchar(20) NOT NULL,
  `payload_hash` varchar(64) NOT NULL,
  `source_topic` varchar(249),
  `source_partition` int,
  `source_offset` bigint,
  `correlation_id` varchar(100),
  `trace_id` varchar(100),
  `event_occurred_at` datetime(6),
  `received_at` datetime(6),
  `processed_at` datetime(6) NOT NULL,
  UNIQUE KEY `uk_processed_source_position`
    (`source_topic`, `source_partition`, `source_offset`),
  INDEX `idx_processed_event_type` (`event_type`),
  INDEX `idx_processed_event_at` (`processed_at`),
  INDEX `idx_processed_source_event` (`source_service`, `event_type`, `processed_at`)
);

CREATE TABLE `promotion_performance_daily` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `promotion_key` varchar(100) NOT NULL,
  `promotion_name` varchar(255),
  `stat_date` date NOT NULL,
  `usage_count` bigint NOT NULL,
  `discount_cost` decimal(19,2) NOT NULL,
  `generated_revenue` decimal(19,2) NOT NULL,
  `roi` decimal(19,6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint,
  UNIQUE KEY `uk_promotion_date` (`promotion_key`, `stat_date`),
  INDEX `idx_promotion_kpi_date` (`stat_date`)
);

CREATE TABLE `recommendations` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `insight_id` bigint NOT NULL,
  `target_service` varchar(100) NOT NULL,
  `action_type` varchar(100) NOT NULL,
  `priority` varchar(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `expected_impact` text NOT NULL,
  `estimated_impact_value` decimal(19,6),
  `impact_unit` varchar(30),
  `confidence_score` decimal(12,6) NOT NULL,
  `status` varchar(30) NOT NULL,
  `accepted_by` varchar(100),
  `accepted_at` datetime(6),
  `completed_at` datetime(6),
  `expires_at` datetime(6),
  `created_at` datetime(6) NOT NULL,
  UNIQUE KEY `uk_recommendation_insight_action` (`insight_id`, `action_type`),
  INDEX `idx_recommendation_created` (`created_at`),
  INDEX `idx_recommendation_work_queue` (`status`, `priority`, `created_at`)
);

-- Backend-computed health score snapshots. No score formula exists in SQL.
CREATE TABLE `analytics_health_scores` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_type` varchar(30) NOT NULL,
  `entity_key` varchar(100) NOT NULL,
  `stat_date` date NOT NULL,
  `overall_score` decimal(12,6) NOT NULL,
  `revenue_score` decimal(12,6),
  `demand_score` decimal(12,6),
  `occupancy_score` decimal(12,6),
  `customer_score` decimal(12,6),
  `operational_score` decimal(12,6),
  `data_quality_score` decimal(12,6),
  `health_status` varchar(30) NOT NULL,
  `confidence_score` decimal(12,6) NOT NULL,
  `algorithm_version` varchar(64) NOT NULL,
  `drivers_json` json,
  `calculated_at` datetime(6) NOT NULL,
  `version` bigint,
  UNIQUE KEY `uk_health_score_entity_date`
    (`entity_type`, `entity_key`, `stat_date`),
  INDEX `idx_health_score_date_status`
    (`stat_date`, `health_status`, `overall_score`)
);

-- Backend-computed anomaly results and their statistical evidence.
CREATE TABLE `anomaly_detections` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `fingerprint` varchar(180) NOT NULL,
  `insight_id` bigint,
  `stat_date` date NOT NULL,
  `entity_type` varchar(30) NOT NULL,
  `entity_key` varchar(100) NOT NULL,
  `metric_name` varchar(100) NOT NULL,
  `actual_value` decimal(19,6) NOT NULL,
  `expected_value` decimal(19,6),
  `deviation_rate` decimal(12,6),
  `anomaly_score` decimal(19,6) NOT NULL,
  `detection_method` varchar(100) NOT NULL,
  `severity` varchar(20) NOT NULL,
  `status` varchar(30) NOT NULL,
  `evidence_json` json,
  `detected_at` datetime(6) NOT NULL,
  `resolved_at` datetime(6),
  UNIQUE KEY `uk_anomaly_fingerprint` (`fingerprint`),
  INDEX `idx_anomaly_active`
    (`status`, `severity`, `stat_date`),
  INDEX `idx_anomaly_entity_metric`
    (`entity_type`, `entity_key`, `metric_name`, `stat_date`)
);

-- Ranked RCA factors generated by Java for each insight.
CREATE TABLE `root_cause_factors` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `insight_id` bigint NOT NULL,
  `rank_order` int NOT NULL,
  `cause_type` varchar(100) NOT NULL,
  `dimension_type` varchar(50),
  `dimension_key` varchar(100),
  `contribution_score` decimal(12,6) NOT NULL,
  `evidence_json` json,
  `created_at` datetime(6) NOT NULL,
  UNIQUE KEY `uk_root_cause_insight_rank` (`insight_id`, `rank_order`),
  INDEX `idx_root_cause_dimension`
    (`dimension_type`, `dimension_key`)
);

-- Backtesting metrics generated by Java; the database does not evaluate models.
CREATE TABLE `forecast_model_metrics` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `entity_type` varchar(30) NOT NULL,
  `entity_key` varchar(100) NOT NULL,
  `forecast_type` varchar(30) NOT NULL,
  `algorithm` varchar(100) NOT NULL,
  `model_version` varchar(64) NOT NULL,
  `evaluation_date` date NOT NULL,
  `test_start_date` date NOT NULL,
  `test_end_date` date NOT NULL,
  `sample_size` int NOT NULL,
  `mae` decimal(19,6),
  `rmse` decimal(19,6),
  `mape` decimal(12,6),
  `bias` decimal(19,6),
  `calculated_at` datetime(6) NOT NULL,
  UNIQUE KEY `uk_forecast_model_evaluation`
    (`entity_type`, `entity_key`, `forecast_type`, `model_version`, `evaluation_date`),
  INDEX `idx_forecast_model_quality`
    (`forecast_type`, `evaluation_date`, `mape`)
);

-- Event-pipeline quality and freshness snapshots generated by Java/metrics jobs.
CREATE TABLE `analytics_data_quality_daily` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `source_service` varchar(100) NOT NULL,
  `event_type` varchar(100) NOT NULL,
  `received_count` bigint NOT NULL,
  `accepted_count` bigint NOT NULL,
  `duplicate_count` bigint NOT NULL,
  `rejected_count` bigint NOT NULL,
  `dlq_count` bigint NOT NULL,
  `late_event_count` bigint NOT NULL,
  `average_lag_seconds` decimal(19,6),
  `maximum_lag_seconds` bigint,
  `completeness_score` decimal(12,6) NOT NULL,
  `freshness_status` varchar(30) NOT NULL,
  `calculated_at` datetime(6) NOT NULL,
  UNIQUE KEY `uk_data_quality_source_date`
    (`stat_date`, `source_service`, `event_type`),
  INDEX `idx_data_quality_status`
    (`freshness_status`, `stat_date`)
);

-- Tracks asynchronous recalculate, backfill and rebuild operations.
CREATE TABLE `analytics_job_runs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `request_id` varchar(100) NOT NULL,
  `job_type` varchar(30) NOT NULL,
  `mode` varchar(30) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `status` varchar(20) NOT NULL,
  `requested_by` varchar(100) NOT NULL,
  `requested_at` datetime(6) NOT NULL,
  `started_at` datetime(6),
  `completed_at` datetime(6),
  `processed_days` int NOT NULL DEFAULT 0,
  `total_days` int NOT NULL,
  `error_message` varchar(1000),
  UNIQUE KEY `uk_analytics_job_request` (`request_id`),
  INDEX `idx_analytics_job_status` (`status`, `requested_at`),
  INDEX `idx_analytics_job_range` (`start_date`, `end_date`)
);

-- Same-service referential integrity only; no cross-service foreign keys.
ALTER TABLE `business_alerts`
  ADD CONSTRAINT `fk_alert_insight`
  FOREIGN KEY (`insight_id`) REFERENCES `business_insights` (`id`);

ALTER TABLE `recommendations`
  ADD CONSTRAINT `fk_recommendation_insight`
  FOREIGN KEY (`insight_id`) REFERENCES `business_insights` (`id`);

ALTER TABLE `anomaly_detections`
  ADD CONSTRAINT `fk_anomaly_insight`
  FOREIGN KEY (`insight_id`) REFERENCES `business_insights` (`id`)
  ON DELETE SET NULL;

ALTER TABLE `root_cause_factors`
  ADD CONSTRAINT `fk_root_cause_insight`
  FOREIGN KEY (`insight_id`) REFERENCES `business_insights` (`id`);
