-- Migration: Create booking_food_catalog_items table for persistent food, drink and combo catalog

CREATE TABLE IF NOT EXISTS booking_food_catalog_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Khóa chính tự tăng',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã sản phẩm',
    name VARCHAR(255) NOT NULL COMMENT 'Tên sản phẩm',
    product_type ENUM('FOOD', 'DRINK', 'COMBO') NOT NULL COMMENT 'Loại sản phẩm: Đồ ăn, Nước uống, Combo',
    image_url VARCHAR(500) COMMENT 'Đường dẫn ảnh sản phẩm',
    price DECIMAL(12,2) NOT NULL COMMENT 'Giá bán',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Cờ kích hoạt',
    sellable BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Cho phép bán',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đã xóa mềm',
    disabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Bị vô hiệu hóa',
    currency VARCHAR(10) NOT NULL DEFAULT 'VND' COMMENT 'Loại tiền tệ',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật'
) ENGINE=InnoDB COMMENT='Bảng danh mục đồ ăn, thức uống và combo thực tế (Catalog)';
