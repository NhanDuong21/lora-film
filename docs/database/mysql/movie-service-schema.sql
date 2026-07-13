-- ============================================================
-- Movie Service Production Foundation Schema
-- Database: MySQL 8+
-- Purpose: Cinema Catalog & Showtime Management Service
-- ============================================================

-- Notes:
-- 1. BIGINT id is internal primary key for DB performance.
-- 2. public_id is exposed to public APIs where needed.
-- 3. created_by / updated_by / deleted_by are logical user IDs from Auth/User Service.
-- 4. Do NOT create cross-service foreign keys to user_db.
-- 5. Customer APIs must filter deleted_at IS NULL and only expose public/active data.
-- 6. HELD / BOOKED seat state is NOT owned by Movie Service.

-- ============================================================
-- 1. MOVIE CATALOG
-- ============================================================

CREATE TABLE movies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE COMMENT 'Khóa ngoại giao tiếp API (UUID)',
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    slug VARCHAR(280) NOT NULL,
    active_slug VARCHAR(280) GENERATED ALWAYS AS (
        CASE
            WHEN deleted_at IS NULL THEN slug
            ELSE NULL
        END
    ) STORED COMMENT 'Tự động null khi bị soft delete để tránh lỗi trùng lặp khi tạo phim mới',
    synopsis TEXT,
    duration_minutes INT NOT NULL,
    age_rating VARCHAR(20) NOT NULL COMMENT 'P, K, T13, T16, T18',
    release_date DATE NOT NULL,
    end_date DATE NULL COMMENT 'Null nghĩa là chưa xác định ngày ngừng chiếu',
    country VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, UPCOMING, NOW_SHOWING, ENDED, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT chk_movies_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_movies_dates CHECK (
        end_date IS NULL
        OR end_date >= release_date
    ),
    UNIQUE KEY uk_movies_active_slug (active_slug),
    INDEX idx_movies_status_release_date (status, release_date),
    INDEX idx_movies_public_id (public_id),
    INDEX idx_movies_deleted_at (deleted_at)
);

CREATE TABLE movie_translations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL COMMENT 'Mã ngôn ngữ: vi, en, ko',
    title VARCHAR(255) NOT NULL,
    synopsis TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_movie_translations_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT uk_movie_translation_locale UNIQUE (movie_id, locale) COMMENT 'Mỗi phim chỉ có 1 bản dịch cho 1 ngôn ngữ',
    INDEX idx_movie_translations_movie (movie_id)
);

CREATE TABLE genres (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    active_slug VARCHAR(120) GENERATED ALWAYS AS (
        CASE
            WHEN deleted_at IS NULL THEN slug
            ELSE NULL
        END
    ) STORED,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    UNIQUE KEY uk_genres_active_slug (active_slug),
    INDEX idx_genres_status (status),
    INDEX idx_genres_deleted_at (deleted_at)
);

CREATE TABLE movie_genres (
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_genres_genre FOREIGN KEY (genre_id) REFERENCES genres (id) ON DELETE RESTRICT
);

-- ============================================================
-- 2. PEOPLE / CREDITS / PRODUCTION
-- ============================================================

CREATE TABLE people (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    stage_name VARCHAR(150),
    biography TEXT,
    birth_date DATE NULL,
    nationality VARCHAR(100),
    profile_image_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    INDEX idx_people_full_name (full_name),
    INDEX idx_people_status (status),
    INDEX idx_people_deleted_at (deleted_at)
);

CREATE TABLE movie_credits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    role_type ENUM('DIRECTOR', 'MAIN_ACTOR', 'SUPPORTING_ACTOR', 'VOICE_ACTOR', 'WRITER', 'PRODUCER', 'GUEST') NOT NULL COMMENT 'Vai trò của nhân sự trong phim',
    character_name VARCHAR(150) NULL COMMENT 'Tên nhân vật đóng (dành cho Actor)',
    display_order INT NOT NULL DEFAULT 0 COMMENT 'Dữ liệu Billing Order: Thứ tự ưu tiên hiển thị (số nhỏ xếp trước)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_movie_credits_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_credits_person FOREIGN KEY (person_id) REFERENCES people (id) ON DELETE RESTRICT,
    CONSTRAINT uk_movie_credit_unique UNIQUE (
        movie_id,
        person_id,
        role_type,
        character_name
    ) COMMENT 'Chặn insert trùng vai trò của cùng 1 người trong cùng 1 phim',
    INDEX idx_movie_credits_movie_role (movie_id, role_type),
    INDEX idx_movie_credits_person (person_id),
    INDEX idx_movie_credits_deleted_at (deleted_at)
);

