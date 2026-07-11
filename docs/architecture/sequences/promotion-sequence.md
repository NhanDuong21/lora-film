# Promotion Service Sequence

> Đây là draft flow cho **Promotion module** trong hệ thống đặt vé xem phim online.
> Promotion Service là service riêng, chỉ xử lý việc validate mã khuyến mãi và trả kết quả giảm giá.

## Mục đích

Tài liệu này mô tả luồng kiểm tra mã khuyến mãi và áp dụng discount vào booking/payment.

## Diagram liên quan

```text
docs/architecture/diagrams/promotion-sequence.puml
docs/architecture/diagrams/promotion-sequence.drawio
docs/architecture/diagrams/promotion-sequence.png
```

## Mô tả flow

### Bước 1: User nhập mã khuyến mãi

User nhập mã khuyến mãi trên giao diện React Frontend trong quá trình đặt vé.

### Bước 2: React Frontend gửi request kiểm tra promotion

React Frontend gửi request đến API Gateway để kiểm tra mã khuyến mãi.

```text
POST /api/promotions/validate
```

Request có thể gồm:

```json
{
  "userId": "USER_ID",
  "promotionCode": "SALE20",
  "bookingAmount": 200000,
  "ticketQuantity": 2
}
```

### Bước 3: API Gateway route request đến Promotion Service

API Gateway nhận request từ frontend và chuyển tiếp request đến Promotion Service.

Promotion Service chỉ xử lý nghiệp vụ liên quan đến promotion, không xử lý điểm thưởng.

### Bước 4: Promotion Service validate mã khuyến mãi

Promotion Service kiểm tra:

* Mã khuyến mãi có tồn tại không
* Mã còn hiệu lực không
* Mã còn lượt sử dụng không
* User có đủ điều kiện dùng mã không
* Booking có đạt giá trị tối thiểu không

### Bước 5: Promotion Service trả discount result

Nếu mã hợp lệ, Promotion Service trả về discount result.

```json
{
  "valid": true,
  "promotionCode": "SALE20",
  "discountAmount": 40000,
  "finalAmount": 160000
}
```

Nếu mã không hợp lệ, Promotion Service trả về lỗi.

```json
{
  "valid": false,
  "message": "Promotion code is invalid or expired"
}
```

### Bước 6: Booking Service kiểm tra lại promotion

Khi user xác nhận đặt vé, Booking Service kiểm tra lại promotion trước khi áp dụng discount.

Việc kiểm tra lại giúp tránh trường hợp dữ liệu từ frontend bị chỉnh sửa hoặc mã khuyến mãi thay đổi trạng thái.

### Bước 7: Booking/Payment áp dụng discount vào tổng tiền

Booking Service áp dụng discount vào tổng tiền booking.

```text
finalAmount = originalAmount - discountAmount
```

Sau đó Booking Service gửi `finalAmount` sang Payment Service để xử lý thanh toán.

## Ghi chú

* Đây là draft flow cho Promotion module.
* Promotion Service là service riêng.
* Promotion Service không xử lý điểm thưởng.
* Score Service không nằm trong diagram này.
* Code PlantUML để vẽ diagram nằm trong file `promotion-sequence.puml`.
