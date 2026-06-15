# Đặc Tả Hợp Đồng Event: ACCOUNT_CREATED (Event Contract)

Tài liệu này quy định cấu trúc gói tin (Message Schema), định dạng phân phối và các nguyên tắc xử lý bất đồng bộ đối với sự kiện khởi tạo tài khoản thành công (`ACCOUNT_CREATED`) trong hệ thống LoraFilm. Tài liệu này đóng vai trò là hợp đồng kỹ thuật bắt buộc giữa phân hệ Xác thực (`auth-service`) và phân hệ Hồ sơ cá nhân (`user-service`).

## 1. Kiến Trúc Luồng Đi Dữ Liệu (Message Topology Flow)
Luồng xử lý nghiệp vụ diễn ra theo trình tự tuyến tính sau:
Ứng dụng React Frontend -> API Gateway -> Auth Service (Lưu cơ sở dữ liệu `accounts` thành công) -> Phát hành (Publish) Event `ACCOUNT_CREATED` -> Hệ thống hàng đợi Kafka -> User Service (Tiếp nhận Consumer và khởi tạo hồ sơ trong bảng `users`).

## 2. Thông Tin Cấu Hình Hàng Đợi Kafka (Kafka Stream Configurations)
* **Tên Kafka Topic:** `auth.account.created.v1`
* **Kafka Message Key:** `accountId`
* **Consumer Group:** `user-service-account-created-consumer`
* **Loại Sự Kiện (Event Type):** `ACCOUNT_CREATED`
* **Phiên Bản Hợp Đồng (Event Version):** `1.0`
* **Thành Phần Phát Hành (Producer):** `auth-service`
* **Thành Phần Tiếp Nhận (Consumer):** `user-service`
* **Payload Format:** JSON

## 3. Đặc Tả Gói Tin Sự Kiện (Event Payload Specification)
Gói tin được truyền tải qua Kafka sử dụng định dạng chuỗi JSON thuần ký tự.

### Danh Mục Các Trường Dữ Liệu Gói Tin (Metadata & Data Fields)
| Trường (Field) | Kiểu Dữ Liệu | Bắt Buộc | Thuộc Tính Phân Vùng | Mô Tả Kỹ Thuật |
| :--- | :--- | :--- | :--- | :--- |
| eventId | String (UUID) | Có | Metadata | Mã định danh duy nhất cho bản tin event (UUID string) |
| eventType | String | Có | Metadata | Giá trị hằng số cố định: `ACCOUNT_CREATED` |
| eventVersion | String | Có | Metadata | Phiên bản hợp đồng gói tin: `1.0` |
| source | String | Có | Metadata | Tên định danh dịch vụ phát hành: `auth-service` |
| occurredAt | String (ISO 8601)| Có | Metadata | Thời điểm phát sinh sự kiện hệ thống dưới định dạng chuỗi ISO 8601 UTC (Ví dụ: `2026-06-15T02:20:30.007Z`) |
| data | Object | Có | Payload | Khối dữ liệu chứa thông tin nghiệp vụ chi tiết |
| data.accountId | Long / Bigint | Có | Payload | Khóa chính ID tài khoản vừa được tạo thành công |
| data.email | String | Có | Payload | Địa chỉ Email đăng ký của tài khoản người dùng |
| data.role | String | Có | Payload | Phân quyền mặc định hệ thống cấp (Ví dụ: `CUSTOMER`) |
| data.fullName | String | Có | Payload | Họ và tên đầy đủ của người dùng |
| data.phoneNumber| String | Có | Payload | Số điện thoại liên hệ |
| data.cccd | String | Có | Payload | Số Căn cước công dân gốc (12 chữ số) |
| data.cccdMasked | String | Có | Payload | Số CCCD đã được che một phần để hiển thị an toàn |
| data.provinceCode | String | Có | Payload | Mã định danh tỉnh thành cấp thẻ CCCD (3 chữ số) |
| data.provinceName | String | Có | Payload | Tên tỉnh thành tương ứng giải mã từ CCCD |
| data.gender | String | Có | Payload | Định danh giới tính (Ví dụ: `MALE`, `FEMALE`) |
| data.birthday | String (YYYY-MM-DD)| Có | Payload | Ngày tháng năm sinh được serialized dưới định dạng `yyyy-MM-dd` |
| data.birthYear | Integer | Có | Payload | Năm sinh bóc tách từ thông tin CCCD |