CREATE TABLE production_companies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    country VARCHAR(100),
    logo_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    UNIQUE KEY uk_production_companies_name (name),
    INDEX idx_production_companies_status (status),
    INDEX idx_production_companies_deleted_at (deleted_at)
);

CREATE TABLE movie_production_companies (
    movie_id BIGINT NOT NULL,
    production_company_id BIGINT NOT NULL,
    role ENUM('PRODUCTION', 'DISTRIBUTOR', 'STUDIO') NOT NULL DEFAULT 'PRODUCTION' COMMENT 'Vai trò của công ty (Sản xuất hay Phân phối)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    PRIMARY KEY (
        movie_id,
        production_company_id,
        role
    ),
    CONSTRAINT fk_mpc_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT fk_mpc_company FOREIGN KEY (production_company_id) REFERENCES production_companies (id) ON DELETE RESTRICT
);

-- ============================================================
-- 3. MOVIE VERSIONS / MEDIA
-- ============================================================

CREATE TABLE movie_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    movie_id BIGINT NOT NULL,
    version_name VARCHAR(150) NOT NULL COMMENT 'VD: 2D Vietsub, IMAX Vietsub, 2D Lồng tiếng',
    format VARCHAR(30) NOT NULL COMMENT '2D, 3D, IMAX, 4DX, SCREENX',
    audio_language VARCHAR(50) NOT NULL COMMENT 'EN, VI, JA, KO, ZH',
    subtitle_language VARCHAR(50) NULL COMMENT 'VI, EN, NONE',
    dub_language VARCHAR(50) NULL COMMENT 'VI, NONE',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_movie_versions_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT uk_movie_version_unique UNIQUE (
        movie_id,
        format,
        audio_language,
        subtitle_language,
        dub_language
    ),
    INDEX idx_movie_versions_movie_status (movie_id, status),
    INDEX idx_movie_versions_deleted_at (deleted_at)
);

CREATE TABLE movie_media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    movie_id BIGINT NOT NULL,
    media_type ENUM('POSTER', 'BANNER', 'TRAILER', 'TEASER', 'STILL_IMAGE', 'BEHIND_THE_SCENES') NOT NULL,
    url VARCHAR(500) NOT NULL,
    title VARCHAR(150),
    display_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Chỉ định đây là Poster chính hiển thị ngoài trang chủ',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_movie_media_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    INDEX idx_movie_media_movie_type_status (movie_id, media_type, status),
    INDEX idx_movie_media_deleted_at (deleted_at)
);

-- ============================================================
-- 4. CINEMA
-- ============================================================

CREATE TABLE cinemas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    active_slug VARCHAR(180) GENERATED ALWAYS AS (
        CASE
            WHEN deleted_at IS NULL THEN slug
            ELSE NULL
        END
    ) STORED,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh' COMMENT 'Cực kì quan trọng để backend parse đúng giờ khởi chiếu ở các quốc gia khác nhau',
    hotline VARCHAR(30),
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, ACTIVE, MAINTENANCE, TEMPORARILY_CLOSED, INACTIVE, PERMANENTLY_CLOSED',
    opened_date DATE NULL,
    closed_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT chk_cinemas_closed_date CHECK (
        closed_date IS NULL
        OR opened_date IS NULL
        OR closed_date >= opened_date
    ),
    UNIQUE KEY uk_cinemas_active_slug (active_slug),
    INDEX idx_cinemas_city_district_status (city, district, status),
    INDEX idx_cinemas_status (status),
    INDEX idx_cinemas_public_id (public_id),
    INDEX idx_cinemas_deleted_at (deleted_at)
);

