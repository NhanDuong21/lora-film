# Development Guidelines

> Mục tiêu của tài liệu này là thống nhất quy tắc phát triển Promotion Service nhằm:
>
> - Giảm tối đa xung đột (Merge Conflict).
> - Đảm bảo chất lượng mã nguồn.
> - Đồng nhất Coding Style.
> - Dễ Review, Test và Maintain.
> - Tuân thủ kiến trúc Package by Feature (DDD Lite).

---

# 1. Development Principles

Promotion Service được phát triển theo các nguyên tắc:

- Package by Feature (DDD Lite)
- Domain-driven Design
- Clean Architecture
- SOLID Principles
- Production Ready
- Code First
- API First
- Database Migration bằng Flyway/Liquibase
- Không sử dụng Lombok

---

# 2. Domain Ownership

Mỗi thành viên chỉ chịu trách nhiệm **một Domain**.

Không chỉnh sửa Business Logic của Domain khác nếu chưa trao đổi với người phụ trách.

| Domain | Owner |
|----------|-------|
| promotion | Member A |
| benefit | Member B |
| reservation | Member C |
| partner | Member D |
| configuration | Member E |
| integration | Member F |
| common | Foundation Owner |

---

# 3. Foundation Owner

Foundation Owner chịu trách nhiệm toàn bộ phần hạ tầng của hệ thống.

Bao gồm:

- Project Initialization
- Maven Configuration
- Package Structure
- Spring Security
- JWT
- Swagger/OpenAPI
- Kafka Configuration
- Redis Configuration
- Feign Configuration
- Database Configuration
- Flyway/Liquibase
- Global Exception Handler
- Base Entity
- Base Repository
- Common Utility
- Validation Framework
- Logging
- Audit Framework
- Idempotency Framework

Các thành viên khác **không tự ý chỉnh sửa** các thành phần này.

---

# 4. Domain Structure

Mỗi Domain phải tuân thủ cùng một cấu trúc thư mục.

```text
domain
├── controller
├── service
│   ├── impl
│   └── validator
├── repository
├── entity
├── dto
│   ├── request
│   └── response
├── mapper
├── specification
├── exception
└── enum
```

Không tạo thêm package mới nếu chưa thống nhất với cả nhóm.

---

# 5. Domain Isolation Rule

Mỗi Domain chỉ được phép:

- Sửa Entity của Domain mình.
- Sửa Repository của Domain mình.
- Sửa Controller của Domain mình.
- Sửa Service của Domain mình.
- Sửa DTO của Domain mình.

Không được sửa trực tiếp Domain khác.

Ví dụ:

Promotion Domain không được sửa:

