CREATE TABLE `daily_revenue_stats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
  `stat_date` date UNIQUE NOT NULL COMMENT 'Ngay tong hop du lieu, e.g., 2026-06-03',
  `total_revenue` decimal(14,2) NOT NULL DEFAULT 0 COMMENT 'Tong doanh thu thuc te thu duoc trong ngay',
  `total_bookings_count` int NOT NULL DEFAULT 0 COMMENT 'Tong so don hang thanh cong',
  `cancelled_bookings_count` int NOT NULL DEFAULT 0 COMMENT 'Tong so don hang bi huy/timeout',
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `movie_revenue_stats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `movie_id` bigint UNIQUE NOT NULL COMMENT 'Logical Ref sang movies.id cua Movie Service',
  `movie_title` varchar(255) NOT NULL COMMENT 'Luu dem ten phim de hien thi bao cao nhanh ma khong can goi API sang Movie Service',
  `total_tickets_sold` int NOT NULL DEFAULT 0 COMMENT 'Tong so luong ve da ban ra',
  `total_revenue` decimal(14,2) NOT NULL DEFAULT 0 COMMENT 'Tong doanh thu rieng cua bo phim nay',
  `updated_at` timestamp DEFAULT (now())
);