CREATE TABLE cinema_media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    cinema_id BIGINT NOT NULL,
    media_type VARCHAR(30) NOT NULL COMMENT 'LOGO, BANNER, GALLERY, MAP',
    url VARCHAR(500) NOT NULL,
    title VARCHAR(150),
    display_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_cinema_media_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas (id) ON DELETE CASCADE,
    INDEX idx_cinema_media_cinema_type_status (cinema_id, media_type, status),
    INDEX idx_cinema_media_deleted_at (deleted_at)
);

CREATE TABLE cinema_operating_hours (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cinema_id BIGINT NOT NULL,
    day_of_week TINYINT NOT NULL COMMENT '1=Monday, 7=Sunday',
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_operating_hours_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas (id) ON DELETE CASCADE,
    CONSTRAINT uk_cinema_operating_day UNIQUE (cinema_id, day_of_week),
    CONSTRAINT chk_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    INDEX idx_operating_hours_cinema (cinema_id)
);

CREATE TABLE cinema_closure_periods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cinema_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(255) COMMENT 'Lý do đóng cửa rạp đột xuất (Lễ Tết, Cúp điện)',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, CANCELLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_closure_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas (id) ON DELETE CASCADE,
    CONSTRAINT chk_closure_time CHECK (end_time > start_time),
    INDEX idx_closure_cinema_time (
        cinema_id,
        start_time,
        end_time
    ),
    INDEX idx_closure_cinema_status_time (
        cinema_id,
        status,
        start_time,
        end_time
    )
);

-- ============================================================
-- 5. AUDITORIUM / SEAT LAYOUT
-- ============================================================

CREATE TABLE auditoriums (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    cinema_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    screen_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD, IMAX, 4DX, SCREENX',
    sound_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD, DOLBY_ATMOS',
    capacity INT NOT NULL,
    cleaning_buffer_minutes INT NOT NULL DEFAULT 15 COMMENT 'Tạo khoản buffer lúc check trùng lặp suất chiếu để nhân viên dọn rạp',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, ACTIVE, MAINTENANCE, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_auditoriums_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas (id) ON DELETE RESTRICT,
    CONSTRAINT uk_auditoriums_cinema_name UNIQUE (cinema_id, name),
    CONSTRAINT chk_auditorium_capacity CHECK (capacity > 0),
    CONSTRAINT chk_auditorium_cleaning_buffer CHECK (cleaning_buffer_minutes >= 0),
    INDEX idx_auditoriums_cinema_status (cinema_id, status),
    INDEX idx_auditoriums_deleted_at (deleted_at)
);

CREATE TABLE auditorium_maintenance_windows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auditorium_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(255) COMMENT 'Bảo trì riêng cho 1 phòng',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, CANCELLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_maintenance_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums (id) ON DELETE CASCADE,
    CONSTRAINT chk_maintenance_time CHECK (end_time > start_time),
    INDEX idx_maintenance_auditorium_time (
        auditorium_id,
        start_time,
        end_time
    ),
    INDEX idx_maintenance_auditorium_status_time (
        auditorium_id,
        status,
        start_time,
        end_time
    )
);

CREATE TABLE seat_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    code VARCHAR(30) NOT NULL UNIQUE COMMENT 'STANDARD, VIP, COUPLE, DISABLED',
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    INDEX idx_seat_types_status (status),
    INDEX idx_seat_types_deleted_at (deleted_at)
);

