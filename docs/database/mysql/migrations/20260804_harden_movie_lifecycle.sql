-- Harden Movie lifecycle concurrency and retain an explicit approval/status audit trail.

ALTER TABLE movies
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER status;

CREATE TABLE movie_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    changed_at TIMESTAMP(6) NOT NULL,
    changed_by BIGINT NULL,
    CONSTRAINT fk_movie_status_history_movie
        FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    INDEX idx_movie_status_history_order (movie_id, changed_at, id)
);
