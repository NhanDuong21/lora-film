# Đặc Tả Hợp Đồng Event Đăng Ký Tài Khoản (Registration Events Contract)

Tài liệu này quy định cấu trúc gói tin (Message Schema), định dạng phân phối và các nguyên tắc xử lý bất đồng bộ đối với luồng đăng ký tài khoản trong hệ thống LoraFilm. 

> **Lưu ý Lịch sử (23/06/2026):** Event `ACCOUNT_CREATED` cũ đã được thay thế bằng bộ 3 sự kiện Kafka theo mô hình Request-Reply Pattern nhằm đảm bảo tính toàn vẹn dữ liệu (không trùng lặp phone/CCCD) và tránh tạo "orphan accounts" (hồ sơ rác).

---

## 1. Kiến Trúc Luồng Đi Dữ Liệu (Message Topology Flow)

Luồng xử lý nghiệp vụ đăng ký hiện tại diễn ra theo 3 giai đoạn (3 Kafka Events):

1. **Giai đoạn 1: Yêu cầu Validate**
   - Ứng dụng React -> API Gateway -> Auth Service.
   - Auth Service phát hành Event `REGISTRATION_VALIDATION_REQUESTED` lên Kafka.
2. **Giai đoạn 2: Trả kết quả Validate**
   - User Service tiêu thụ (consume) Event, kiểm tra Phone/CCCD trong Database và Redis (reservation).
   - User Service phát hành Event `REGISTRATION_VALIDATION_RESULT` trả lại cho Auth Service.
   - Auth Service nhận kết quả. Nếu `SUCCESS` -> Tạo tài khoản, gửi OTP. Nếu `FAILED` -> Hủy đăng ký.
3. **Giai đoạn 3: Xác thực OTP & Tạo User Profile**
   - Người dùng nhập OTP -> Auth Service xác thực OTP thành công.
   - Auth Service phát hành Event `ACCOUNT_VERIFIED`.
   - User Service tiêu thụ Event, chính thức tạo User Profile trong Database và xóa reservation trên Redis.

---

## 2. Thông Tin Cấu Hình Hàng Đợi Kafka (Kafka Stream Configurations)

| Thuộc tính | Sự kiện 1 (Validate Request) | Sự kiện 2 (Validate Result) | Sự kiện 3 (Account Verified) |
| :--- | :--- | :--- | :--- |
| **Topic** | `auth.registration.validation.requested.v1` | `auth.registration.validation.result.v1` | `auth.account.verified.v1` |
| **Loại Sự Kiện** | `REGISTRATION_VALIDATION_REQUESTED` | `REGISTRATION_VALIDATION_RESULT` | `ACCOUNT_VERIFIED` |
| **Producer** | `auth-service` | `user-service` | `auth-service` |
| **Consumer** | `user-service` | `auth-service` | `user-service` |
| **Consumer Group** | `user-service-validation-group` | `auth-service-registration-group` | `user-service-account-verified-consumer` |
| **Message Key** | `requestId` | `requestId` | `accountId` |

---

## 3. Đặc Tả Gói Tin Sự Kiện (Event Payload Specification)

Gói tin được truyền tải qua Kafka sử dụng định dạng chuỗi JSON thuần ký tự.

### 3.1. Event: REGISTRATION_VALIDATION_REQUESTED

**Mô tả:** Auth Service gửi thông tin người dùng muốn đăng ký sang User Service để kiểm tra trùng lặp.

**Định Dạng Mẫu (JSON):**
```json
{
  "eventId": "uuid-string-1",
  "eventType": "REGISTRATION_VALIDATION_REQUESTED",
  "eventVersion": "1.0",
  "source": "auth-service",
  "occurredAt": "2026-06-23T02:20:30.007Z",
  "data": {
    "requestId": "request-uuid-1234",
    "email": "user@example.com",
    "phoneNumber": "0901234567",
    "cccd": "092205006789"
  }
}
```

### 3.2. Event: REGISTRATION_VALIDATION_RESULT

**Mô tả:** User Service trả lời Auth Service về việc Phone và CCCD có hợp lệ để đăng ký tiếp hay không.

**Định Dạng Mẫu (JSON):**
```json
{
  "eventId": "uuid-string-2",
  "eventType": "REGISTRATION_VALIDATION_RESULT",
  "eventVersion": "1.0",
  "source": "user-service",
  "occurredAt": "2026-06-23T02:20:31.007Z",
  "data": {
    "requestId": "request-uuid-1234",
    "status": "SUCCESS", 
    "errorCode": null
  }
}
```
*(Ghi chú: Nếu thất bại, `status` là `FAILED` và `errorCode` có thể là `PHONE_NUMBER_ALREADY_EXISTS` hoặc `CCCD_ALREADY_EXISTS`)*

### 3.3. Event: ACCOUNT_VERIFIED

**Mô tả:** Được Auth Service phát hành sau khi người dùng xác nhận đúng mã OTP. Báo hiệu cho User Service tạo User Profile.

**Định Dạng Mẫu (JSON):**
```json
{
  "eventId": "uuid-string-3",
  "eventType": "ACCOUNT_VERIFIED",
  "eventVersion": "1.0",
  "source": "auth-service",
  "occurredAt": "2026-06-23T02:25:30.007Z",
  "data": {
    "accountId": 1,
    "email": "user@example.com",
    "role": "CUSTOMER",
    "fullName": "Nguyen Van A",
    "phoneNumber": "0901234567",
    "cccd": "092205006789",
    "cccdMasked": "092******789",
    "provinceCode": "092",
    "provinceName": "Cần Thơ",
    "gender": "MALE",
    "birthday": "2005-06-12",
    "birthYear": 2005
  }
}
```

---

## 4. Quy Tắc Bảo Mật Và Ràng Buộc Dữ Liệu (Security Constraints)

* **Bảo vệ mật khẩu người dùng:** Nghiêm cấm tuyệt đối việc nhúng trường dữ liệu mật khẩu (`password`) vào bất kỳ gói tin Kafka nào. 
* **Nguyên tắc ghi nhật ký (Log Masking Policy):** Cả hai phân hệ `auth-service` và `user-service` tuyệt đối không được phép in (log) chuỗi số CCCD nguyên bản (12 số) ra các hệ thống file nhật ký. Chỉ cho phép in trường dữ liệu `cccdMasked`.

---

## 5. Cơ Chế Xử Lý Trùng Lặp Và Khôi Phục Lỗi (Idempotency & Retry)

Do mạng lưới Kafka có thể gửi tin trùng lặp (At-least-once delivery):

* **Tại User Service (ACCOUNT_VERIFIED):** Bắt buộc kiểm tra xem `accountId` (hoặc email/phone) đã tồn tại trong bảng `users` chưa trước khi Insert. Nếu đã tồn tại -> Bỏ qua bản tin (Skip Event) và in log cảnh báo, không throw Exception.
* **Cơ chế Reserve:** User Service sử dụng Redis để "tạm giữ" (reserve) Phone và CCCD trong thời gian 15 phút ngay khi nhận được `REGISTRATION_VALIDATION_REQUESTED`. Reservation này sẽ được xóa (release) khi nhận được `ACCOUNT_VERIFIED`.
* **DLQ (Dead Letter Queue):** Nếu có lỗi parse JSON (Poison Pill), Kafka Deserializer cấu hình mặc định đẩy log và bỏ qua hoặc gửi sang topic lỗi. Lỗi Database cục bộ được ném Exception để Kafka tự động Re-poll.