CREATE TABLE seats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    auditorium_id BIGINT NOT NULL,
    seat_type_id BIGINT NOT NULL,
    row_label VARCHAR(5) NOT NULL COMMENT 'Tên hàng, VD: A, B, C',
    seat_number INT NOT NULL COMMENT 'Số thứ tự, VD: 1, 2, 3',
    seat_code VARCHAR(10) NOT NULL COMMENT 'Mã in trên vé: VD: H8, K9',
    position_row INT NOT NULL COMMENT 'Tọa độ lưới trục X để FE vẽ sơ đồ',
    position_column INT NOT NULL COMMENT 'Tọa độ lưới trục Y để FE vẽ sơ đồ',
    pair_group VARCHAR(30) NULL COMMENT 'Dùng gom nhóm vé (VD ghế couple bắt buộc mua 1 cặp)',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, MAINTENANCE (Chỉ bảo trì khi ghế gãy hỏng vật lý)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_seats_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums (id) ON DELETE RESTRICT,
    CONSTRAINT fk_seats_type FOREIGN KEY (seat_type_id) REFERENCES seat_types (id) ON DELETE RESTRICT,
    CONSTRAINT uk_seats_auditorium_code UNIQUE (auditorium_id, seat_code),
    CONSTRAINT uk_seats_auditorium_position UNIQUE (
        auditorium_id,
        position_row,
        position_column
    ),
    CONSTRAINT chk_seats_number CHECK (seat_number > 0),
    CONSTRAINT chk_seats_position CHECK (
        position_row > 0
        AND position_column > 0
    ),
    INDEX idx_seats_auditorium_status (auditorium_id, status),
    INDEX idx_seats_auditorium_type (auditorium_id, seat_type_id),
    INDEX idx_seats_deleted_at (deleted_at)
);

-- ============================================================
-- 6. SHOWTIME / PRICING
-- ============================================================

CREATE TABLE showtimes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    movie_id BIGINT NOT NULL,
    movie_version_id BIGINT NOT NULL,
    cinema_id BIGINT NOT NULL,
    auditorium_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    booking_open_time TIMESTAMP NULL,
    booking_close_time TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, OPEN_FOR_BOOKING, CLOSED, CANCELLED, FINISHED',
    cancellation_reason VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_showtimes_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE RESTRICT,
    CONSTRAINT fk_showtimes_movie_version FOREIGN KEY (movie_version_id) REFERENCES movie_versions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_showtimes_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_showtimes_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums (id) ON DELETE RESTRICT,
    CONSTRAINT chk_showtimes_time CHECK (end_time > start_time),
    CONSTRAINT chk_showtimes_booking_window CHECK (
        booking_open_time IS NULL
        OR booking_close_time IS NULL
        OR booking_close_time > booking_open_time
    ),
    INDEX idx_showtimes_movie_start (movie_id, start_time),
    INDEX idx_showtimes_version_start (movie_version_id, start_time),
    INDEX idx_showtimes_cinema_start (cinema_id, start_time),
    INDEX idx_showtimes_auditorium_time (
        auditorium_id,
        start_time,
        end_time
    ),
    INDEX idx_showtimes_status_start (status, start_time),
    INDEX idx_showtimes_movie_cinema_start (
        movie_id,
        cinema_id,
        start_time
    ),
    INDEX idx_showtimes_public_id (public_id),
    INDEX idx_showtimes_deleted_at (deleted_at)
);

CREATE TABLE showtime_prices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    showtime_id BIGINT NOT NULL,
    seat_type_id BIGINT NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_showtime_prices_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes (id) ON DELETE CASCADE,
    CONSTRAINT fk_showtime_prices_seat_type FOREIGN KEY (seat_type_id) REFERENCES seat_types (id) ON DELETE RESTRICT,
    CONSTRAINT uk_showtime_prices_type UNIQUE (showtime_id, seat_type_id) COMMENT 'Mỗi suất chiếu chỉ có 1 mức giá chốt cho 1 loại ghế',
    CONSTRAINT chk_showtime_prices_amount CHECK (price >= 0),
    INDEX idx_showtime_prices_showtime (showtime_id)
);

CREATE TABLE showtime_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    showtime_id BIGINT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by BIGINT NULL,
    CONSTRAINT fk_showtime_status_history_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes (id) ON DELETE CASCADE,
    INDEX idx_showtime_status_history_showtime (showtime_id, changed_at)
);

CREATE TABLE showtime_blocked_seats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    showtime_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    reason VARCHAR(255) COMMENT 'Lý do khóa nội bộ: Ghế khách VIP, ghế ướt chưa lau',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, CANCELLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_blocked_seats_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes (id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked_seats_seat FOREIGN KEY (seat_id) REFERENCES seats (id) ON DELETE RESTRICT,
    CONSTRAINT uk_showtime_blocked_seat UNIQUE (showtime_id, seat_id),
    INDEX idx_blocked_seats_showtime_status (showtime_id, status)
);


-- ============================================================
-- 6.1. AUTO SHOWTIME SCHEDULING PREVIEW
-- ============================================================

