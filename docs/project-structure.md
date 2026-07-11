# Tài Liệu Cấu Trúc Thư Mục Dự Án

Tài liệu này quy định và giải thích cấu trúc thư mục phân rã cho hệ thống Đặt Vé Xem Phim Trực Tuyến. Toàn bộ thành viên nhóm bắt buộc phải tuân thủ đúng vị trí phân chia này trong suốt quá trình phát triển dự án.

## 1. Sơ Đồ Cấu Trúc Hệ Thống (Repository Structure)

```text
hcm26_cpl_java_05_group3/
├── client/
├── server/
│   ├── auth-service/
│   ├── movie-service/
│   ├── booking-service/
│   ├── payment-service/
│   └── notification-service/
├── api-gateway/
└── docs/
```

## 2. Mô Tả Chi Tiết các Thành Phần (Component Descriptions)

*   **`client/`**: Chứa toàn bộ mã nguồn của ứng dụng phía giao diện người dùng (Frontend/Client).
*   **`server/`**: Chứa mã nguồn của các backend microservices viết bằng Java:
    *   **`auth-service/`**: Dịch vụ chịu trách nhiệm xác thực người dùng, đăng ký, đăng nhập và cấp phát token (JWT/OAuth2).
    *   **`movie-service/`**: Dịch vụ quản lý thông tin chi tiết về phim, danh sách phim, lịch chiếu, cụm rạp và sơ đồ phòng chiếu.
    *   **`booking-service/`**: Dịch vụ chịu trách nhiệm xử lý các yêu cầu đặt vé, giữ ghế tạm thời, tạo hóa đơn và điều phối quá trình đặt chỗ.
    *   **`payment-service/`**: Dịch vụ tích hợp với các cổng thanh toán trực tuyến (VNPay, Momo, thẻ ngân hàng...) để thực hiện giao dịch tài chính.
    *   **`notification-service/`**: Dịch vụ xử lý việc gửi email xác nhận đặt vé, gửi vé điện tử (QR Code) và thông báo qua SMS hoặc hệ thống notification.
*   **`api-gateway/`**: Cổng Gateway trung gian chịu trách nhiệm định tuyến các request từ client tới các microservices tương ứng, kiểm soát traffic, phân quyền cơ bản và giới hạn băng thông (rate limiting).
*   **`docs/`**: Chứa toàn bộ tài liệu hướng dẫn dự án, đặc tả API, hướng dẫn Git workflow và tài liệu cấu trúc thư mục này.

---

> [!NOTE]
> Tất cả các thư mục trống ở trên đã được đưa vào Git tracking bằng file `.gitkeep`. Không được xóa các file này cho đến khi có mã nguồn thực tế được thêm vào.
