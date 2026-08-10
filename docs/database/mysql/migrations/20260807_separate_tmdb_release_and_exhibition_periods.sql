-- Tách ngày phát hành gốc từ TMDB khỏi thời gian khai thác tại rạp.
-- Đồng thời bổ sung lịch sử các đợt khai thác và tiến độ nhập phim TMDB.

ALTER TABLE movies
    ADD COLUMN original_release_date DATE NULL AFTER age_rating,
    MODIFY COLUMN release_date DATE NULL;

UPDATE movies
SET original_release_date = release_date
WHERE id > 0
  AND original_release_date IS NULL;

CREATE TABLE movie_exhibition_periods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    movie_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_movie_exhibition_period_movie
        FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT chk_movie_exhibition_period_dates CHECK (
        end_date IS NULL OR end_date >= start_date
    ),
    INDEX idx_movie_exhibition_period_order (movie_id, start_date, id),
    INDEX idx_movie_exhibition_period_deleted_at (deleted_at)
);

INSERT INTO movie_exhibition_periods (
    public_id, movie_id, start_date, end_date, note,
    created_at, updated_at, created_by, updated_by
)
SELECT UUID(), id, release_date, end_date,
       'Đợt khai thác được chuyển đổi từ dữ liệu cũ.',
       created_at, updated_at, created_by, updated_by
FROM movies
WHERE release_date IS NOT NULL
  AND NOT (
      status = 'DRAFT'
      AND tmdb_id IS NOT NULL
      AND release_date < CURRENT_DATE
  );

-- Phim cũ mới nhập từ TMDB chưa từng được khai thác tại rạp của hệ thống.
-- Admin sẽ chủ động lập một đợt khai thác tương lai nếu muốn chiếu lại.
UPDATE movies
SET release_date = NULL,
    end_date = NULL
WHERE id > 0
  AND status = 'DRAFT'
  AND tmdb_id IS NOT NULL
  AND release_date < CURRENT_DATE;

ALTER TABLE tmdb_sync_state
    ADD COLUMN sync_scope VARCHAR(30) NULL AFTER last_sync_time,
    ADD COLUMN release_date_from DATE NULL AFTER sync_scope,
    ADD COLUMN release_date_to DATE NULL AFTER release_date_from,
    ADD COLUMN max_movies INT NULL AFTER release_date_to,
    ADD COLUMN processed_movies INT NOT NULL DEFAULT 0 AFTER max_movies,
    ADD COLUMN imported_movies INT NOT NULL DEFAULT 0 AFTER processed_movies,
    ADD COLUMN skipped_movies INT NOT NULL DEFAULT 0 AFTER imported_movies,
    ADD COLUMN status_message VARCHAR(500) NULL AFTER skipped_movies;
