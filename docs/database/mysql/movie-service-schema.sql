CREATE TABLE `movies` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Logical Ref sang cac service khac',
  `title` varchar(255) NOT NULL,
  `description` text,
  `duration_minutes` int NOT NULL,
  `director` varchar(100),
  `actor` varchar(255),
  `release_date` date,
  `end_date` date,
  `poster_url` varchar(255),
  `trailer_url` varchar(255),
  `age_rating` varchar(10) COMMENT 'P, T13, T16, T18',
  `status` varchar(30) NOT NULL DEFAULT 'UPCOMING' COMMENT 'UPCOMING, NOW_SHOWING, ENDED, INACTIVE',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `genres` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `genre_name` varchar(100) UNIQUE NOT NULL
);

CREATE TABLE `movies_genres` (
  `movie_id` bigint NOT NULL,
  `genre_id` int NOT NULL,
  PRIMARY KEY (`movie_id`, `genre_id`)
);

CREATE TABLE `rooms` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `room_name` varchar(50) UNIQUE NOT NULL COMMENT 'e.g., Phong 01, Phong 02 (IMAX)',
  `total_seats` int NOT NULL,
  `screen_type` varchar(20) DEFAULT '2D' COMMENT '2D, 3D, IMAX',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, MAINTENANCE, INACTIVE'
);

CREATE TABLE `seats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `room_id` int NOT NULL,
  `seat_row` varchar(5) NOT NULL COMMENT 'e.g., A, B, C',
  `seat_number` int NOT NULL,
  `seat_type` varchar(20) DEFAULT 'STANDARD' COMMENT 'STANDARD, VIP, SWEETBOX',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, MAINTENANCE, INACTIVE',
  UNIQUE KEY `uk_seats_room_position` (`room_id`, `seat_row`, `seat_number`)
);

CREATE TABLE `showtimes` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Luong Dat Ve se tham chieu toi day',
  `movie_id` bigint NOT NULL,
  `room_id` int NOT NULL,
  `start_time` timestamp NOT NULL,
  `end_time` timestamp NOT NULL,
  `ticket_price` decimal(10,2) NOT NULL COMMENT 'Gia ve goc cho suat chieu nay',
  `status` varchar(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED, OPEN, CANCELLED, COMPLETED, CLOSED',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `chk_showtimes_time` CHECK (`end_time` > `start_time`)
);

ALTER TABLE `movies_genres` ADD FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`) ON DELETE CASCADE;

ALTER TABLE `movies_genres` ADD FOREIGN KEY (`genre_id`) REFERENCES `genres` (`id`) ON DELETE CASCADE;

ALTER TABLE `seats` ADD FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE;

ALTER TABLE `showtimes` ADD FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`);

ALTER TABLE `showtimes` ADD FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`);

CREATE INDEX `idx_movies_status_release_date` ON `movies` (`status`, `release_date`);

CREATE INDEX `idx_showtimes_movie_start` ON `showtimes` (`movie_id`, `start_time`);
CREATE INDEX `idx_showtimes_room_time` ON `showtimes` (`room_id`, `start_time`, `end_time`);
CREATE INDEX `idx_showtimes_status_start` ON `showtimes` (`status`, `start_time`);
