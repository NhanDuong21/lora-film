-- ============================================================
-- DỊCH VỤ ĐẶT VÉ (BOOKING SERVICE) - CƠ SỞ DỮ LIỆU ĐÃ TỐI ƯU
-- PHẦN 1
-- BẢNG: bookings
-- Phiên bản MySQL 8+
-- ============================================================
CREATE TABLE bookings (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ trong database',
  public_id BINARY(16) NOT NULL COMMENT 'UUID dùng cho public (Khuyến khích loại sắp xếp theo thời gian)',
  booking_code VARCHAR(50) NOT NULL COMMENT 'Mã đặt vé hiển thị cho người dùng',
  user_id BIGINT NOT NULL COMMENT 'Liên kết logic sang Dịch vụ Người dùng (User Service)',
  showtime_id BIGINT NOT NULL COMMENT 'Liên kết logic sang Dịch vụ Phim (Movie Service)',
  total_amount DECIMAL(12, 2) NOT NULL COMMENT 'Tổng số tiền đặt vé',
  currency VARCHAR(10) NOT NULL DEFAULT 'VND' COMMENT 'Loại tiền tệ thanh toán',
  ticket_count INT NOT NULL COMMENT 'Tổng số lượng vé đặt',
  status ENUM(
    'PENDING_PAYMENT',
    'CONFIRMED',
    'CANCELLED',
    'EXPIRED',
    'REFUND_PENDING',
    'REFUNDED'
  ) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT 'Trạng thái vòng đời của đơn đặt vé',
  booking_source ENUM(
    'WEB',
    'MOBILE',
    'ADMIN',
    'POS',
    'API'
  ) NOT NULL DEFAULT 'WEB' COMMENT 'Nguồn thực hiện đặt vé',
  payment_deadline DATETIME NOT NULL COMMENT 'Hạn chót để hoàn tất thanh toán',
  confirmed_at DATETIME NULL COMMENT 'Thời điểm xác nhận đặt vé thành công',
  cancelled_at DATETIME NULL COMMENT 'Thời điểm hủy đặt vé',
  expired_at DATETIME NULL COMMENT 'Thời điểm hết hạn đặt vé do quá giờ thanh toán',
  refunded_at DATETIME NULL COMMENT 'Thời điểm hoàn tiền thành công',
  cancel_reason VARCHAR(255) NULL COMMENT 'Lý do hủy đặt vé',
  note VARCHAR(500) NULL COMMENT 'Ghi chú nội bộ',
  version INT NOT NULL DEFAULT 0 COMMENT 'Phiên bản phục vụ cơ chế khóa lạc quan (Optimistic locking)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo bản ghi',
  created_by BIGINT NULL COMMENT 'Mã Người dùng/Admin/Hệ thống đã tạo đơn đặt vé này',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật bản ghi gần nhất',
  updated_by BIGINT NULL COMMENT 'Mã Người dùng/Admin/Hệ thống đã cập nhật đơn đặt vé này',
  deleted_at DATETIME NULL COMMENT 'Thời điểm xóa mềm bản ghi',
  deleted_by BIGINT NULL COMMENT 'Mã Người dùng/Admin/Hệ thống đã xóa mềm đơn đặt vé này',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Bảng gốc chứa thông tin tổng hợp của đơn đặt vé (Booking aggregate root)';

-- ============================================================
-- BẢNG: booking_tickets
-- ============================================================
CREATE TABLE booking_tickets (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của vé',
  booking_id BIGINT NOT NULL COMMENT 'Liên kết tới bảng bookings',
  seat_id BIGINT NOT NULL COMMENT 'Liên kết logic sang Dịch vụ Phim (Movie Service)',
  seat_label VARCHAR(20) NOT NULL COMMENT 'Bản chụp thông tin nhãn ghế (Ví dụ: A1, B2)',
  seat_type VARCHAR(30) NOT NULL COMMENT 'Bản chụp thông tin loại ghế (Ví dụ: VIP, SWEETBOX)',
  ticket_price DECIMAL(12, 2) NOT NULL COMMENT 'Bản chụp giá vé tại thời điểm đặt',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo bản ghi',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Bản chụp thông tin chi tiết của vé (Dữ liệu bất biến sau khi tạo)';

-- ============================================================
-- PHẦN 2
-- BẢNG: seat_reservations
-- BẢNG: booking_status_histories
-- ============================================================
CREATE TABLE seat_reservations (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của phiên giữ ghế',
  public_id BINARY(16) NOT NULL COMMENT 'UUID công khai dùng cho phiên giữ ghế',
  booking_id BIGINT NULL COMMENT 'Liên kết tới bảng bookings sau khi phiên giữ ghế chuyển đổi thành đơn đặt vé thành công',
  showtime_id BIGINT NOT NULL COMMENT 'Liên kết logic sang Dịch vụ Phim (Movie Service)',
  seat_id BIGINT NOT NULL COMMENT 'Liên kết logic sang Dịch vụ Phim (Movie Service)',
  user_id BIGINT NOT NULL COMMENT 'Liên kết logic sang Dịch vụ Người dùng (User Service)',
  status ENUM(
    'HELD',
    'CONVERTED',
    'RELEASED',
    'EXPIRED'
  ) NOT NULL DEFAULT 'HELD' COMMENT 'Trạng thái của phiên giữ ghế tạm thời',
  reservation_token BINARY(16) NOT NULL COMMENT 'Mã token độc nhất xác thực cho phiên giữ ghế',
  expires_at DATETIME NOT NULL COMMENT 'Thời điểm hết hạn giữ ghế',
  converted_at DATETIME NULL COMMENT 'Thời điểm chuyển đổi thành công sang đơn đặt vé',
  released_at DATETIME NULL COMMENT 'Thời điểm giải phóng ghế',
  expired_at DATETIME NULL COMMENT 'Thời điểm phiên giữ ghế hết hiệu lực',
  release_reason VARCHAR(255) NULL COMMENT 'Lý do giải phóng ghế',
  version INT NOT NULL DEFAULT 0 COMMENT 'Phiên bản phục vụ cơ chế khóa lạc quan (Optimistic locking)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo bản ghi',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Bảng giữ ghế tạm thời cho khách hàng trong lúc chờ xử lý thanh toán';

CREATE TABLE booking_status_histories (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của bản ghi lịch sử',
  booking_id BIGINT NOT NULL COMMENT 'Liên kết tới bảng bookings',
  previous_status ENUM(
    'PENDING_PAYMENT',
    'CONFIRMED',
    'CANCELLED',
    'EXPIRED',
    'REFUND_PENDING',
    'REFUNDED'
  ) NULL COMMENT 'Trạng thái cũ của đơn đặt vé trước khi thay đổi',
  current_status ENUM(
    'PENDING_PAYMENT',
    'CONFIRMED',
    'CANCELLED',
    'EXPIRED',
    'REFUND_PENDING',
    'REFUNDED'
  ) NOT NULL COMMENT 'Trạng thái hiện tại của đơn đặt vé sau khi thay đổi',
  changed_by BIGINT NULL COMMENT 'Mã Người dùng/Admin/Hệ thống đã thực hiện thay đổi trạng thái',
  change_type ENUM(
    'USER_ACTION',
    'SYSTEM',
    'PAYMENT_EVENT',
    'ADMIN_ACTION',
    'SCHEDULER'
  ) NOT NULL DEFAULT 'SYSTEM' COMMENT 'Nguồn gốc gây ra sự thay đổi trạng thái',
  reason VARCHAR(500) NULL COMMENT 'Lý do thay đổi trạng thái',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm ghi nhận thay đổi trạng thái',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Lịch sử theo dõi các bước chuyển đổi trạng thái của đơn đặt vé';

-- ============================================================
-- PHẦN 3
-- BẢNG: booking_payment_events
-- BẢNG: booking_outbox_events
-- ============================================================
CREATE TABLE booking_payment_events (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của sự kiện thanh toán',
  event_id BINARY(16) NOT NULL COMMENT 'Mã định danh độc nhất của sự kiện thanh toán',
  booking_id BIGINT NOT NULL COMMENT 'Liên kết tới bảng bookings',
  payment_id BIGINT NOT NULL COMMENT 'Liên kết logic sang Dịch vụ Thanh toán (Payment Service)',
  payment_transaction_code VARCHAR(100) NOT NULL COMMENT 'Mã giao dịch nội bộ của hệ thống thanh toán',
  external_transaction_id VARCHAR(255) NULL COMMENT 'Mã giao dịch trả về từ cổng thanh toán đối tác (Gateway)',
  payment_method ENUM(
    'VNPAY',
    'MOMO',
    'ZALOPAY',
    'BANK_TRANSFER',
    'CREDIT_CARD',
    'CASH'
  ) NOT NULL COMMENT 'Phương thức thanh toán sử dụng',
  payment_status ENUM(
    'PENDING',
    'SUCCESS',
    'FAILED',
    'CANCELLED',
    'REFUNDED'
  ) NOT NULL COMMENT 'Kết quả xử lý giao dịch thanh toán',
  amount DECIMAL(12, 2) NOT NULL COMMENT 'Số tiền thanh toán',
  currency VARCHAR(10) NOT NULL DEFAULT 'VND' COMMENT 'Loại tiền tệ thanh toán',
  gateway_response_code VARCHAR(50) NULL COMMENT 'Mã phản hồi từ cổng thanh toán đối tác',
  gateway_message VARCHAR(500) NULL COMMENT 'Thông báo phản hồi từ cổng thanh toán đối tác',
  processed BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đánh dấu Dịch vụ Đặt vé đã xử lý sự kiện thanh toán này hay chưa',
  processed_at DATETIME NULL COMMENT 'Thời điểm Dịch vụ Đặt vé hoàn tất xử lý sự kiện',
  retry_count INT NOT NULL DEFAULT 0 COMMENT 'Số lần thử lại khi gặp lỗi xử lý sự kiện',
  error_message TEXT NULL COMMENT 'Nội dung thông báo lỗi khi xử lý sự kiện thất bại',
  occurred_at DATETIME NOT NULL COMMENT 'Thời điểm sự kiện thanh toán thực tế xảy ra',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm nhận và tạo bản ghi sự kiện vào database',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Các sự kiện thanh toán được tiếp nhận từ Dịch vụ Thanh toán';

CREATE TABLE booking_outbox_events (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của sự kiện trong hàng đợi outbox',
  event_id BINARY(16) NOT NULL COMMENT 'Mã định danh độc nhất của sự kiện',
  aggregate_type VARCHAR(100) NOT NULL COMMENT 'Loại đối tượng tổng hợp phát sinh sự kiện (Ví dụ: Booking)',
  aggregate_id BIGINT NOT NULL COMMENT 'Mã định danh của đối tượng tổng hợp phát sinh sự kiện',
  event_type VARCHAR(100) NOT NULL COMMENT 'Tên sự kiện nghiệp vụ (Ví dụ: BookingConfirmed)',
  event_version INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản cấu trúc của sự kiện',
  destination_service VARCHAR(100) NULL COMMENT 'Dịch vụ đích nhận sự kiện nếu gửi trực tiếp (Point-to-point)',
  payload JSON NOT NULL COMMENT 'Nội dung chi tiết của sự kiện đã được mã hóa chuỗi JSON',
  headers JSON NULL COMMENT 'Thông tin siêu dữ liệu (Metadata) đính kèm của sự kiện',
  status ENUM(
    'NEW',
    'PUBLISHING',
    'PUBLISHED',
    'FAILED'
  ) NOT NULL DEFAULT 'NEW' COMMENT 'Trạng thái phát bản tin sự kiện lên hệ thống Message Broker',
  retry_count INT NOT NULL DEFAULT 0 COMMENT 'Số lần thử lại khi phát bản tin sự kiện thất bại',
  next_retry_at DATETIME NULL COMMENT 'Thời điểm dự kiến cho lượt thử lại tiếp theo',
  published_at DATETIME NULL COMMENT 'Thời điểm phát bản tin sự kiện thành công lên Message Broker',
  last_error TEXT NULL COMMENT 'Chi tiết lỗi của lần phát bản tin thất bại gần nhất',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo bản ghi sự kiện vào hàng đợi outbox',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Mô hình Transactional Outbox lưu trữ các sự kiện chờ phát đi để đảm bảo tính nhất quán dữ liệu';

-- ============================================================
-- PHẦN 4
-- BẢNG: booking_inbox_events
-- BẢNG: booking_idempotency_keys
-- ============================================================
CREATE TABLE booking_inbox_events (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của sự kiện trong hàng đợi inbox',
  event_id BINARY(16) NOT NULL COMMENT 'Mã định danh độc nhất của sự kiện lấy từ dịch vụ gửi',
  source_service VARCHAR(100) NOT NULL COMMENT 'Tên microservice phát sinh và gửi sự kiện này',
  aggregate_type VARCHAR(100) NOT NULL COMMENT 'Loại đối tượng tổng hợp phát sinh sự kiện ở dịch vụ gốc',
  aggregate_id VARCHAR(100) NOT NULL COMMENT 'Mã định danh của đối tượng tổng hợp ở dịch vụ gốc',
  event_type VARCHAR(100) NOT NULL COMMENT 'Tên sự kiện nghiệp vụ nhận được',
  event_version INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản cấu trúc của sự kiện nhận được',
  payload JSON NOT NULL COMMENT 'Nội dung chi tiết của sự kiện đã được mã hóa chuỗi JSON',
  headers JSON NULL COMMENT 'Thông tin siêu dữ liệu (Metadata) đính kèm của sự kiện',
  status ENUM(
    'RECEIVED',
    'PROCESSING',
    'PROCESSED',
    'FAILED',
    'IGNORED'
  ) NOT NULL DEFAULT 'RECEIVED' COMMENT 'Trạng thái xử lý sự kiện đầu vào của Dịch vụ Đặt vé',
  retry_count INT NOT NULL DEFAULT 0 COMMENT 'Số lần thử lại khi xử lý sự kiện thất bại',
  processed_at DATETIME NULL COMMENT 'Thời điểm xử lý xong sự kiện thành công',
  last_error TEXT NULL COMMENT 'Chi tiết lỗi của lần xử lý thất bại gần nhất',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tiếp nhận và lưu sự kiện vào hàng đợi inbox',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Mô hình Inbox Pattern nhằm đảm bảo tính idempotency (chống trùng lặp) khi tiêu thụ các sự kiện bên ngoài';

CREATE TABLE booking_idempotency_keys (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của bản ghi kiểm tra trùng lặp',
  idempotency_key VARCHAR(255) NOT NULL COMMENT 'Mã định danh chống trùng lặp do phía Client cung cấp',
  request_hash BINARY(32) NOT NULL COMMENT 'Mã băm nhị phân SHA-256 của toàn bộ dữ liệu yêu cầu gửi lên',
  user_id BIGINT NOT NULL COMMENT 'Liên kết logic sang Dịch vụ Người dùng (User Service)',
  endpoint VARCHAR(255) NOT NULL COMMENT 'Đường dẫn API xử lý yêu cầu',
  http_method VARCHAR(10) NOT NULL COMMENT 'Phương thức HTTP xử lý yêu cầu (Ví dụ: POST, PUT)',
  booking_id BIGINT NULL COMMENT 'Mã đơn đặt vé được tạo ra từ yêu cầu này (nếu có)',
  response_status SMALLINT NULL COMMENT 'Mã trạng thái HTTP phản hồi lưu lại (Ví dụ: 200, 201)',
  response_body JSON NULL COMMENT 'Nội dung phản hồi lưu lại dưới dạng JSON để trả về cho Client khi bị trùng yêu cầu',
  status ENUM(
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'EXPIRED'
  ) NOT NULL DEFAULT 'PROCESSING' COMMENT 'Trạng thái xử lý của mã chống trùng lặp',
  expires_at DATETIME NOT NULL COMMENT 'Thời điểm mã chống trùng lặp hết hiệu lực',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tiếp nhận yêu cầu đầu tiên',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Lưu trữ mã chống trùng lặp (Idempotency Key) nhằm ngăn chặn Client gửi trùng lặp yêu cầu đặt vé';

-- ============================================================
-- PHẦN 5
-- BẢNG: booking_retry_tasks
-- BẢNG: booking_reconciliation_tasks
-- ============================================================
CREATE TABLE booking_retry_tasks (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của tác vụ thử lại',
  public_id BINARY(16) NOT NULL COMMENT 'UUID công khai của tác vụ thử lại',
  task_type ENUM(
    'OUTBOX_PUBLISH',
    'PAYMENT_EVENT_PROCESS',
    'INBOX_EVENT_PROCESS',
    'BOOKING_EXPIRE',
    'SEAT_RELEASE',
    'RECONCILIATION'
  ) NOT NULL COMMENT 'Loại tác vụ cần thực hiện thử lại',
  reference_type ENUM(
    'BOOKING',
    'OUTBOX_EVENT',
    'INBOX_EVENT',
    'PAYMENT_EVENT',
    'SEAT_RESERVATION',
    'RECONCILIATION_TASK'
  ) NOT NULL COMMENT 'Loại đối tượng nghiệp vụ được tham chiếu để xử lý',
  reference_id BIGINT NOT NULL COMMENT 'Mã định danh của đối tượng nghiệp vụ được tham chiếu',
  payload JSON NULL COMMENT 'Dữ liệu bổ sung đi kèm để phục vụ quá trình xử lý lại',
  priority ENUM(
    'LOW',
    'NORMAL',
    'HIGH',
    'CRITICAL'
  ) NOT NULL DEFAULT 'NORMAL' COMMENT 'Mức độ ưu tiên xử lý của tác vụ',
  status ENUM(
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'DEAD'
  ) NOT NULL DEFAULT 'PENDING' COMMENT 'Trạng thái xử lý của tác vụ thử lại',
  retry_count INT NOT NULL DEFAULT 0 COMMENT 'Số lần đã thực hiện chạy lại tác vụ',
  max_retry_count INT NOT NULL DEFAULT 10 COMMENT 'Số lần chạy lại tối đa được phép',
  next_retry_at DATETIME NOT NULL COMMENT 'Thời điểm dự kiến thực hiện lượt chạy lại tiếp theo',
  last_retry_at DATETIME NULL COMMENT 'Thời điểm thực hiện lượt chạy lại gần nhất',
  completed_at DATETIME NULL COMMENT 'Thời điểm hoàn thành tác vụ thành công',
  last_error TEXT NULL COMMENT 'Chi tiết thông báo lỗi của lần chạy thất bại gần nhất',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm khởi tạo tác vụ xử lý lại',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Quản lý lịch trình và trạng thái các tác vụ chạy lại (Retry Scheduler)';

CREATE TABLE booking_reconciliation_tasks (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của tác vụ đối soát',
  public_id BINARY(16) NOT NULL COMMENT 'UUID công khai của tác vụ đối soát',
  booking_id BIGINT NOT NULL COMMENT 'Liên kết tới bảng bookings',
  reconciliation_type ENUM(
    'PAYMENT',
    'BOOKING_STATUS',
    'SEAT',
    'TICKET',
    'OUTBOX',
    'INBOX'
  ) NOT NULL COMMENT 'Danh mục/Phạm vi cần thực hiện đối soát dữ liệu',
  expected_value JSON NULL COMMENT 'Trạng thái dữ liệu chuẩn theo logic nghiệp vụ mong đợi',
  actual_value JSON NULL COMMENT 'Trạng thái dữ liệu thực tế ghi nhận được trong hệ thống',
  status ENUM(
    'PENDING',
    'PROCESSING',
    'RESOLVED',
    'FAILED',
    'IGNORED'
  ) NOT NULL DEFAULT 'PENDING' COMMENT 'Trạng thái xử lý của tác vụ đối soát sai lệch dữ liệu',
  resolution_type ENUM('AUTO', 'MANUAL') NULL COMMENT 'Chiến lược xử lý sai lệch (Tự động hoặc Thủ công)',
  resolution_note VARCHAR(1000) NULL COMMENT 'Thông tin chi tiết về phương án xử lý sai lệch',
  resolved_at DATETIME NULL COMMENT 'Thời điểm xử lý xong sai lệch dữ liệu',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm phát hiện sai lệch và tạo tác vụ đối soát',
  created_by BIGINT NULL COMMENT 'Mã Người dùng/Admin/Hệ thống đã tạo tác vụ đối soát',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật tác vụ đối soát gần nhất',
  updated_by BIGINT NULL COMMENT 'Mã Admin chịu trách nhiệm xử lý tác vụ đối soát này',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Quản lý các tác vụ xử lý và đối soát dữ liệu sai lệch giữa các microservices';

-- ============================================================
-- PHẦN 6
-- BẢNG: booking_dead_letter_events
-- BẢNG: booking_operation_logs (Bảng Log đã phân vùng - Partitioned)
-- ============================================================
CREATE TABLE booking_dead_letter_events (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của sự kiện lỗi nghiêm trọng',
  public_id BINARY(16) NOT NULL COMMENT 'UUID công khai của sự kiện trong hàng đợi lỗi (DLQ)',
  event_id BINARY(16) NOT NULL COMMENT 'Mã định danh gốc của sự kiện bị lỗi',
  source_service VARCHAR(100) NOT NULL COMMENT 'Tên microservice nguồn phát sinh sự kiện',
  aggregate_type VARCHAR(100) NOT NULL COMMENT 'Loại đối tượng tổng hợp ở dịch vụ gốc',
  aggregate_id VARCHAR(100) NOT NULL COMMENT 'Mã định danh của đối tượng tổng hợp ở dịch vụ gốc',
  event_type VARCHAR(100) NOT NULL COMMENT 'Tên sự kiện nghiệp vụ gốc',
  event_version INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản cấu trúc dữ liệu của sự kiện gốc',
  payload JSON NOT NULL COMMENT 'Nội dung chi tiết của sự kiện gốc bị lỗi',
  headers JSON NULL COMMENT 'Thông tin siêu dữ liệu (Metadata) đi kèm của sự kiện gốc',
  error_code VARCHAR(100) NULL COMMENT 'Mã lỗi ứng dụng ghi nhận lúc xử lý thất bại',
  error_message TEXT NOT NULL COMMENT 'Nội dung thông báo lỗi chi tiết khi xử lý thất bại liên tục',
  stack_trace LONGTEXT NULL COMMENT 'Chi tiết vết mã nguồn (Stack trace) bắt được lúc lỗi xảy ra',
  retry_count INT NOT NULL DEFAULT 0 COMMENT 'Số lần đã cố gắng xử lý lại từ hàng đợi lỗi',
  status ENUM(
    'OPEN',
    'RETRYING',
    'RESOLVED',
    'DISCARDED'
  ) NOT NULL DEFAULT 'OPEN' COMMENT 'Trạng thái xử lý sự kiện trong hàng đợi lỗi Dead Letter Queue',
  resolved_at DATETIME NULL COMMENT 'Thời điểm Admin xử lý xong sự kiện lỗi này',
  resolved_by BIGINT NULL COMMENT 'Mã Admin đã thực hiện xử lý sự kiện lỗi',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm chuyển sự kiện lỗi vào hàng đợi DLQ',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Hàng đợi chứa các sự kiện lỗi nghiêm trọng không thể tự động xử lý lại (Dead Letter Queue)';

-- Tối ưu hóa: Phân vùng (Partition) bảng log dựa theo thời gian để tối ưu hiệu năng và dễ dọn dẹp data cũ.
-- Lưu ý: Khi phân vùng, cột dùng để phân vùng (created_at) BẮT BUỘC phải nằm trong Composite Primary Key.
CREATE TABLE booking_operation_logs (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của log vận hành hệ thống',
  public_id BINARY(16) NOT NULL COMMENT 'UUID công khai phục vụ tra cứu log',
  booking_id BIGINT NULL COMMENT 'Liên kết tới bảng bookings (nếu có liên quan)',
  correlation_id BINARY(16) NULL COMMENT 'Mã định danh liên kết chuỗi tương tác trong hệ thống phân tán',
  trace_id VARCHAR(128) NULL COMMENT 'Mã định danh vết (Trace ID) phục vụ giám sát OpenTelemetry/Jaeger',
  span_id VARCHAR(128) NULL COMMENT 'Mã định danh phân đoạn (Span ID) phục vụ giám sát OpenTelemetry/Jaeger',
  service_name VARCHAR(100) NOT NULL COMMENT 'Tên microservice ghi nhận log',
  component_name VARCHAR(100) NULL COMMENT 'Tên thành phần cấu phần ứng dụng (Ví dụ: Controller, Consumer)',
  operation_name VARCHAR(150) NOT NULL COMMENT 'Tên chức năng/hành động kỹ thuật thực thi',
  operation_type ENUM('API', 'SCHEDULER', 'EVENT', 'DATABASE', 'PAYMENT', 'SYSTEM') NOT NULL COMMENT 'Danh mục hình thức vận hành hệ thống',
  status ENUM('STARTED', 'SUCCESS', 'FAILED') NOT NULL COMMENT 'Trạng thái thực thi của hành động kỹ thuật',
  request_data JSON NULL COMMENT 'Dữ liệu đầu vào (Request payload) lưu lại để debug',
  response_data JSON NULL COMMENT 'Dữ liệu đầu ra (Response payload) trả về',
  error_code VARCHAR(100) NULL COMMENT 'Mã lỗi hệ thống/ứng dụng phát sinh nếu thực thi thất bại',
  error_message TEXT NULL COMMENT 'Nội dung thông báo lỗi kỹ thuật chi tiết',
  execution_time_ms INT NULL COMMENT 'Thời gian thực thi của hành động tính bằng mili-giây',
  client_ip VARCHAR(45) NULL COMMENT 'Địa chỉ IP của tác nhân gọi hệ thống',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm ghi nhận hành động kỹ thuật',
  PRIMARY KEY (id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Bảng lưu trữ log vận hành kỹ thuật hỗ trợ giám sát và khắc phục sự cố hệ thống'
PARTITION BY RANGE COLUMNS(created_at) (
    PARTITION p2026_q1 VALUES LESS THAN ('2026-04-01'),
    PARTITION p2026_q2 VALUES LESS THAN ('2026-07-01'),
    PARTITION p2026_q3 VALUES LESS THAN ('2026-10-01'),
    PARTITION p2026_q4 VALUES LESS THAN ('2027-01-01'),
    PARTITION p_future VALUES LESS THAN (MAXVALUE)
);

-- ============================================================
-- PHẦN 7
-- BẢNG: booking_audit_logs
-- BẢNG: booking_snapshots
-- ============================================================
CREATE TABLE booking_audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của log kiểm toán',
  public_id BINARY(16) NOT NULL COMMENT 'UUID công khai của bản ghi kiểm toán',
  booking_id BIGINT NULL COMMENT 'Liên kết tới bảng bookings',
  actor_id BIGINT NULL COMMENT 'Mã định danh của đối tượng thực hiện hành động nghiệp vụ',
  actor_type ENUM('USER', 'ADMIN', 'SYSTEM', 'SCHEDULER') NOT NULL COMMENT 'Loại đối tượng thực hiện hành động nghiệp vụ',
  action VARCHAR(100) NOT NULL COMMENT 'Tên hành động nghiệp vụ được thực hiện (Ví dụ: ConfirmBooking)',
  entity_name VARCHAR(100) NOT NULL COMMENT 'Tên thực thể chịu tác động từ hành động (Ví dụ: Booking)',
  entity_id BIGINT NULL COMMENT 'Mã định danh của thực thể chịu tác động',
  old_value JSON NULL COMMENT 'Trạng thái dữ liệu nghiệp vụ cũ trước khi thay đổi',
  new_value JSON NULL COMMENT 'Trạng thái dữ liệu nghiệp vụ mới sau khi thay đổi',
  reason VARCHAR(500) NULL COMMENT 'Lý do thực hiện hành động nghiệp vụ (Ví dụ: Khách hàng yêu cầu hủy đơn)',
  correlation_id BINARY(16) NULL COMMENT 'Mã định danh kết nối giao dịch phân tán',
  ip_address VARCHAR(45) NULL COMMENT 'Địa chỉ IP của đối tượng thực hiện hành động',
  user_agent VARCHAR(500) NULL COMMENT 'Thông tin trình duyệt/thiết bị của đối tượng thực hiện hành động',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm ghi nhận nhật ký kiểm toán',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Nhật ký kiểm toán lưu trữ dấu vết thay đổi dữ liệu phục vụ bảo mật và thanh tra nghiệp vụ';

CREATE TABLE booking_snapshots (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Mã định danh nội bộ của bản ghi snapshot',
  public_id BINARY(16) NOT NULL COMMENT 'UUID công khai của bản ghi snapshot',
  booking_id BIGINT NOT NULL COMMENT 'Liên kết tới bảng bookings',
  snapshot_type ENUM('MOVIE', 'SHOWTIME', 'CINEMA', 'AUDITORIUM', 'PRICING') NOT NULL COMMENT 'Danh mục dữ liệu ngoại vi cần lưu snapshot',
  snapshot_version INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản cấu trúc của dữ liệu snapshot',
  snapshot_data JSON NOT NULL COMMENT 'Toàn bộ dữ liệu của thực thể ngoại vi được chụp lại dưới dạng JSON bất biến',
  checksum CHAR(64) NULL COMMENT 'Mã băm SHA-256 xác thực tính toàn vẹn, chống chỉnh sửa dữ liệu snapshot',
  source_service VARCHAR(100) NOT NULL COMMENT 'Tên microservice cung cấp nguồn dữ liệu để chụp hình',
  source_version VARCHAR(50) NULL COMMENT 'Phiên bản schema của dữ liệu gốc tại thời điểm chụp hình',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm thực hiện chụp hình sao lưu dữ liệu',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Bản sao lưu bất biến của các dữ liệu thuộc microservices khác tại thời điểm đặt vé thành công';

-- ============================================================
-- PHẦN 8: CÁC RÀNG BUỘC DUY NHẤT (UNIQUE CONSTRAINTS)
-- ============================================================
ALTER TABLE bookings ADD CONSTRAINT uk_bookings_public_id UNIQUE (public_id);
ALTER TABLE bookings ADD CONSTRAINT uk_bookings_booking_code UNIQUE (booking_code);
ALTER TABLE booking_tickets ADD CONSTRAINT uk_booking_ticket UNIQUE (booking_id, seat_id);
ALTER TABLE seat_reservations ADD CONSTRAINT uk_seat_reservation_public_id UNIQUE (public_id);
ALTER TABLE seat_reservations ADD CONSTRAINT uk_seat_reservation_token UNIQUE (reservation_token);
ALTER TABLE booking_outbox_events ADD CONSTRAINT uk_booking_outbox_event UNIQUE (event_id);
ALTER TABLE booking_inbox_events ADD CONSTRAINT uk_booking_inbox_event UNIQUE (event_id);
ALTER TABLE booking_idempotency_keys ADD CONSTRAINT uk_booking_idempotency_key UNIQUE (idempotency_key);
ALTER TABLE booking_retry_tasks ADD CONSTRAINT uk_booking_retry_public_id UNIQUE (public_id);
ALTER TABLE booking_reconciliation_tasks ADD CONSTRAINT uk_booking_reconciliation_public_id UNIQUE (public_id);
ALTER TABLE booking_dead_letter_events ADD CONSTRAINT uk_booking_dead_letter_public_id UNIQUE (public_id);
ALTER TABLE booking_audit_logs ADD CONSTRAINT uk_booking_audit_public_id UNIQUE (public_id);
ALTER TABLE booking_snapshots ADD CONSTRAINT uk_booking_snapshot_public_id UNIQUE (public_id);

-- Tối ưu: Đảm bảo chống trùng lặp event từ Payment Service gửi sang
ALTER TABLE booking_payment_events ADD CONSTRAINT uk_booking_payment_event UNIQUE (event_id);
ALTER TABLE booking_payment_events ADD CONSTRAINT uk_payment_gateway_txn UNIQUE (payment_method, external_transaction_id);

-- ============================================================
-- PHẦN 9: KHÓA NGOẠI VẬT LÝ (FOREIGN KEYS - Chỉ áp dụng trong nội bộ context)
-- Note: booking_operation_logs loại bỏ FK vật lý để tránh lỗi liên quan đến Partitioning.
-- ============================================================
ALTER TABLE booking_tickets ADD CONSTRAINT fk_booking_ticket_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE seat_reservations ADD CONSTRAINT fk_seat_reservation_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE booking_status_histories ADD CONSTRAINT fk_booking_status_history_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE booking_payment_events ADD CONSTRAINT fk_booking_payment_event_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE booking_reconciliation_tasks ADD CONSTRAINT fk_booking_reconciliation_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE booking_audit_logs ADD CONSTRAINT fk_booking_audit_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE booking_snapshots ADD CONSTRAINT fk_booking_snapshot_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- ============================================================
-- PHẦN 10: CHỈ MỤC (INDEXES)
-- ============================================================
CREATE INDEX idx_booking_user ON bookings(user_id);
CREATE INDEX idx_booking_showtime ON bookings(showtime_id);
CREATE INDEX idx_booking_status ON bookings(status);
CREATE INDEX idx_booking_status_deadline ON bookings(status, payment_deadline);
CREATE INDEX idx_booking_created_at ON bookings(created_at);
CREATE INDEX idx_booking_deleted_at ON bookings(deleted_at);

CREATE INDEX idx_booking_ticket_booking ON booking_tickets(booking_id);
CREATE INDEX idx_booking_ticket_seat ON booking_tickets(seat_id);

CREATE INDEX idx_seat_reservation_lookup ON seat_reservations(showtime_id, seat_id);
CREATE INDEX idx_seat_reservation_user ON seat_reservations(user_id);
CREATE INDEX idx_seat_reservation_booking ON seat_reservations(booking_id);
CREATE INDEX idx_seat_reservation_expiration ON seat_reservations(status, expires_at);

CREATE INDEX idx_booking_history_booking ON booking_status_histories(booking_id);

CREATE INDEX idx_payment_event_booking ON booking_payment_events(booking_id);
CREATE INDEX idx_payment_event_status ON booking_payment_events(payment_status);

CREATE INDEX idx_outbox_retry ON booking_outbox_events(status, next_retry_at);
CREATE INDEX idx_outbox_aggregate ON booking_outbox_events(aggregate_type, aggregate_id);

CREATE INDEX idx_inbox_status ON booking_inbox_events(status);
CREATE INDEX idx_inbox_source ON booking_inbox_events(source_service, event_type);

CREATE INDEX idx_idempotency_user ON booking_idempotency_keys(user_id);
CREATE INDEX idx_idempotency_expiration ON booking_idempotency_keys(expires_at);

CREATE INDEX idx_retry_status ON booking_retry_tasks(status);
CREATE INDEX idx_retry_next ON booking_retry_tasks(next_retry_at);
CREATE INDEX idx_retry_reference ON booking_retry_tasks(reference_type, reference_id);

CREATE INDEX idx_reconciliation_booking ON booking_reconciliation_tasks(booking_id);

CREATE INDEX idx_dead_letter_status ON booking_dead_letter_events(status);

CREATE INDEX idx_operation_booking ON booking_operation_logs(booking_id);
CREATE INDEX idx_operation_trace ON booking_operation_logs(trace_id);

CREATE INDEX idx_audit_booking ON booking_audit_logs(booking_id);
CREATE INDEX idx_audit_created ON booking_audit_logs(created_at);

CREATE INDEX idx_snapshot_booking ON booking_snapshots(booking_id);

-- ============================================================
-- PHẦN 11: RÀNG BUỘC KIỂM TRA GIÁ TRỊ (CHECK CONSTRAINTS)
-- ============================================================
ALTER TABLE bookings ADD CONSTRAINT chk_booking_total_amount CHECK (total_amount >= 0);
ALTER TABLE bookings ADD CONSTRAINT chk_booking_ticket_count CHECK (ticket_count > 0);
ALTER TABLE booking_tickets ADD CONSTRAINT chk_booking_ticket_price CHECK (ticket_price >= 0);
ALTER TABLE booking_payment_events ADD CONSTRAINT chk_payment_amount CHECK (amount >= 0);
ALTER TABLE booking_retry_tasks ADD CONSTRAINT chk_retry_max_count CHECK (max_retry_count > 0);
ALTER TABLE seat_reservations ADD CONSTRAINT chk_reservation_expiration CHECK (expires_at > created_at);