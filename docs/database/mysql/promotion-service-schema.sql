-- LoraFilm promotion-service schema for MySQL 8.
-- DESTRUCTIVE: this intentionally replaces promotion_db.
-- Apply this file once before starting Promotion Service.
-- Hibernate validates this schema; the service does not run Flyway or mutate DDL.

DROP DATABASE IF EXISTS promotion_db;
CREATE DATABASE promotion_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE promotion_db;

-- Tables are emitted in a stable alphabetical order. Disable relationship
-- checks while bootstrapping so foreign-key targets may appear later.
SET @PROMOTION_OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS;
SET @PROMOTION_OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_histories` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `new_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approver_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `comment` text COLLATE utf8mb4_unicode_ci,
  `approved_at` datetime(6) NOT NULL,
  `metadata_json` json DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_approval_public` (`public_id`),
  KEY `idx_approval_target` (`target_type`,`target_public_id`),
  KEY `idx_approval_approver` (`approver_public_id`),
  KEY `idx_approval_action` (`action`),
  KEY `idx_approval_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actor_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` text COLLATE utf8mb4_unicode_ci,
  `request_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trace_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `before_data` json DEFAULT NULL,
  `after_data` json DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_public` (`public_id`),
  KEY `idx_audit_entity` (`entity_type`,`entity_public_id`),
  KEY `idx_audit_actor` (`actor_public_id`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_request` (`request_id`),
  KEY `idx_audit_trace` (`trace_id`),
  KEY `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outbox_events` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `aggregate_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `aggregate_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payload` json NOT NULL,
  `headers_json` json DEFAULT NULL,
  `topic_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `publish_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `retry_count` int NOT NULL DEFAULT '0',
  `next_retry_at` datetime(6) DEFAULT NULL,
  `processing_started_at` datetime(6) DEFAULT NULL,
  `processing_owner` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbox_public` (`public_id`),
  KEY `idx_outbox_status` (`publish_status`),
  KEY `idx_outbox_topic` (`topic_name`),
  KEY `idx_outbox_retry` (`next_retry_at`),
  KEY `idx_outbox_claim` (`publish_status`,`next_retry_at`,`processing_started_at`,`created_at`),
  KEY `idx_outbox_aggregate` (`aggregate_type`,`aggregate_public_id`),
  KEY `idx_outbox_created` (`created_at`),
  KEY `idx_outbox_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_campaigns` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approval_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `legal_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` int NOT NULL DEFAULT '100',
  `stackable` tinyint(1) NOT NULL DEFAULT '0',
  `exclusive_campaign` tinyint(1) NOT NULL DEFAULT '0',
  `auto_activate` tinyint(1) NOT NULL DEFAULT '1',
  `auto_complete` tinyint(1) NOT NULL DEFAULT '1',
  `auto_pause_when_budget_exceeded` tinyint(1) NOT NULL DEFAULT '1',
  `kill_switch` tinyint(1) NOT NULL DEFAULT '0',
  `timezone` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  `start_at` datetime(6) NOT NULL,
  `end_at` datetime(6) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `approved_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `budget_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `budget_used` decimal(18,2) NOT NULL DEFAULT '0.00',
  `budget_reserved` decimal(18,2) NOT NULL DEFAULT '0.00',
  `budget_remaining` decimal(18,2) NOT NULL DEFAULT '0.00',
  `max_redemptions` int DEFAULT NULL,
  `redemption_count` int NOT NULL DEFAULT '0',
  `max_redemptions_per_user` int NOT NULL DEFAULT '1',
  `legal_notification_ref` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` text COLLATE utf8mb4_unicode_ci,
  `version` int NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_promotion_campaign_legal_status` CHECK ((`legal_status` in (_utf8mb4'PENDING',_utf8mb4'PASSED',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_configurations` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_key` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `value_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `editable` tinyint(1) NOT NULL DEFAULT '1',
  `requires_restart` tinyint(1) NOT NULL DEFAULT '0',
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `metadata_json` json DEFAULT NULL,
  `version` int NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configuration_public` (`public_id`),
  UNIQUE KEY `uk_configuration_key` (`config_key`),
  KEY `idx_configuration_category` (`category`),
  KEY `idx_configuration_deleted` (`deleted_at`),
  KEY `idx_configuration_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_idempotency_keys` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `idempotency_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `api_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `http_method` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `client_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_id` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `session_id` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_uri` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_body` json DEFAULT NULL,
  `response_body` json DEFAULT NULL,
  `response_status` int DEFAULT NULL,
  `reservation_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `booking_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processing_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `first_request_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `expired_at` datetime(6) NOT NULL,
  `metadata_json` json DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotency_public` (`public_id`),
  UNIQUE KEY `uk_idempotency_scope` (`client_id`,`api_name`,`idempotency_key`),
  KEY `idx_idempotency_user` (`user_public_id`),
  KEY `idx_idempotency_status` (`processing_status`),
  KEY `idx_idempotency_api` (`api_name`),
  KEY `idx_idempotency_request_hash` (`request_hash`),
  KEY `idx_idempotency_booking` (`booking_public_id`),
  KEY `idx_idempotency_payment` (`payment_public_id`),
  KEY `idx_idempotency_expired` (`expired_at`),
  KEY `idx_idempotency_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_integration_events` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_service` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_id` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `schema_version` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `correlation_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trace_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payload` json NOT NULL,
  `processing_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RECEIVED',
  `retry_count` int NOT NULL DEFAULT '0',
  `next_retry_at` datetime(6) DEFAULT NULL,
  `last_error` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_integration_event_public` (`public_id`),
  UNIQUE KEY `uk_integration_event_source_id` (`source_service`,`event_id`),
  KEY `idx_integration_event_status` (`processing_status`,`next_retry_at`,`created_at`),
  KEY `idx_integration_event_type` (`event_type`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_redemption_adjustments` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `redemption_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reservation_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `adjustment_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_amount` decimal(18,2) NOT NULL,
  `reason_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `version` int NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_promotion_adjustment_public` (`public_id`),
  UNIQUE KEY `uk_promotion_adjustment_reverse` (`redemption_public_id`,`adjustment_type`),
  KEY `idx_promotion_adjustment_reservation` (`reservation_public_id`,`occurred_at`),
  CONSTRAINT `fk_promotion_adjustment_redemption` FOREIGN KEY (`redemption_public_id`) REFERENCES `promotion_redemptions` (`public_id`),
  CONSTRAINT `chk_promotion_adjustment_amount` CHECK ((`discount_amount` >= 0)),
  CONSTRAINT `chk_promotion_adjustment_type` CHECK ((`adjustment_type` = _utf8mb4'REVERSE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_redemptions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reservation_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `promotion_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `campaign_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `promotion_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `promotion_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `promotion_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `promotion_priority` int NOT NULL,
  `promotion_stackable` tinyint(1) NOT NULL,
  `conditions_snapshot_json` json NOT NULL,
  `actions_snapshot_json` json NOT NULL,
  `sequence_no` int NOT NULL,
  `user_promotion_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `booking_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discount_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `original_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `final_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `confirmed_at` datetime(6) DEFAULT NULL,
  `rollback_at` datetime(6) DEFAULT NULL,
  `rollback_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `metadata_json` json DEFAULT NULL,
  `version` int NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_promotion_redemption_public` (`public_id`),
  UNIQUE KEY `uk_promotion_redemption_reservation_item` (`reservation_public_id`,`promotion_public_id`),
  KEY `idx_promotion_redemption_reservation` (`reservation_public_id`),
  KEY `idx_promotion_redemption_template` (`promotion_public_id`,`status`),
  KEY `idx_promotion_redemption_wallet` (`user_promotion_public_id`,`status`),
  KEY `idx_promotion_redemption_user` (`user_public_id`,`status`,`created_at`),
  KEY `idx_promotion_redemption_booking` (`booking_public_id`),
  KEY `idx_promotion_redemption_order` (`order_public_id`),
  KEY `idx_promotion_redemption_campaign` (`campaign_public_id`,`status`),
  KEY `idx_promotion_redemption_sequence` (`reservation_public_id`,`sequence_no`),
  CONSTRAINT `fk_promotion_redemption_template` FOREIGN KEY (`promotion_public_id`) REFERENCES `promotions` (`public_id`),
  CONSTRAINT `fk_promotion_redemption_wallet` FOREIGN KEY (`user_promotion_public_id`) REFERENCES `user_promotions` (`public_id`),
  CONSTRAINT `chk_promotion_redemption_amount_v2` CHECK (((`original_amount` >= 0) and (`discount_amount` >= 0) and (`final_amount` >= 0) and (`final_amount` = (`original_amount` - `discount_amount`)))),
  CONSTRAINT `chk_promotion_redemption_status_v2` CHECK ((`status` in (_utf8mb4'RESERVED',_utf8mb4'CONFIRMED',_utf8mb4'REVERSED',_utf8mb4'ROLLBACKED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_reservations` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reservation_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `booking_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reservation_scope_key` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `discount_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `final_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VND',
  `reservation_started_at` datetime(6) NOT NULL,
  `reservation_expired_at` datetime(6) NOT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `rollback_at` datetime(6) DEFAULT NULL,
  `rollback_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `release_reason_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `released_at` datetime(6) DEFAULT NULL,
  `released_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_service` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_reference` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason_detail` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `metadata_json` json DEFAULT NULL,
  `expiration_attempts` int NOT NULL DEFAULT '0',
  `expiration_last_attempt_at` datetime(6) DEFAULT NULL,
  `expiration_next_attempt_at` datetime(6) DEFAULT NULL,
  `expiration_error` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` int NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_promotion_reservation_public` (`public_id`),
  UNIQUE KEY `uk_promotion_reservation_code` (`reservation_code`),
  UNIQUE KEY `uk_promotion_reservation_scope` (`reservation_scope_key`),
  KEY `idx_reservation_user` (`user_public_id`),
  KEY `idx_reservation_booking` (`booking_public_id`),
  KEY `idx_reservation_order` (`order_public_id`),
  KEY `idx_reservation_status` (`status`),
  KEY `idx_reservation_expired` (`reservation_expired_at`),
  KEY `idx_reservation_deleted` (`deleted_at`),
  KEY `idx_reservation_expiration_due` (`status`,`reservation_expired_at`,`expiration_next_attempt_at`),
  KEY `idx_reservation_created` (`created_at`),
  KEY `idx_reservation_history_v2` (`status`,`created_at`),
  CONSTRAINT `chk_reservation_amount` CHECK (((`original_amount` >= 0) and (`discount_amount` >= 0) and (`final_amount` >= 0) and (`final_amount` = (`original_amount` - `discount_amount`)))),
  CONSTRAINT `chk_reservation_lifecycle_v3` CHECK ((((`status` = _latin1'ACTIVE') and (`confirmed_at` is null) and (`rollback_at` is null)) or ((`status` = _latin1'CONFIRMED') and (`confirmed_at` is not null) and (`payment_public_id` is not null) and (`rollback_at` is null)) or ((`status` = _latin1'REVERSED') and (`confirmed_at` is not null) and (`payment_public_id` is not null) and (`rollback_at` is not null) and (`rollback_reason` is not null)) or ((`status` = _latin1'RELEASED') and (`confirmed_at` is null) and (`rollback_at` is not null) and (`rollback_reason` is not null)) or ((`status` = _latin1'EXPIRED') and (`confirmed_at` is null) and (`rollback_at` is null)))),
  CONSTRAINT `chk_reservation_period` CHECK ((`reservation_expired_at` > `reservation_started_at`)),
  CONSTRAINT `chk_reservation_release_metadata` CHECK ((`status` <> _latin1'RELEASED' OR (`release_reason_type` IS NOT NULL AND `released_at` IS NOT NULL AND `released_by` IS NOT NULL AND `source_service` IS NOT NULL))),
  CONSTRAINT `chk_reservation_release_reason_type` CHECK ((`release_reason_type` IS NULL OR `release_reason_type` IN (_latin1'PAYMENT_FAILED',_latin1'PAYMENT_TIMEOUT',_latin1'CUSTOMER_CANCELLED_BOOKING',_latin1'STAFF_CANCELLED_BOOKING',_latin1'BOOKING_EXPIRED',_latin1'CAMPAIGN_PAUSED',_latin1'CAMPAIGN_KILL_SWITCH',_latin1'SYSTEM_COMPENSATION'))),
  CONSTRAINT `chk_reservation_status_v3` CHECK ((`status` in (_latin1'ACTIVE',_latin1'CONFIRMED',_latin1'REVERSED',_latin1'RELEASED',_latin1'EXPIRED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_scheduler_job_executions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `job_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trigger_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `instance_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `started_at` datetime(6) NOT NULL,
  `finished_at` datetime(6) DEFAULT NULL,
  `processed_count` int NOT NULL DEFAULT '0',
  `error_message` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scheduler_execution_public` (`public_id`),
  KEY `idx_scheduler_execution_job` (`job_name`,`started_at`),
  KEY `idx_scheduler_execution_status` (`status`,`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_scheduler_locks` (
  `job_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `locked_until` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`job_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `campaign_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `promotion_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `is_public` tinyint(1) NOT NULL DEFAULT '0',
  `priority` int NOT NULL DEFAULT '100',
  `stackable` tinyint(1) NOT NULL DEFAULT '0',
  `conditions_json` json NOT NULL,
  `actions_json` json NOT NULL,
  `metadata_json` json DEFAULT NULL,
  `max_redemptions` int DEFAULT NULL,
  `redemption_count` int NOT NULL DEFAULT '0',
  `max_redemptions_per_user` int NOT NULL DEFAULT '1',
  `valid_from` datetime(6) NOT NULL,
  `valid_to` datetime(6) NOT NULL,
  `version` int NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cloned_from_public_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_promotion_public` (`public_id`),
  UNIQUE KEY `uk_promotion_campaign_type_code` (`campaign_public_id`,`promotion_type`,`code`),
  KEY `idx_promotion_campaign` (`campaign_public_id`),
  KEY `idx_promotion_discovery` (`promotion_type`,`status`,`is_public`,`valid_from`,`valid_to`),
  KEY `idx_promotion_priority` (`priority`),
  KEY `idx_promotion_deleted` (`deleted_at`),
  KEY `idx_promotions_cloned_from` (`cloned_from_public_id`),
  CONSTRAINT `chk_promotion_counts` CHECK (((`redemption_count` >= 0) and (`max_redemptions_per_user` > 0) and ((`max_redemptions` is null) or (`max_redemptions` > 0)))),
  CONSTRAINT `chk_promotion_period` CHECK ((`valid_to` > `valid_from`)),
  CONSTRAINT `chk_promotion_priority` CHECK ((`priority` >= 0)),
  CONSTRAINT `chk_promotion_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'ACTIVE',_utf8mb4'PAUSED',_utf8mb4'DISABLED',_utf8mb4'EXPIRED'))),
  CONSTRAINT `chk_promotion_type` CHECK ((`promotion_type` in (_utf8mb4'AUTO',_utf8mb4'VOUCHER',_utf8mb4'COUPON'))),
  CONSTRAINT `chk_promotion_visibility` CHECK ((((`promotion_type` = _utf8mb4'VOUCHER') and (`code` is not null)) or ((`promotion_type` = _utf8mb4'COUPON') and (`code` is not null) and (`is_public` = false)) or ((`promotion_type` = _utf8mb4'AUTO') and (`is_public` = false))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_promotions` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `promotion_public_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE',
  `claimed_at` datetime(6) NOT NULL,
  `valid_from` datetime(6) NOT NULL,
  `valid_to` datetime(6) NOT NULL,
  `usage_count` int NOT NULL DEFAULT '0',
  `max_usage` int NOT NULL DEFAULT '1',
  `version` int NOT NULL DEFAULT '1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `updated_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_promotion_public` (`public_id`),
  KEY `idx_user_promotion_wallet` (`user_public_id`,`status`,`valid_to`),
  KEY `idx_user_promotion_template` (`promotion_public_id`),
  KEY `idx_user_promotion_deleted` (`deleted_at`),
  KEY `idx_user_promotion_owner_template` (`user_public_id`,`promotion_public_id`,`created_at`),
  CONSTRAINT `fk_user_promotion_template` FOREIGN KEY (`promotion_public_id`) REFERENCES `promotions` (`public_id`),
  CONSTRAINT `chk_user_promotion_period` CHECK ((`valid_to` > `valid_from`)),
  CONSTRAINT `chk_user_promotion_status` CHECK ((`status` in (_utf8mb4'AVAILABLE',_utf8mb4'USED',_utf8mb4'EXPIRED',_utf8mb4'REVOKED'))),
  CONSTRAINT `chk_user_promotion_usage` CHECK (((`usage_count` >= 0) and (`max_usage` > 0) and (`usage_count` <= `max_usage`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

SET FOREIGN_KEY_CHECKS = @PROMOTION_OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS = @PROMOTION_OLD_UNIQUE_CHECKS;