- benefit/*
- reservation/*
- partner/*

---

# 6. File Ownership

Một số file chỉ có Foundation Owner được chỉnh sửa.

| File | Owner |
|------|-------|
| pom.xml | Foundation |
| application.yml | Foundation |
| application-dev.yml | Foundation |
| application-prod.yml | Foundation |
| Dockerfile | Foundation |
| docker-compose.yml | Foundation |
| SecurityConfig | Foundation |
| SwaggerConfig | Foundation |
| KafkaConfig | Foundation |
| RedisConfig | Foundation |
| WebMvcConfig | Foundation |
| GlobalExceptionHandler | Foundation |
| BaseEntity | Foundation |
| BaseRepository | Foundation |

---

# 7. Git Branch Strategy

Mỗi Issue tương ứng với một Branch.

Ví dụ:

```text
feature/foundation

feature/promotion-domain

feature/benefit-domain

feature/reservation-domain

feature/partner-domain

feature/configuration-domain

feature/integration-domain
```

Không commit trực tiếp lên:

- main
- master
- develop

---

# 8. Pull Request Rule

Mỗi Pull Request chỉ giải quyết **một Issue**.

Không gộp nhiều Issue vào cùng một Pull Request.

Ví dụ:

✅ Đúng

```
Promotion CRUD
```

❌ Sai

```
Promotion CRUD

+

Voucher CRUD

+

Kafka
```

---

# 9. Commit Convention

Định dạng:

```text
type(scope): description
```

Ví dụ:

```text
feat(promotion): implement campaign creation

feat(benefit): implement voucher redemption

fix(reservation): resolve timeout release

refactor(partner): optimize settlement query

docs(readme): update development guideline

test(promotion): add campaign service test
```

---

# 10. Coding Convention

Toàn bộ source code sử dụng:

- Java 21
- Spring Boot 3
- Spring Data JPA
- Jakarta Validation

Coding Style:

- Class PascalCase
- Method camelCase
- Variable camelCase
- Constant UPPER_SNAKE_CASE

Ví dụ:

```java
PromotionCampaignService
```

```java
createCampaign()
```

```java
promotionRepository
```

```java
MAX_RETRY_COUNT
```

---

# 11. Không sử dụng Lombok

Promotion Service **không sử dụng Lombok**.

Không được dùng:

```java
@Getter
```

```java
@Setter
```

```java
@Data
```

```java
@Builder
```

```java
@NoArgsConstructor
```

```java
@AllArgsConstructor
```

```java
@RequiredArgsConstructor
```

Tất cả Entity, DTO và Model phải viết thủ công.

Ví dụ:

```java
private String campaignName;

public String getCampaignName() {
    return campaignName;
}

public void setCampaignName(String campaignName) {
    this.campaignName = campaignName;
}
```

Lý do:

- Dễ Debug.
- Dễ Review.
- Rõ ràng khi Merge.
- Không phụ thuộc Annotation Processor.
- Đồng nhất với Coding Standard của dự án.

---

# 12. Constructor Rule

Không sử dụng Lombok Constructor.

Ví dụ:

```java
public PromotionCampaign() {
}

public PromotionCampaign(String code, String name) {
    this.code = code;
    this.name = name;
}
```

---

# 13. Entity Rule

Entity chỉ chứa:

- Mapping
- Getter
- Setter
- Constructor
- equals()
- hashCode()
- toString()

Không đặt Business Logic trong Entity.

---

# 14. Service Rule

Business Logic chỉ được viết trong Service.

Không viết Business Logic trong:

- Controller
- Entity
- Repository

---

# 15. Controller Rule

Controller chỉ thực hiện:

- Validate Request
- Gọi Service
- Trả Response

Không viết Business Logic.

---

# 16. Repository Rule

Repository chỉ chứa truy vấn dữ liệu.

Không viết:

- Validation
- Business Logic
- Mapping

---

# 17. DTO Rule

Request DTO

↓

Validation

↓

Service

↓

Response DTO

Không trả Entity trực tiếp ra API.

---

# 18. Merge Conflict Prevention

Để giảm tối đa Merge Conflict:

- Mỗi thành viên chỉ làm một Domain.
- Không sửa package của Domain khác.
- Không sửa Entity của Domain khác.
- Không sửa Repository của Domain khác.
- Không sửa Controller của Domain khác.
- Không sửa Service của Domain khác.
- Foundation Owner là người duy nhất chỉnh sửa các file cấu hình.
- Luôn Pull/Rebase nhánh `develop` mới nhất trước khi bắt đầu làm việc và trước khi tạo Pull Request.
- Mỗi Pull Request chỉ tập trung vào một chức năng hoặc một Issue.

---

# 19. Code Review Checklist

Trước khi tạo Pull Request, cần kiểm tra:

- Code build thành công.
- Không có lỗi Compile.
- Không có Warning nghiêm trọng.
- Đã thêm Validation đầy đủ.
- Không sử dụng Lombok.
- Không sửa Domain khác.
- Đúng Coding Convention.
- Đúng Package Structure.
- Đúng Naming Convention.
- Đã cập nhật Migration nếu thay đổi Database.

---

# 20. Definition of Done

Một Issue được xem là hoàn thành khi:

- Hoàn thành toàn bộ yêu cầu nghiệp vụ.
- Build thành công.
- Không có lỗi Compile.
- Đã Review.
- Đã Merge vào develop.
- Không gây ảnh hưởng Domain khác.
- Tuân thủ toàn bộ Coding Guideline của dự án.
```