CREATE TABLE showtime_schedule_previews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    public_id CHAR(36) NOT NULL UNIQUE
        COMMENT 'UUID dùng để expose preview qua Admin API',

    cinema_id BIGINT NOT NULL
        COMMENT 'Mỗi preview v1 chỉ thuộc một cinema',

    schedule_from DATE NOT NULL,
    schedule_to DATE NOT NULL,

    timezone_snapshot VARCHAR(50) NOT NULL
        COMMENT 'Snapshot timezone của cinema tại thời điểm generate preview',

    strategy VARCHAR(30) NOT NULL
        COMMENT 'BALANCED',

    strategy_version VARCHAR(30) NOT NULL DEFAULT 'BALANCED_V1'
        COMMENT 'Version thuật toán scoring để audit và tái hiện kết quả',

    apply_mode VARCHAR(30) NOT NULL DEFAULT 'ALL_OR_NOTHING'
        COMMENT 'Version đầu chỉ hỗ trợ ALL_OR_NOTHING',

    status VARCHAR(30) NOT NULL DEFAULT 'GENERATING'
        COMMENT 'GENERATING, PREVIEWED, APPLYING, APPLIED, EXPIRED, FAILED, CANCELLED',

    slot_granularity_minutes INT NOT NULL DEFAULT 15
        COMMENT 'Khoảng bước sinh candidate, ví dụ mỗi 15 phút',

    total_candidate_count INT NOT NULL DEFAULT 0,
    valid_candidate_count INT NOT NULL DEFAULT 0,
    rejected_candidate_count INT NOT NULL DEFAULT 0,
    selected_candidate_count INT NOT NULL DEFAULT 0,

    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,

    generated_by BIGINT NOT NULL
        COMMENT 'Logical account ID từ Auth/User Service, không tạo cross-service FK',

    applied_at TIMESTAMP NULL,
    applied_by BIGINT NULL
        COMMENT 'Logical account ID của Admin thực hiện apply',

    generate_idempotency_key VARCHAR(100) NOT NULL,
    apply_idempotency_key VARCHAR(100) NULL,

    request_fingerprint CHAR(64) NOT NULL
        COMMENT 'SHA-256 của normalized generate request',

    failure_reason VARCHAR(500) NULL,

    version BIGINT NOT NULL DEFAULT 0
        COMMENT 'Optimistic locking version',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_schedule_preview_cinema
        FOREIGN KEY (cinema_id)
        REFERENCES cinemas(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_schedule_preview_generate_idempotency
        UNIQUE (generate_idempotency_key),

    CONSTRAINT uk_schedule_preview_apply_idempotency
        UNIQUE (apply_idempotency_key),

    CONSTRAINT chk_schedule_preview_dates
        CHECK (schedule_to >= schedule_from),

    CONSTRAINT chk_schedule_preview_expiry
        CHECK (expires_at > generated_at),

    CONSTRAINT chk_schedule_preview_slot_granularity
        CHECK (slot_granularity_minutes > 0),

    CONSTRAINT chk_schedule_preview_counts
        CHECK (
            total_candidate_count >= 0
            AND valid_candidate_count >= 0
            AND rejected_candidate_count >= 0
            AND selected_candidate_count >= 0
            AND valid_candidate_count + rejected_candidate_count
                = total_candidate_count
            AND selected_candidate_count <= valid_candidate_count
        ),

    CONSTRAINT chk_schedule_preview_strategy
        CHECK (
            strategy IN ('BALANCED')
        ),

    CONSTRAINT chk_schedule_preview_apply_mode
        CHECK (
            apply_mode IN ('ALL_OR_NOTHING')
        ),

    CONSTRAINT chk_schedule_preview_status
        CHECK (
            status IN (
                'GENERATING',
                'PREVIEWED',
                'APPLYING',
                'APPLIED',
                'EXPIRED',
                'FAILED',
                'CANCELLED'
            )
        ),

    INDEX idx_schedule_preview_status_expiry (
        status,
        expires_at
    ),

    INDEX idx_schedule_preview_cinema_date (
        cinema_id,
        schedule_from,
        schedule_to
    ),

    INDEX idx_schedule_preview_generated_by_time (
        generated_by,
        generated_at
    ),

    INDEX idx_schedule_preview_created_at (
        created_at
    )
);