### Định Dạng Mẫu Gói Tin Gửi Đi (Standard Event Payload JSON)
```json
{
  "eventId": "uuid-string",
  "eventType": "ACCOUNT_CREATED",
  "eventVersion": "1.0",
  "source": "auth-service",
  "occurredAt": "2026-06-15T02:20:30.007Z",
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

## 4. Quy Tắc Bảo Mật Và Ràng Buộc Dữ Liệu (Security Constraints)
* **Bảo vệ mật khẩu người dùng:** Nghiêm cấm tuyệt đối việc nhúng trường dữ liệu mật khẩu (`password`) hoặc chuỗi mã hóa mật khẩu (`password_hash`) vào gói tin Kafka này. Dịch vụ `user-service` hoàn toàn không có quyền hạn và không được phép phụ thuộc vào dữ liệu bảo mật mật khẩu.
* **Nguyên tắc ghi nhật ký (Log Masking Policy):** Trong quá trình vận hành luồng xử lý hoặc in vết debug hệ thống, cả hai phân hệ `auth-service` và `user-service` tuyệt đối không được phép in (log) chuỗi số CCCD nguyên bản (12 số) ra các hệ thống file nhật ký ứng dụng (application logs). Chỉ cho phép in trường dữ liệu `cccdMasked` nhằm tránh rò rỉ thông tin cá nhân của khách hàng.
* **Độc quyền phân phối:** Chỉ có dịch vụ `auth-service` được phép giữ quyền làm Producer phát bản tin này lên Topic.

## 5. Cơ Chế Xử Lý Trùng Lặp Và Đảm Bảo Tính Bất Biến (Idempotency Rule)
Do mạng lưới phân phối của Kafka hoạt động theo cơ chế đảm bảo gửi tin ít nhất một lần (At-least-once delivery), một bản tin sự kiện hoàn toàn có thể bị gửi trùng lặp nhiều lần do mất kết nối ACKs mạng.

* **Idempotent Processing:** Dịch vụ `user-service` bắt buộc phải triển khai cơ chế xử lý trùng lặp đảm bảo tính bất biến (idempotent processing) dựa trên `accountId` để an toàn xử lý các sự kiện trùng lặp (duplicate events).
* **Nguyên tắc xử lý của Consumer (user-service):** Trước khi thực hiện lệnh chèn (Insert) một bản ghi hồ sơ mới, `user-service` bắt buộc phải thực hiện truy vấn kiểm tra xem giá trị trường `data.accountId` nhận được từ gói tin đã tồn tại trong bảng dữ liệu nội bộ của mình hay chưa.
* **Hành vi xử lý khi phát hiện trùng:** Nếu trường `account_id` đã có sẵn trong database, Consumer phải hiểu đây là một bản tin gửi lặp. Consumer tiến hành Bỏ qua bản tin (Skip Event), in một dòng thông báo log cảnh báo an toàn hệ thống (Safe Warning Log) và thực hiện xác nhận ACK hoàn thành bản tin với Kafka Broker. Tuyệt đối không được phép ném ra lỗi ngoại lệ hệ thống (Fatal Exception) gây treo hoặc sập cụm Consumer.

## 6. Cơ Chế Khôi Phục Lỗi Và Tái Thử Nghiệm (Retry & Error Handling)
Trường hợp xảy ra sập kết nối cơ sở dữ liệu tạm thời (Database Connection Timeout) tại user-service, Consumer cấu hình cơ chế tự động thử lại (Retry) tối đa 3 lần với khoảng cách thời gian giãn cách tăng dần (Exponential Backoff).

Nếu sau 3 lần thử lại vẫn thất bại do lỗi logic hệ thống (Data Malformation Lỗi dữ liệu), bản tin lỗi phải được điều hướng tự động sang hàng đợi riêng biệt (Dead Letter Queue - DLQ) có tên là auth.account.created.v1.dlq để đội ngũ vận hành rà soát thủ công, tránh gây tắc nghẽn luồng xử lý của các tài khoản đăng ký tiếp theo.

## 7. Đồng Bộ Môi Trường Docker-Compose
Toàn bộ đội ngũ phát triển Backend thống nhất sử dụng chung cấu hình cụm Kafka Broker (Zookeeper hoặc KRaft mode) được định nghĩa tập trung trong tệp cấu hình docker-compose.yml ở thư mục gốc dự án để đảm bảo tính đồng bộ môi trường phát triển dưới máy local.
