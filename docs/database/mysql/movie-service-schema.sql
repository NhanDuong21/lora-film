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
  `status` varchar(30) DEFAULT 'UPCOMING' COMMENT 'UPCOMING, SHOWING, ENDED',
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
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
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, MAINTENANCE'
);

CREATE TABLE `seats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `room_id` int NOT NULL,
  `seat_row` varchar(5) NOT NULL COMMENT 'e.g., A, B, C',
  `seat_number` int NOT NULL,
  `seat_type` varchar(20) DEFAULT 'STANDARD' COMMENT 'STANDARD, VIP, SWEETBOX',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, BROKEN'
);

CREATE TABLE `showtimes` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Luong Dat Ve se tham chieu toi day',
  `movie_id` bigint NOT NULL,
  `room_id` int NOT NULL,
  `start_time` timestamp NOT NULL,
  `end_time` timestamp NOT NULL,
  `ticket_price` decimal(10,2) NOT NULL COMMENT 'Gia ve goc cho suat chieu nay',
  `status` varchar(20) DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE, CANCELLED',
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

ALTER TABLE `movies_genres` ADD FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`) ON DELETE CASCADE;

ALTER TABLE `movies_genres` ADD FOREIGN KEY (`genre_id`) REFERENCES `genres` (`id`) ON DELETE CASCADE;

ALTER TABLE `seats` ADD FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE;

ALTER TABLE `showtimes` ADD FOREIGN KEY (`movie_id`) REFERENCES `movies` (`id`);

ALTER TABLE `showtimes` ADD FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`);
