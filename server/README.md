# LoraFilm Backend Services

Thư mục này chứa 9 Spring Boot microservice độc lập. Mỗi service có Maven build,
cấu hình, schema và database riêng; không có Maven multi-module build ở cấp
`server/`.

## Service map

| Service | Cổng | Database | Health |
|---|---:|---|---|
| `auth-service` | 8081 | `auth_db` | Kiểm tra qua Eureka/startup log |
| `movie-service` | 8082 | `movie_db` | `/actuator/health` |
| `booking-service` | 8083 | `booking_db` | `/actuator/health` |
| `payment-service` | 8084 | `payment_db` | `/health` |
| `notification-service` | 8085 | `notification_db` | `/actuator/health` |
| `user-service` | 8086 | `user_db` | `/health` |
| `promotion-service` | 8087 | `promotion_db` | `/actuator/health` |
| `score-service` | 8088 | `score_db` | `/actuator/health` |
| `analytics-service` | 8089 | `analytics_db` | `/actuator/health` |

API Gateway (`../api-gateway`) chạy tại 8080 và Eureka
(`../eureka-server`) chạy tại 8761.

## Chạy một service

Yêu cầu Java 21, Maven 3.9+, MySQL 8 và các dependency hạ tầng phù hợp. Từ thư
mục gốc repository, khởi động hạ tầng bằng `docker compose up -d`, sau đó:

```powershell
Copy-Item server/movie-service/src/main/resources/application.example.properties `
          server/movie-service/src/main/resources/application.properties
cd server/movie-service
mvn spring-boot:run
```

Thay `movie-service` bằng service cần chạy. File `application.properties` là cấu
hình local đã được Git ignore. Mọi service dùng `ddl-auto=validate`, vì vậy phải
chạy schema canonical tương ứng trong `../docs/database/mysql/schema/` trước lần khởi
động đầu tiên.

Thứ tự chạy toàn hệ thống:

1. MySQL, Redis, Kafka và Zookeeper.
2. Eureka Server.
3. Các service cần dùng.
4. API Gateway.

Không phải service nào cũng cần toàn bộ Redis/Kafka trong mọi luồng, nhưng chạy
đủ hạ tầng giúp tránh health check hoặc consumer thất bại trong integration test.

## Build và API discovery

Mỗi service được kiểm tra độc lập:

```powershell
cd server/<service-name>
mvn test
```

Một số integration test sử dụng Testcontainers và cần Docker. OpenAPI JSON thông
thường có tại `/v3/api-docs`; Swagger UI có tại `/swagger-ui.html` hoặc
`/swagger-ui/index.html` tùy Springdoc redirect của service.

Tài liệu API, event, schema và thiết kế được lập chỉ mục tại
[`docs/README.md`](../docs/README.md). README riêng hiện có trong một số service
chỉ bổ sung chi tiết đặc thù của service đó.

## Nguyên tắc service boundary

- Service chỉ sở hữu schema của chính nó; không tạo foreign key hoặc truy vấn
  trực tiếp database của service khác.
- Giao tiếp đồng bộ qua API công khai/nội bộ đã xác thực; giao tiếp bất đồng bộ
  qua event contract đã được tài liệu hóa.
- Thay đổi endpoint, event, schema hoặc biến môi trường phải cập nhật contract,
  canonical schema và file cấu hình mẫu trong cùng Merge Request.
- Không commit `application.properties`, secret, output trong `target/` hay log
  runtime.
