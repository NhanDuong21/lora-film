
CREATE TABLE `daily_revenue_stats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
  `stat_date` date UNIQUE NOT NULL COMMENT 'Ngay tong hop du lieu theo timezone Asia/Ho_Chi_Minh',
  `total_revenue` decimal(14,2) NOT NULL DEFAULT 0 COMMENT 'Net revenue thuc thu trong ngay sau discount va refund',
  `total_bookings_count` int NOT NULL DEFAULT 0 COMMENT 'Tong so booking thanh toan thanh cong trong ngay',
  `cancelled_bookings_count` int NOT NULL DEFAULT 0 COMMENT 'Tong so booking bi huy hoac timeout trong ngay',
  `total_tickets_sold` int NOT NULL DEFAULT 0 COMMENT 'Tong so ve da ban thanh cong trong ngay',
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Lan cap nhat gan nhat'
);

CREATE TABLE `movie_revenue_stats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `movie_id` bigint UNIQUE NOT NULL COMMENT 'Logical Ref sang movies.id cua Movie Service',
  `movie_title` varchar(255) NOT NULL COMMENT 'Snapshot ten phim de query report nhanh ma khong can goi Movie Service',
  `total_tickets_sold` int NOT NULL DEFAULT 0 COMMENT 'Lifetime tong so ve da ban cua phim',
  `total_revenue` decimal(14,2) NOT NULL DEFAULT 0 COMMENT 'Lifetime net revenue cua phim sau discount va refund',
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Lan cap nhat gan nhat'
);

CREATE TABLE `movie_daily_revenue_stats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `movie_id` bigint NOT NULL COMMENT 'Logical Ref sang movies.id cua Movie Service',
  `movie_title` varchar(255) NOT NULL COMMENT 'Movie title snapshot',
  `stat_date` date NOT NULL COMMENT 'Ngay tong hop theo timezone Asia/Ho_Chi_Minh',
  `tickets_sold` int NOT NULL DEFAULT 0 COMMENT 'Net ticket count cua phim trong ngay',
  `revenue` decimal(14,2) NOT NULL DEFAULT 0 COMMENT 'Net revenue cua phim trong ngay',
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Lan cap nhat gan nhat',
  UNIQUE KEY `uk_movie_daily_revenue_movie_date` (`movie_id`, `stat_date`),
  INDEX `idx_movie_daily_revenue_movie_id` (`movie_id`),
  INDEX `idx_movie_daily_revenue_stat_date` (`stat_date`)
);

CREATE TABLE `processed_analytics_events` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` varchar(150) UNIQUE NOT NULL COMMENT 'Unique event ID dung de chong xu ly trung',
  `event_type` varchar(100) NOT NULL COMMENT 'PAYMENT_SUCCEEDED, PAYMENT_REFUNDED, BOOKING_CANCELLED',
  `source_service` varchar(100) NOT NULL COMMENT 'Service publish event',
  `processed_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT 'Thoi diem event duoc aggregate thanh cong',
  INDEX `idx_processed_analytics_events_type` (`event_type`),
  INDEX `idx_processed_analytics_events_processed_at` (`processed_at`)
);