CREATE TABLE showtime_schedule_preview_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    public_id CHAR(36) NOT NULL UNIQUE
        COMMENT 'UUID dùng để thao tác item qua Admin API',

    preview_id BIGINT NOT NULL,

    movie_id BIGINT NOT NULL,
    movie_version_id BIGINT NOT NULL,
    cinema_id BIGINT NOT NULL,
    auditorium_id BIGINT NOT NULL,

    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,

    occupancy_end_time TIMESTAMP NOT NULL
        COMMENT 'end_time cộng cleaning buffer tại thời điểm generate',

    score DECIMAL(10, 3) NOT NULL DEFAULT 0,

    score_breakdown_json JSON NULL
        COMMENT 'Chi tiết các thành phần score để audit và explainability',

    ranking_position INT NOT NULL DEFAULT 0,

    validation_status VARCHAR(30) NOT NULL
        COMMENT 'VALID, REJECTED',

    rejection_code VARCHAR(100) NULL,
    rejection_reason VARCHAR(500) NULL,

    selected BOOLEAN NOT NULL DEFAULT FALSE,

    selected_at TIMESTAMP NULL,
    selected_by BIGINT NULL
        COMMENT 'Logical account ID của Admin thay đổi selection',

    apply_status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING, CREATED, CONFLICT, FAILED, SKIPPED',

    created_showtime_id BIGINT NULL
        COMMENT 'Showtime thật được tạo sau khi apply thành công',

    apply_error_code VARCHAR(100) NULL,
    apply_error_message VARCHAR(500) NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_schedule_preview_item_preview
        FOREIGN KEY (preview_id)
        REFERENCES showtime_schedule_previews(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_schedule_preview_item_movie
        FOREIGN KEY (movie_id)
        REFERENCES movies(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_schedule_preview_item_movie_version
        FOREIGN KEY (movie_version_id)
        REFERENCES movie_versions(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_schedule_preview_item_cinema
        FOREIGN KEY (cinema_id)
        REFERENCES cinemas(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_schedule_preview_item_auditorium
        FOREIGN KEY (auditorium_id)
        REFERENCES auditoriums(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_schedule_preview_item_created_showtime
        FOREIGN KEY (created_showtime_id)
        REFERENCES showtimes(id)
        ON DELETE SET NULL,

    CONSTRAINT uk_schedule_preview_item_slot
        UNIQUE (
            preview_id,
            auditorium_id,
            start_time
        ),

    CONSTRAINT chk_schedule_preview_item_time
        CHECK (
            end_time > start_time
            AND occupancy_end_time >= end_time
        ),

    CONSTRAINT chk_schedule_preview_item_score
        CHECK (score >= 0),

    CONSTRAINT chk_schedule_preview_item_ranking
        CHECK (ranking_position >= 0),

    CONSTRAINT chk_schedule_preview_item_validation_status
        CHECK (
            validation_status IN (
                'VALID',
                'REJECTED'
            )
        ),

    CONSTRAINT chk_schedule_preview_item_apply_status
        CHECK (
            apply_status IN (
                'PENDING',
                'CREATED',
                'CONFLICT',
                'FAILED',
                'SKIPPED'
            )
        ),

    CONSTRAINT chk_schedule_preview_item_selection
        CHECK (
            selected = FALSE
            OR validation_status = 'VALID'
        ),

    INDEX idx_schedule_preview_items_preview_rank (
        preview_id,
        ranking_position,
        id
    ),

    INDEX idx_schedule_preview_items_preview_selected (
        preview_id,
        selected,
        validation_status
    ),

    INDEX idx_schedule_preview_items_apply_status (
        preview_id,
        apply_status
    ),

    INDEX idx_schedule_preview_items_auditorium_time (
        auditorium_id,
        start_time,
        occupancy_end_time
    ),

    INDEX idx_schedule_preview_items_movie_version (
        preview_id,
        movie_version_id
    ),

    INDEX idx_schedule_preview_items_created_showtime (
        created_showtime_id
    )
);