# PROMOTION SERVICE — ĐẶC TẢ THIẾT KẾ & TRIỂN KHAI (v2)
### Hệ thống bán vé xem phim (Movie Ticketing Platform) — Microservices Architecture
### Tham chiếu nghiệp vụ thực tế: CGV Cinemas, Galaxy Cinema, Lotte Cinema (Việt Nam)
### Phạm vi: **promotion-service ĐỘC LẬP** — không gộp chung `score-service`

> **Thay đổi quan trọng so với v1**: bản v1 vô tình để Promotion sở hữu dữ liệu điểm thưởng/hạng thành viên (`point_transactions`, `user_point_wallets`, `membership_tiers`). Bản v2 này sửa triệt để: **score-service là chủ sở hữu duy nhất (single source of truth)** của điểm & hạng thành viên. Promotion **chỉ tiêu thụ** dữ liệu đó qua API/event, không lưu trữ, không tính toán.

---

## 01. Introduction

**Promotion Service** là service chịu trách nhiệm về **toàn bộ logic khuyến mãi, giảm giá, mã ưu đãi, và chiến dịch marketing gắn với giao dịch bán vé** — tương tự cách CGV vận hành *Happy Wednesday*, *Culture Day*, voucher sinh nhật, hay Galaxy Cinema vận hành *combo khuyến mãi bắp nước*, *flash sale theo phim*.

Promotion Service **không phải** là nơi lưu trữ điểm thưởng hay xếp hạng thành viên — đó là trách nhiệm của `score-service`. Promotion chỉ **sử dụng kết quả** (hạng thành viên, số dư điểm) do `score-service` cung cấp để quyết định mức giảm giá, và **yêu cầu** `score-service` trừ/hoàn điểm khi khách dùng điểm đổi ưu đãi — thông qua API hợp đồng rõ ràng (xem mục 52).

---

## 02. Business Vision

> "Promotion Service là bộ não thương mại (commercial brain) đứng sau mỗi giao dịch — quyết định khách hàng trả bao nhiêu tiền và doanh nghiệp giữ lại bao nhiêu biên lợi nhuận. Promotion không đo lường khách hàng — nó **hành động dựa trên** những gì Score đã đo lường."

- Tăng tỷ lệ chuyển đổi đặt vé qua ưu đãi đúng thời điểm, đúng đối tượng.
- Cho phép Marketing tự cấu hình chiến dịch (self-service campaign builder) mà không cần đội kỹ thuật.
- Không thất thoát doanh thu do lỗi cấu hình, gian lận, hoặc cộng dồn sai.
- Tuân thủ pháp luật Việt Nam về khuyến mại (Nghị định 81/2018/NĐ-CP, Thông tư 39/2025/TT-BCT).
- Là nguồn dữ liệu sạch cho `analytics-service` đo ROI từng chiến dịch.

---

## 03. Goals

1. Cung cấp engine xử lý **Campaign, Coupon, Voucher, Discount Rule** thống nhất.
2. Đảm bảo tính toàn vẹn giao dịch giữa Promotion – Booking – Payment (không double-apply, không vượt ngân sách).
3. Real-time eligibility check với độ trễ p95 < 150ms.
4. Cấu hình rule linh hoạt không cần deploy code.
5. Chống gian lận khuyến mãi (coupon abuse, bot mass-redeem).
6. Audit trail đầy đủ phục vụ đối soát tài chính.
7. **Tích hợp chặt chẽ nhưng tách bạch rõ ràng** với `score-service` (điểm/hạng), `movie-service` (phim/suất chiếu), `user-service` (định danh khách hàng), `payment-service` (thanh toán), `booking-service` (giỏ vé).
8. Tuân thủ đầy đủ quy định pháp luật về khuyến mại tại Việt Nam.

---

## 04. Scope

> **Ranh giới implementation hiện tại (ISSUE-003/004):** checkout dùng mã và chỉ
> reserve một `COUPON` hoặc `VOUCHER` trên mỗi order/booking. Các API đồng bộ của
> Booking/Payment là nguồn sự thật; Promotion publish lifecycle event bằng outbox
> nhưng chưa consume event thay đổi reservation. Automatic rule discovery,
> multi-benefit stacking và point saga vẫn là roadmap, không được xem là logic đã
> chạy production chỉ vì có bảng/configuration admin.

Promotion Service chịu trách nhiệm:
- Vòng đời **Campaign** (khởi tạo → duyệt → kích hoạt → tạm dừng → kết thúc → báo cáo).
- **Coupon Code** (mã dùng chung, mã cá nhân hoá, mã một lần).
- **Voucher** (giá trị cố định/phần trăm, vé tặng, combo tặng hoặc gắn với hạng thành viên do score-service cung cấp).
- **Discount Rule tự động** (Happy Wednesday, Culture Day, giá vé theo khung giờ).
- **Ánh xạ hạng thành viên → mức ưu đãi** (`tier_benefit_mapping`): Promotion sở hữu bảng "hạng VIP được giảm 10%" nhưng **không** sở hữu việc "user X có phải VIP không" — đó là câu trả lời từ score-service.
- **Eligibility Engine**, **Stacking/Priority/Conflict Resolution Engine**.
- **Redemption & Reservation** trong lúc thanh toán.
- **Yêu cầu** score-service trừ/hoàn điểm khi khách dùng điểm quy đổi ưu đãi (Promotion không tự trừ).
- Tuân thủ và validate trần pháp lý khuyến mại trước khi Campaign được duyệt.

---

## 05. Out Of Scope — Ranh giới rõ ràng với từng service

| Không thuộc Promotion | Thuộc về | Cách Promotion tương tác |
|---|---|---|
| Tính điểm, lưu số dư điểm, tính hạng thành viên, lịch sử điểm | **score-service** | Gọi Internal API `GET /internal/score/users/{id}/tier`, `POST /internal/score/points/deduct`, nghe event `score.tier.upgraded` |
| Sơ đồ ghế, suất chiếu, giữ ghế | **booking-service** | Nhận `cartSnapshot` qua request, không truy vấn ngược |
| Thông tin phim, thể loại, định dạng (2D/3D/IMAX), rạp | **movie-service** | Gọi API `GET /internal/movies/{id}` hoặc cache dữ liệu định kỳ (xem mục 53) |
| Xác thực người dùng, hồ sơ cá nhân, ngày sinh, KYC | **user-service** | Nhận `userId` đã xác thực từ Gateway; nghe event `user.birthday.today`, `user.registered` |
| Xử lý thanh toán thực, đối soát ngân hàng | **payment-service** | Chỉ trả `finalAmount` + `appliedPromotionIds`; nghe `payment.completed/failed` |
| Quản lý đối tác tài trợ và quyết toán chi phí đồng tài trợ | **Finance/Accounting ngoài Promotion Service** | Không thuộc runtime khuyến mãi hiện tại |
| Gửi email/SMS/push | **notification-service** | Chỉ phát event `promotion.voucher.issued`, không tự gửi |
| Badge, achievement, gamification | **score-service** | Không liên quan đến Promotion |
| Phê duyệt pháp lý với Sở Công Thương (nộp hồ sơ) | **Legal/Compliance team (ngoài hệ thống)** | Promotion chỉ **chặn kỹ thuật** nếu campaign vi phạm trần luật, không tự nộp hồ sơ |

---

## 06. Business Domain

| Nhóm nghiệp vụ | Sở hữu bởi Promotion? | Ví dụ thực tế |
|---|---|---|
| Ưu đãi theo lịch cố định | ✅ Có | Happy Wednesday, Culture Day |
| Ưu đãi theo hạng thành viên (mức giảm) | ✅ Có (mapping), ❌ không sở hữu việc xác định hạng | U22/Member/VIP/VVIP → % giảm |
| Chương trình tích điểm (số dư, lịch sử) | ❌ Không — thuộc score-service | 1 điểm = 1.000 VNĐ |
| Đổi điểm lấy voucher (hành động thương mại) | ✅ Có (Promotion phát voucher), phối hợp trừ điểm qua score-service | — |
| Vé tặng sinh nhật | ✅ Có (phát voucher), trigger từ user-service | Vé 0đ sinh nhật |
| Mã giảm giá công khai của hệ thống | ✅ Có | Mã dùng chung theo campaign |
| Combo & Upsell | ✅ Có | Combo bắp nước kèm vé |
| Chiến dịch theo phim/nhà phát hành | ✅ Có (rule), dữ liệu phim từ movie-service | Ưu đãi đặt vé sớm phim bom tấn |
| Chương trình B2B | ✅ Có | Vé tập thể công ty |

---

## 07. Ubiquitous Language

| Thuật ngữ | Định nghĩa | Sở hữu bởi |
|---|---|---|
| **Campaign** | Chiến dịch khuyến mãi tổng thể | Promotion |
| **Coupon** | Mã khuyến mãi dạng chuỗi ký tự | Promotion |
| **Voucher** | Đơn vị giá trị đã phát hành cho một khách hàng cụ thể | Promotion |
| **Discount Rule** | Quy tắc giảm giá tự động không cần mã | Promotion |
| **Eligibility** | Tập điều kiện áp dụng khuyến mãi | Promotion |
| **Redemption** | Hành động sử dụng coupon/voucher | Promotion |
| **Reservation (Hold)** | Trạng thái tạm giữ khuyến mãi khi đang thanh toán | Promotion |
| **Stacking** | Áp dụng đồng thời nhiều khuyến mãi | Promotion |
| **Tier (Hạng thành viên)** | Cấp bậc thành viên — **giá trị** do score-service xác định | score-service (nguồn), Promotion (tiêu thụ) |
| **Point/Loyalty Point** | Điểm tích lũy — **toàn bộ vòng đời** thuộc score-service | score-service |
| **Tier Benefit Mapping** | Bảng ánh xạ "hạng X → giảm Y%" | Promotion |
| **Budget Cap** | Ngân sách tối đa của Campaign | Promotion |
| **Promotional Discount Ceiling** | Trần giảm giá tối đa 50% theo Nghị định 81/2018 | Promotion (validate), Legal (chịu trách nhiệm pháp lý) |

---

## 08. Domain Model

```
Campaign (1) ── (N) CampaignRule
Campaign (1) ── (N) Coupon
Campaign (1) ── (N) Voucher
Campaign (1) ── (1) Budget
CampaignRule (1) ── (N) EligibilityCondition
Coupon (1) ── (N) CouponRedemption
Voucher (1) ── (N) VoucherRedemption
TierBenefitMapping (N) ── (1) [External: score-service.Tier]   ◄── chỉ tham chiếu, không sở hữu
Order (1) ── (N) AppliedPromotion
AppliedPromotion (N) ── (1) Coupon/Voucher/DiscountRule

--- Các entity KHÔNG thuộc Promotion (tham chiếu bằng ID, không JOIN được) ---
[External] User (score-service/user-service sở hữu)
[External] PointWallet (score-service sở hữu)
[External] Movie/Showtime (movie-service sở hữu)
```

**Aggregate gốc của Promotion:** `Campaign`, `Coupon`, `Voucher`, `PromotionReservation`, `TierBenefitMapping`.
**Không có** `PointWallet Aggregate`, không có `User Aggregate` trong Promotion.

---

## 09. Bounded Context

Promotion Service là một Bounded Context độc lập, giao tiếp qua **Anti-Corruption Layer (ACL)**:

```
┌─────────────────┐   tierLevel (cache 30p)   ┌──────────────────┐
│  score-service   │ ─────────────────────────▶│                  │
│  (source of      │◀───────────────────────── │                  │
│  truth: điểm,     │  deduct/refund point       │                  │
│  hạng thành viên) │  (sync API + async event)  │                  │
└──────────────────┘                            │                  │
┌──────────────────┐   movie/showtime info       │  PROMOTION       │
│  movie-service    │ ─────────────────────────▶│  SERVICE         │
└──────────────────┘                            │  (Bounded         │
┌──────────────────┐   userId, birthday event    │  Context độc     │
│  user-service     │ ─────────────────────────▶│  lập)            │
└──────────────────┘                            │                  │
┌──────────────────┐   cartSnapshot              │                  │
│  booking-service  │◀────────────────────────── │                  │
└──────────────────┘   finalAmount + promoIds    │                  │
┌──────────────────┐   payment.completed/failed  │                  │
│  payment-service  │ ─────────────────────────▶│                  │
└──────────────────┘                            └──────────────────┘
```

Nguyên tắc ACL: Promotion **dịch** dữ liệu ngoại lai (VD `tierLevel: "VVIP"`) thành khái niệm nội bộ (`discountPercent: 15`) qua `TierBenefitMapping`, không để mô hình dữ liệu ngoài xâm nhập trực tiếp vào domain model của mình.

---

## 10. Responsibilities

**Promotion Service PHẢI:**
- Tính toán số tiền giảm giá chính xác, deterministic.
- Xác thực coupon/voucher hợp lệ, còn hạn, còn lượt, đúng đối tượng.
- Giữ chỗ (reserve) khuyến mãi khi giỏ hàng được tạo.
- Xác nhận/hoàn tác khi thanh toán thành công/thất bại.
- **Gọi** score-service để trừ điểm khi khách dùng điểm đổi ưu đãi, **gọi** để hoàn điểm khi rollback.
- Phát sự kiện cho Notification/Analytics khi có redemption.
- Bảo vệ ngân sách Campaign, validate trần pháp lý trước khi kích hoạt.

**Promotion Service KHÔNG:**
- Không xác thực người dùng.
- Không giữ ghế/suất chiếu.
- Không xử lý tiền thật.
- **Không lưu số dư điểm, không tự cộng/trừ điểm trong DB của mình.**
- **Không tự xác định hạng thành viên của user** — chỉ đọc kết quả từ score-service.
- **Không quyết định nội dung phim/suất chiếu** — chỉ đọc để làm điều kiện rule.

---

## 11. Architecture

```
                        ┌─────────────────────┐
                        │     API Gateway      │
                        └──────────┬───────────┘
                                   │
     ┌──────────────┬──────────────┼──────────────┬───────────────┐
     │               │              │              │               │
┌────▼─────┐  ┌──────▼──────┐ ┌────▼────┐  ┌──────▼──────┐ ┌──────▼──────┐
│ booking- │  │   user-      │ │ score-  │  │   movie-     │ │  payment-    │
│ service  │  │   service    │ │ service │  │   service    │ │  service     │
└────┬─────┘  └──────┬──────┘ └────┬────┘  └──────┬──────┘ └──────┬──────┘
     │ reserve()      │ event        │ tier/point   │ movie info    │ event
     │                │              │ API+event    │ (cache)       │
     ▼                ▼              ▼              ▼               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        PROMOTION SERVICE                              │
│  ┌───────────┐┌───────────┐┌───────────┐┌───────────┐┌─────────────┐ │
│  │ Campaign  ││  Coupon   ││  Voucher  ││ Discount  ││ TierBenefit  │ │
│  │  Engine   ││  Engine   ││  Engine   ││  Engine   ││   Mapper     │ │
│  └───────────┘└───────────┘└───────────┘└───────────┘└─────────────┘ │
│  ┌───────────┐┌───────────┐┌────────────┐┌────────────────────────┐ │
│  │   Rule    ││ Priority/ ││ Eligibility││   ACL / Anti-Corruption │ │
│  │  Engine   ││ Conflict  ││  Validator ││   Layer (score/movie/   │ │
│  │           ││ Resolution││            ││   user client adapters) │ │
│  └───────────┘└───────────┘└────────────┘└────────────────────────┘ │
└──────────┬──────────────────────────┬────────────────────────────────┘
           │                          │
 ┌─────────▼────────┐        ┌────────▼──────────┐
 │  MySQL (RW)       │        │ Redis (Cache/Lock/  │
 │  Promotion DB only│        │ Reservation/TierCache)│
 │  (KHÔNG có bảng   │        └────────────────────┘
 │  point/tier gốc)  │
 └───────────────────┘
           │
 ┌─────────▼────────┐
 │   Kafka Broker    │──▶ notification-service, analytics-service,
 └────────────────────┘   score-service (điểm/hạng), payment-service
```

**Stack:** Spring Boot/NestJS, MySQL (ACID), Redis (cache/lock), Kafka (event), rule engine dạng JSON/Drools.

---

## 12. Internal Modules

```
promotion-service/
├── campaign-module/          # CRUD & lifecycle campaign
├── coupon-module/            # Sinh mã, validate, redeem coupon
├── voucher-module/           # Phát hành & quản lý voucher
├── discount-rule-module/     # Quy tắc giảm giá tự động
├── tier-benefit-module/      # CHỈ ánh xạ hạng → % giảm, KHÔNG lưu ai thuộc hạng nào
├── eligibility-module/       # Kiểm tra điều kiện áp dụng
├── priority-conflict-module/ # Ưu tiên & xử lý xung đột
├── reservation-module/       # Giữ chỗ khuyến mãi khi checkout
├── redemption-module/        # Xác nhận sử dụng khuyến mãi
├── budget-module/            # Theo dõi & giới hạn ngân sách
├── legal-compliance-module/  # Validate trần giảm giá pháp lý (mục 51)
├── fraud-detection-module/   # Phát hiện gian lận
├── reporting-module/         # Số liệu hiệu quả campaign
├── acl/
│   ├── score-service-client/    # Adapter gọi score-service (tier/point)
│   ├── movie-service-client/    # Adapter gọi movie-service
│   └── user-service-client/     # Adapter gọi user-service
├── admin-api/
├── customer-api/
├── internal-api/
└── scheduler/
```

---

## 13. Database Design

**MySQL — chỉ chứa dữ liệu Promotion sở hữu, KHÔNG có bảng điểm/hạng gốc:**

```sql
campaigns (
  id UUID PK, name, code, type, status,
  start_date, end_date, budget_total, budget_used,
  discount_type, discount_value,          -- dùng để validate trần 50%
  legal_notification_ref VARCHAR,          -- số hồ sơ thông báo Sở Công Thương (nếu có)
  priority INT, applies_to JSON, created_by, approved_by, created_at
)

campaign_rules (
  id UUID PK, campaign_id FK, rule_type,
  conditions JSON,        -- ngày, rạp, phim (movieId ref), kênh, min_amount, tierCode...
  action_type, action_value NUMERIC,
  stackable BOOLEAN, priority INT
)

coupons (
  id UUID PK, campaign_id FK, code VARCHAR UNIQUE,
  type, max_redemptions INT, redemptions_count INT,
  max_redemptions_per_user INT,
  valid_from, valid_to, status
)

coupon_redemptions (
  id UUID PK, coupon_id FK, user_id, order_id,
  redeemed_at, amount_discounted, status
)

vouchers (
  id UUID PK, user_id, voucher_type, value,
  source,                   -- BIRTHDAY, TIER_UPGRADE, COMPENSATION, POINT_REDEEM
  issued_at, expires_at, status
)

tier_benefit_mapping (
  id UUID PK, tier_code VARCHAR,   -- 'U22','MEMBER','VIP','VVIP' — CHỈ LÀ CODE tham chiếu
  discount_percent NUMERIC, point_earn_multiplier_display NUMERIC, -- hiển thị, KHÔNG dùng để tính điểm thật
  effective_from, effective_to
)

promotion_reservations (
  id UUID PK, order_id, user_id, promotion_ref_id,
  promotion_type, amount_held, status, expires_at,
  point_deduction_request_id VARCHAR NULL  -- tham chiếu request đã gửi score-service
)

budget_ledger (
  id UUID PK, campaign_id, delta_amount, order_id, created_at
)

audit_logs (
  id UUID PK, actor, action, entity_type, entity_id,
  before JSON, after JSON, created_at
)

-- KHÔNG CÓ: user_point_wallets, point_transactions, membership_tiers (thuộc score-service)
```

**Redis:**
- `promo:reservation:{orderId}` — giữ chỗ khuyến mãi TTL ngắn.
- `promo:coupon:usage:{couponCode}` — bộ đếm nhanh lượt dùng.
- `promo:campaign:active` — cache campaign đang chạy.
- `acl:tier:{userId}` — **cache tạm** (TTL 15–30 phút) kết quả gọi score-service, KHÔNG phải nguồn sự thật.
- `acl:movie:{movieId}` — cache metadata phim từ movie-service.

---

## 14. Aggregate Design

- **Campaign Aggregate**: root = `Campaign`, chứa `CampaignRule`, `Budget`.
- **Coupon Aggregate**: root = `Coupon`, chứa `CouponRedemption`. Invariant: `redemptions_count <= max_redemptions`.
- **Voucher Aggregate**: root = `Voucher`, vòng đời độc lập.
- **PromotionReservation Aggregate**: vòng đời ngắn hạn, tự hết hạn.
- **TierBenefitMapping Aggregate**: cấu hình tĩnh, thay đổi hiếm (chỉ Admin sửa), không liên quan số dư điểm thực tế.

> **Đã loại bỏ hoàn toàn** `PointWallet Aggregate`/`User Aggregate` khỏi Promotion (có ở v1, là lỗi thiết kế đã sửa).

---

## 15. Rule Engine

```json
{
  "ruleId": "happy-wednesday-2026",
  "conditions": {
    "dayOfWeek": ["WEDNESDAY"],
    "seatType": ["STANDARD"],
    "format": ["2D"],
    "excludeRoomType": ["IMAX", "4DX", "SCREENX"],
    "excludeDates": ["2026-01-01", "2026-04-30"],
    "movieId": null,
    "requiredTierCode": null
  },
  "action": { "type": "FIXED_PRICE", "value": 55000 },
  "priority": 10,
  "stackableWith": ["TIER_DISCOUNT"],
  "notStackableWith": ["OTHER_FIXED_PRICE_RULE"],
  "legalDiscountCeilingCheck": true
}
```

`requiredTierCode` chỉ là **điều kiện tham chiếu** — giá trị thật của user được lấy real-time/cache từ score-service qua ACL, Rule Engine không lưu trữ ai thuộc hạng nào.

---

## 16. Campaign Engine

```
DRAFT → PENDING_APPROVAL → LEGAL_COMPLIANCE_CHECK → SCHEDULED → ACTIVE → PAUSED ⇄ ACTIVE → COMPLETED / CANCELLED
```

- **LEGAL_COMPLIANCE_CHECK** (mới bổ sung): hệ thống tự động kiểm tra `discount_value <= 50%` giá trị hàng hoá/dịch vụ (Nghị định 81/2018/NĐ-CP, Thông tư 39/2025/TT-BCT), và cảnh báo nếu tổng số ngày giảm giá trong năm cho cùng 1 loại vé vượt 120 ngày. Nếu vi phạm → campaign **không thể chuyển sang SCHEDULED**, cần override thủ công có ghi chú lý do (VD thuộc diện miễn trần theo Điều 8/10/11 NĐ 81/2018) bởi role `LEGAL_COMPLIANCE`.
- **PENDING_APPROVAL**: 4-eyes principle — người tạo ≠ người duyệt.
- Campaign tự động **auto-pause** khi `budget_used >= budget_total`.

---

## 17. Coupon Engine

- Sinh mã: dùng chung, cá nhân hoá theo batch, một lần dùng (single-use).
- Thuật toán tránh trùng: base62 + checksum.
- Giới hạn: `max_redemptions`, `max_redemptions_per_user` (thường = 1).
- Import hàng loạt từ CSV cho campaign.
- Validate real-time: hạn dùng, lượt còn, campaign ACTIVE, điều kiện giỏ hàng.

---

## 18. Voucher Engine

Nguồn phát hành voucher:
- **Sinh nhật**: trigger từ event `user.birthday.today` (user-service phát ra) → Promotion tự phát vé 0đ.
- **Nâng hạng thành viên**: trigger từ event `score.tier.upgraded` (**score-service phát ra**, không phải Promotion tự tính) → Promotion phát voucher chào mừng tương ứng `tier_benefit_mapping`.
- **Đền bù dịch vụ**: CSKH phát voucher thủ công.
- **Đổi điểm lấy voucher**: khách yêu cầu qua Customer API của **score-service** (score-service biết số dư điểm); score-service gọi Internal API của Promotion `POST /internal/promotions/vouchers/issue-from-point-exchange` để Promotion phát voucher, đồng thời score-service tự trừ điểm bên phía nó. **Promotion không chủ động trừ điểm.**

---

## 19. Discount Engine

- Giảm giá theo khung giờ chiếu.
- Giảm giá theo hạng thành viên: Promotion tra `tier_benefit_mapping` theo `tierCode` lấy từ ACL cache (score-service), **không tự suy luận** hạng.
- Giảm giá theo đối tượng đặc biệt (học sinh/sinh viên, người cao tuổi, trẻ em — `requiresVerification: true`, xác minh thủ công tại quầy, không do hệ thống tự động xác thực giấy tờ).
- Giảm giá theo combo vé + bắp nước.

---

## 20. Stack Engine

```
1. Base Price (từ booking-service)
2. Discount Engine (tự động: tier, khung giờ, đối tượng)
3. Coupon Engine (mã khách nhập)
4. Voucher Engine (voucher có sẵn trong ví — voucher đã tồn tại trong Promotion DB)
5. Point Redemption Request (nếu khách chọn dùng điểm → Promotion GỌI score-service để xác nhận đủ điểm & giữ tạm, KHÔNG tự trừ)
= Final Price
```

---

## 21. Priority Engine

```
1. Voucher cá nhân hoá đã cam kết (VD vé sinh nhật) — priority cao nhất
2. Campaign giới hạn thời gian (Flash Sale)
3. Coupon nhập tay
4. Discount tự động theo hạng thành viên
5. Giá vé chuẩn
```

Cùng priority → chọn phương án giảm nhiều tiền hơn cho khách (customer-friendly default), trừ khi campaign đánh dấu `forced: true`.

---

## 22. Conflict Resolution

- **Xung đột loại trừ**: xử lý theo Priority Engine.
- **Xung đột ngân sách**: optimistic locking + Redis atomic decrement.
- **Xung đột lượt dùng coupon**: Redis distributed lock hoặc `SELECT FOR UPDATE`.
- **Xung đột giữa hai nguồn "miễn phí vé"**: chỉ 1 được áp dụng.
- **Xung đột giữa Point Redemption và Coupon giảm 100%**: không cho dùng điểm nếu đơn hàng đã về 0đ (tránh lãng phí điểm không cần thiết — cảnh báo UI cho khách).

---

## 23. Eligibility Validation

1. Trạng thái campaign/coupon/voucher (ACTIVE, còn hạn, còn lượt).
2. Đối tượng người dùng: **hạng thành viên (đọc từ ACL/score-service)**, độ tuổi (đọc từ user-service nếu cần), khách mới/cũ.
3. Điều kiện giỏ hàng: giá trị tối thiểu, số lượng vé, loại vé/định dạng.
4. Điều kiện suất chiếu: rạp, phim (đọc từ movie-service), khung giờ, blackout date.
5. Điều kiện kênh & thanh toán.
6. Kiểm tra gian lận.
7. **Kiểm tra trần pháp lý** (không vượt 50% giá trị vé tại thời điểm áp dụng).

Kết quả: `ELIGIBLE`, `NOT_ELIGIBLE(reason)`.

---

## 24. Redemption

```
1. Khách chọn ghế → Booking tạo Order (PENDING)
2. Khách nhập coupon/chọn voucher/chọn dùng điểm
   → Promotion.reserve():
       - Giữ chỗ coupon/voucher nội bộ (HELD)
       - Nếu có dùng điểm: gọi score-service POST /points/hold (giữ tạm, KHÔNG trừ hẳn)
3. Khách thanh toán → Payment xử lý
4a. Payment thành công → Promotion.confirm():
       - Coupon/voucher: HELD → CONFIRMED, trừ lượt/ngân sách thật
       - Nếu có point hold: gọi score-service POST /points/commit (score-service trừ điểm thật)
4b. Payment thất bại/timeout → Promotion.release():
       - Giải phóng lượt coupon/voucher đã giữ
       - Nếu có point hold: gọi score-service POST /points/release (score-service huỷ giữ, không trừ)
5. Sau CONFIRMED → phát event promotion.redeemed (score-service tự nghe để CỘNG điểm tích lũy cho phần tiền thực trả — Promotion không tính hộ)
```

---

## 25. Release, Cancel và Compensation

Trước confirm: payment thất bại/timeout dùng `RELEASED`; booking bị hủy dùng `CANCELLED`; quá TTL do hệ thống tự chuyển `EXPIRED`. Sau `COMPLETED` không rollback reservation/redemption mà phải dùng refund hoặc compensation ledger riêng.

Mọi transition phải **idempotent**. Nếu có point-hold đã gửi sang score-service, **bắt buộc gọi compensating API `release`** — đây là điểm dễ gây rò rỉ dữ liệu giữa 2 service nếu thiếu retry/DLQ (xem mục 47, 52).

---

## 26. Reservation

- TTL đồng bộ với TTL giữ ghế của booking-service.
- Dùng Redis lock atomic cho `couponCode`/`campaignId`.
- Background job quét Reservation quá hạn để chuyển sang `EXPIRED` (self-healing), **bao gồm cả việc gọi `release` sang score-service** nếu có point-hold treo.

---

## 27. Event Driven

**Consume (kiến trúc mục tiêu, chưa bật trong ISSUE-004):**
- `booking.order.created` → khởi tạo eligibility check.
- `payment.completed` → confirm redemption.
- `payment.failed` → release active reservation; `booking.order.cancelled` → cancel active reservation. Giao dịch đã completed đi qua refund/compensation.
- `user.registered` → phát voucher chào mừng.
- `user.birthday.today` → phát voucher sinh nhật.
- `score.tier.upgraded` (**từ score-service**) → phát voucher/benefit hạng mới, invalidate ACL cache.
- `movie.updated` (từ movie-service) → invalidate cache metadata phim dùng trong rule.

Reservation Runtime hiện tại không consume `payment.completed`,
`payment.failed` hay `booking.order.cancelled`, vì chưa có producer contract và
inbox/idempotency ownership thống nhất. Booking/Payment gọi API canonical;
consumer chỉ được bật khi có versioned schema, inbox table, replay/DLQ và một
quy tắc single-writer rõ ràng.

**Produce:**
- `promotion.applied` — cho analytics-service.
- `promotion.redeemed` — **score-service nghe để tự cộng điểm** (Promotion không gọi API cộng điểm, chỉ phát sự kiện, tách rời trách nhiệm rõ ràng qua event thay vì lệnh trực tiếp).
- `promotion.campaign.budget_exhausted` — cảnh báo Marketing/Admin.
- `promotion.voucher.issued` — cho notification-service.
- `promotion.fraud.alert` — cho security/ops.

---

## 28. Kafka Events

| Topic | Partition Key | Mục đích |
|---|---|---|
| `promotion.events.applied` | `orderId` | Log mọi lần áp dụng khuyến mãi |
| `promotion.events.redeemed` | `orderId` | Xác nhận redeem cuối — **score-service subscribe để cộng điểm** |
| `promotion.events.rolledback` | `orderId` | Sự kiện hoàn tác |
| `promotion.campaign.lifecycle` | `campaignId` | Trạng thái campaign thay đổi |
| `promotion.voucher.issued` | `userId` | Voucher mới phát hành |
| `promotion.fraud.alerts` | `userId` | Cảnh báo gian lận |
| *(consume)* `score.tier.upgraded` | `userId` | score-service phát khi user đổi hạng |
| *(consume)* `user.birthday.today` | `userId` | user-service phát hằng ngày |

Dùng **Outbox Pattern** để đảm bảo nhất quán giữa ghi DB và publish Kafka.

---

## 29. API Design

RESTful cho CRUD/Admin; gRPC nội bộ cho Booking ↔ Promotion tốc độ cao. Response lỗi chuẩn hoá (xem mục 46).

---

## 30. Admin APIs

```
POST   /admin/campaigns
POST   /admin/campaigns/{id}/submit
POST   /admin/campaigns/{id}/legal-check        # chạy validate trần pháp lý
POST   /admin/campaigns/{id}/approve
POST   /admin/campaigns/{id}/pause / resume
GET    /admin/campaigns/{id}/report

POST   /admin/coupons/generate
GET    /admin/coupons/{code}/usage
POST   /admin/coupons/{code}/revoke

POST   /admin/vouchers/issue                     # CSKH phát thủ công
GET    /admin/vouchers?userId=

PUT    /admin/tier-benefit-mapping/{tierCode}    # CHỈ sửa % giảm theo hạng, KHÔNG sửa ai thuộc hạng nào
GET    /admin/budget/{campaignId}
```

---

## 31. Customer APIs

```
GET    /api/v1/promotions/available             Danh sách ưu đãi khả dụng
POST   /api/v1/promotions/validate                Validate coupon trước khi apply
POST   /api/v1/promotions/apply                   Áp dụng khuyến mãi vào order (reserve)
DELETE /api/v1/promotions/apply/{orderId}          Gỡ khuyến mãi
GET    /api/v1/vouchers/my-wallet                  Ví voucher (Promotion sở hữu voucher)
```

> **Đã loại bỏ khỏi Promotion**: `/points/balance`, `/points/history`, `/points/redeem` — các endpoint này **thuộc Customer API của score-service**. Nếu cần hiển thị gộp trên 1 màn hình app, tầng **BFF/API Gateway** sẽ gọi song song cả 2 service, Promotion không proxy hộ để tránh coupling.

---

## 32. Internal APIs

```
# Promotion cung cấp cho service khác
POST   /internal/runtime/validate                              (booking-service preview, không giữ)
POST   /internal/reservations                                  (booking-service reserve)
GET    /internal/reservations/{reservationId}                  (booking/payment-service đọc)
POST   /internal/reservations/{reservationId}/confirm          (payment-service gọi)
POST   /internal/reservations/{reservationId}/release          (payment-service gọi khi thất bại)
POST   /internal/reservations/{reservationId}/cancel           (booking-service gọi khi hủy)
POST   /internal/reservations/{reservationId}/refresh          (booking-service gia hạn)
GET    /api/admin/reservations                                 (admin/operations tra cứu)
POST   /internal/promotions/vouchers/issue-from-point-exchange   (score-service gọi khi khách đổi điểm)

# Promotion gọi sang service khác (ACL client)
GET    score-service:/internal/score/users/{userId}/tier
POST   score-service:/internal/score/points/hold        {userId, amount, orderId}
POST   score-service:/internal/score/points/commit       {holdId}
POST   score-service:/internal/score/points/release       {holdId}
GET    movie-service:/internal/movies/{movieId}
GET    user-service:/internal/users/{userId}/basic-profile   (chỉ độ tuổi/ngày đăng ký nếu cần điều kiện)
```

Bảo vệ bằng **mTLS + service token**, không public qua Gateway.

---

## 33. Scheduled Jobs

| Job | Tần suất | Chức năng |
|---|---|---|
| `ExpireReservationsJob` | mỗi 1 phút | Chuyển reservation quá hạn sang `EXPIRED` (kể cả release point-hold) |
| `ExpireCouponsVouchersJob` | mỗi giờ | Cập nhật status EXPIRED |
| `CampaignAutoStartJob` | mỗi 5 phút | SCHEDULED → ACTIVE |
| `CampaignAutoEndJob` | mỗi 5 phút | ACTIVE → COMPLETED khi hết hạn/ngân sách |
| `BirthdayVoucherJob` | hằng ngày 06:00 | Phát vé sinh nhật (trigger từ event, job là cơ chế dự phòng nếu event lỡ) |
| `BudgetReconciliationJob` | hằng đêm | Đối soát `budget_used` Redis vs DB |
| `FraudScanJob` | mỗi 15 phút | Quét pattern redeem bất thường |
| `TierCacheRefreshJob` | mỗi 15 phút | Làm mới cache `acl:tier:{userId}` chủ động (giảm phụ thuộc real-time call) |
| `CampaignReportAggregationJob` | hằng đêm | Tổng hợp số liệu cho analytics-service |

> **Đã loại bỏ**: `PointExpiryJob`, `TierRecalculationJob` — thuộc scheduler của **score-service**.

---

## 34. Redis Strategy

- Cache-aside cho campaign ACTIVE, coupon metadata.
- Distributed Lock cho redeem coupon giới hạn thấp.
- Atomic Counter cho `budget_used`, `redemptions_count`.
- Sorted Set cho hàng đợi Flash Sale.
- **ACL Cache** (`acl:tier:{userId}`, `acl:movie:{movieId}`) — TTL ngắn, có cơ chế invalidate qua event, luôn coi là **dữ liệu tạm thời có thể stale**, không phải nguồn sự thật.

---

## 35. Cache

| Dữ liệu | Chiến lược | TTL | Nguồn sự thật |
|---|---|---|---|
| Active campaigns | Cache-aside | 5 phút | Promotion |
| Tier benefit mapping | Cache-aside | 30 phút | Promotion |
| **Tier hiện tại của user** | ACL cache, invalidate on event | 15–30 phút | **score-service** |
| Coupon metadata | Cache-aside | 15 phút | Promotion |
| Movie/showtime metadata | ACL cache | 30 phút | **movie-service** |

---

## 36. Concurrency

- Redeem coupon giới hạn số lượng: optimistic/pessimistic lock hoặc Redis atomic decrement.
- Ngân sách campaign: atomic decrement + reconciliation.
- Idempotency bắt buộc cho `reserve`, `confirm`, `release`, `cancel`, `refresh`.
- **Point hold/commit/release**: đây là **giao dịch phân tán 2 service** (Promotion + score-service) — cần Saga pattern với compensating action rõ ràng, không dùng 2-phase-commit truyền thống (không phù hợp microservices).

---

## 37. Security

- Customer API: JWT từ auth-service.
- Admin API: RBAC (mục 38).
- Coupon code: chống đoán mã, rate-limit theo IP/user.
- Voucher: gắn chặt `userId`, kiểm tra chủ sở hữu khi redeem.
- Internal API: mTLS giữa các service.
- Input validation cho `conditions JSON` (whitelist field).
- Không log PII nhạy cảm trong audit log.

---

## 38. Authorization

| Vai trò | Quyền hạn |
|---|---|
| `MARKETING_STAFF` | Tạo campaign (DRAFT), sinh coupon nháp |
| `MARKETING_MANAGER` | Duyệt campaign, chỉnh ngân sách |
| `LEGAL_COMPLIANCE` | Override cảnh báo trần pháp lý (có ghi lý do) |
| `CSKH_AGENT` | Phát voucher đền bù thủ công (giới hạn giá trị) |
| `FINANCE` | Xem redemption ledger và báo cáo ngân sách campaign |
| `SYSTEM_ADMIN` | Toàn quyền, cấu hình tier-benefit-mapping, rule engine |
| `CUSTOMER` | Xem/dùng khuyến mãi của chính mình |

4-eyes principle cho campaign ngân sách lớn.

---

## 39. Performance

- SLA: `validate`/`apply` p95 < 150ms, p99 < 300ms.
- Tránh N+1 query, preload rule/campaign vào cache.
- Read replica MySQL cho API đọc.
- Gọi ACL sang score-service/movie-service **phải có cache + timeout ngắn (≤ 100ms) + fallback**, không để một service ngoài làm chậm toàn bộ luồng đặt vé.
- Load test kịch bản mở bán phim bom tấn (spike 20–50 lần).

---

## 40. Observability

- Distributed Tracing (OpenTelemetry) xuyên suốt Booking → Promotion → score-service/movie-service → Payment.
- Structured logging JSON (`traceId`, `orderId`, `campaignId`, `couponCode`).
- Dashboard riêng theo dõi **tỷ lệ gọi ACL thành công/timeout sang score-service** — vì đây là điểm phụ thuộc chéo mới giữa 2 service.

---

## 41. Monitoring

- Campaign sắp hết ngân sách (< 10%).
- Tỷ lệ lỗi `apply`/`redeem` vượt ngưỡng.
- Redis lock timeout tăng bất thường.
- Reservation `ACTIVE` không được confirm/release/cancel trước deadline.
- **Point hold không được commit/release trong X phút** (rủi ro điểm bị "kẹt" phía score-service) → cảnh báo riêng, vì đây là lỗi liên service dễ bị bỏ sót.
- Kafka outbox publish failure/stale lease; consumer lag chỉ áp dụng cho các
  consumer mục tiêu đã thực sự được bật.

---

## 42. Metrics

| Metric | Ý nghĩa |
|---|---|
| `promotion_apply_success_rate` | Tỷ lệ áp dụng thành công |
| `promotion_apply_latency_p95/p99` | Độ trễ xử lý |
| `campaign_budget_utilization` | % ngân sách đã dùng |
| `coupon_redemption_rate` | Lượt dùng / số mã phát hành |
| `voucher_expiry_waste_rate` | Tỷ lệ voucher hết hạn không dùng |
| `acl_score_service_call_success_rate` | Độ ổn định tích hợp với score-service |
| `acl_score_service_call_latency_p95` | Độ trễ gọi score-service |
| `point_hold_stuck_count` | Số point-hold chưa được commit/release đúng hạn |
| `fraud_flagged_transactions_count` | Giao dịch bị gắn cờ gian lận |
| `legal_ceiling_violation_blocked_count` | Số campaign bị chặn do vượt trần pháp lý |

---

## 43. Audit

Ghi `audit_logs` cho: tạo/sửa/duyệt/tạm dừng Campaign, override cảnh báo pháp lý (bắt buộc lý do), phát voucher thủ công, revoke coupon. Audit log immutable, phục vụ đối soát kế toán & tranh chấp khách hàng. **Không** ghi log điều chỉnh điểm — đó thuộc audit log của score-service.

---

## 44. Reporting

- Hiệu quả Campaign: doanh thu, số vé bán qua campaign, ngân sách đã dùng, ROI (phối hợp analytics-service).
- Đối soát tài chính: tổng giá trị voucher/coupon redeem theo ngày.
- Báo cáo gian lận.
- Báo cáo tuân thủ pháp lý: danh sách campaign, mức giảm, số ngày áp dụng trong năm theo từng loại vé (phục vụ kiểm tra nội bộ trước khi Sở Công Thương thanh tra).

> Báo cáo điểm thưởng (tổng điểm phát hành/đổi/hết hạn) **thuộc trách nhiệm báo cáo của score-service**, Promotion chỉ cung cấp dữ liệu `promotion.redeemed` làm đầu vào.

---

## 45. Analytics

- Funnel: `promotion viewed → applied → confirmed`.
- Phân khúc khách hàng phản ứng tốt với loại khuyến mãi nào (feed cho analytics-service).
- A/B test: gắn `experimentGroup` để so sánh hiệu quả biến thể ưu đãi.

---

## 46. Error Handling

```
COUPON_NOT_FOUND, COUPON_EXPIRED, COUPON_EXHAUSTED,
COUPON_ALREADY_USED_BY_USER, VOUCHER_NOT_OWNED, VOUCHER_EXPIRED,
CAMPAIGN_INACTIVE, BUDGET_EXHAUSTED, NOT_ELIGIBLE_TIER,
NOT_ELIGIBLE_CART, BLACKOUT_DATE, CONFLICTING_PROMOTIONS,
RESERVATION_EXPIRED, DUPLICATE_REQUEST,
LEGAL_DISCOUNT_CEILING_EXCEEDED,
SCORE_SERVICE_UNAVAILABLE,        -- fallback: cho phép áp dụng khuyến mãi KHÔNG cần điểm, chặn riêng phần dùng điểm
MOVIE_SERVICE_UNAVAILABLE          -- fallback: dùng cache cũ nếu còn hạn, nếu không thì bỏ qua rule cần dữ liệu phim
```

Nguyên tắc: nếu Promotion lỗi/timeout, `booking-service` vẫn cho đặt vé theo giá gốc (circuit breaker + fallback). Nếu **score-service** lỗi, Promotion vẫn cho áp dụng các khuyến mãi không liên quan điểm/hạng, chỉ chặn riêng phần phụ thuộc score-service.

---

## 47. Retry

- Gọi Booking/Payment ↔ Promotion: exponential backoff, tối đa 3 lần, kèm idempotency key.
- **Gọi Promotion ↔ score-service (point hold/commit/release)**: retry bắt buộc + Dead Letter Queue riêng, vì đây là dữ liệu tài chính (điểm) dễ gây khiếu nại khách hàng nếu kẹt.
- Kafka consumer tương lai: retry topic + DLQ, cảnh báo vận hành khi vào DLQ;
  runtime hiện tại dùng outbox producer ACK + exponential retry.
- Circuit Breaker giữa Promotion ↔ score-service/movie-service để tránh cascading failure.

---

## 48. Idempotency

- Mọi API thay đổi trạng thái bắt buộc nhận `orderId`/`idempotencyKey`.
- Lưu kết quả theo key trong Redis/DB ngắn hạn (24h).
- **Point hold/commit/release** phải idempotent ở cả 2 phía (Promotion và score-service) — dùng chung `holdId` làm khoá idempotency xuyên suốt.

---

## 49. High Availability

- Multi-instance stateless sau Load Balancer.
- PostgreSQL Primary-Replica, failover tự động.
- Redis Cluster/Sentinel.
- Kafka replication factor ≥ 3.
- **Graceful degradation khi score-service down**: Promotion tự động chuyển sang "chế độ hạn chế" — vẫn phục vụ Coupon/Voucher/Discount không cần tier, tạm khoá tính năng "dùng điểm đổi ưu đãi" và hiển thị thông báo rõ ràng cho khách thay vì lỗi toàn màn hình.
- Capacity plan riêng cho Flash Sale/mở bán phim bom tấn.

---

## 50. Future Roadmap

| Giai đoạn | Hạng mục |
|---|---|
| **Phase 1 (MVP)** | Coupon/Voucher dùng mã, validate điều kiện đã hỗ trợ, reserve/confirm/release/cancel/expire |
| **Phase 2** | Campaign Engine đầy đủ (approval + legal-check workflow), Voucher cá nhân hoá, budget cap, fraud detection cơ bản |
| **Phase 3** | Rule Engine tự cấu hình (self-service Marketing) và A/B testing |
| **Phase 4** | Cá nhân hoá khuyến mãi bằng ML (phối hợp analytics-service), dynamic pricing thử nghiệm |

---

## 51. Legal & Compliance (Bổ sung mới)

Căn cứ pháp lý áp dụng cho khuyến mại tại Việt Nam:
- **Luật Thương mại 2005** và **Nghị định 81/2018/NĐ-CP** quy định chi tiết hoạt động xúc tiến thương mại.
- **Thông tư 39/2025/TT-BCT** (hiệu lực từ 01/7/2025): mức giảm giá tối đa cho một sản phẩm/dịch vụ **không vượt quá 50%** giá trị ngay trước thời điểm khuyến mại; tổng giá trị khuyến mại trong một chương trình cũng không vượt quá 50% tổng giá trị hàng hoá/dịch vụ được khuyến mại.
- **Thời hạn áp dụng**: tổng thời gian giảm giá cho một loại vé/dịch vụ **không vượt quá 120 ngày/năm** (Điều 10, NĐ 81/2018), trừ các chương trình khuyến mại tập trung do Nhà nước tổ chức (VD giờ vàng mua sắm quốc gia) hoặc các trường hợp miễn trừ (hàng thanh lý, hàng sắp hết hạn...).
- **Thủ tục thông báo**: với các hình thức khuyến mại thuộc diện phải thông báo, hồ sơ phải gửi Sở Công Thương **tối thiểu 3 ngày làm việc** trước khi thực hiện (không cần chờ xác nhận, chỉ cần thông báo).
- **Hàng hoá/dịch vụ bị cấm dùng làm khuyến mại**: rượu, xổ số, thuốc lá, dược phẩm (trừ ngoại lệ) — không áp dụng trực tiếp cho vé xem phim nhưng liên quan nếu rạp có chương trình khuyến mại combo bắp nước có đồ uống có cồn.

**Trách nhiệm kỹ thuật của Promotion Service:**
1. `legal-compliance-module` validate tự động `discount_value <= 50%` trước khi campaign được duyệt.
2. Theo dõi **tổng số ngày áp dụng giảm giá trong năm theo từng loại vé** (`campaign_rules.conditions` gắn `productLine`) để cảnh báo khi gần chạm 120 ngày.
3. Lưu `legal_notification_ref` (số hồ sơ đã thông báo Sở Công Thương) trong bảng `campaigns` — do đội Legal/Marketing nhập, Promotion chỉ lưu trữ và bắt buộc có trước khi ACTIVE với các chương trình thuộc diện phải thông báo.
4. Cho phép **override có kiểm soát** (role `LEGAL_COMPLIANCE`, bắt buộc ghi lý do — VD chương trình thuộc diện miễn trừ) khi vượt trần nhưng vẫn hợp pháp.

> Đây là module dễ bị bỏ qua khi thiết kế kỹ thuật thuần tuý, nhưng là **rủi ro pháp lý và tài chính thực tế** với doanh nghiệp rạp chiếu phim tại Việt Nam — vi phạm trần khuyến mại có thể bị xử phạt hành chính.

---

## 52. Integration Contract với score-service (Bổ sung mới)

**Hợp đồng API (Consumer-Driven Contract — khuyến nghị dùng Pact để test):**

| Hướng | Endpoint/Event | Mục đích | SLA kỳ vọng |
|---|---|---|---|
| Promotion → score-service | `GET /internal/score/users/{userId}/tier` | Lấy hạng hiện tại | p99 < 80ms, cache 15–30p |
| Promotion → score-service | `POST /internal/score/points/hold` | Giữ tạm điểm khi khách chọn dùng điểm | Đồng bộ, timeout 500ms |
| Promotion → score-service | `POST /internal/score/points/commit` | Xác nhận trừ điểm thật | Bất đồng bộ có retry, DLQ |
| Promotion → score-service | `POST /internal/score/points/release` | Huỷ giữ điểm khi rollback | Bất đồng bộ có retry, DLQ |
| score-service → Promotion | Event `score.tier.upgraded` | Thông báo đổi hạng để phát voucher chào mừng | At-least-once |
| score-service → Promotion | `POST /internal/promotions/vouchers/issue-from-point-exchange` | Khách đổi điểm lấy voucher | Đồng bộ, timeout 1s |
| Promotion → score-service | Event `promotion.redeemed` (score-service subscribe) | Để score-service tự cộng điểm theo `finalAmount` | At-least-once |

**Nguyên tắc hợp đồng:**
- Promotion **không bao giờ** giả định cấu trúc nội bộ của score-service — chỉ dùng DTO đã thống nhất qua contract test.
- Khi score-service thay đổi API (breaking change), phải version hoá (`/v2/...`) và thông báo trước cho team Promotion — quản lý qua API Gateway/Service Mesh versioning.
- Mọi lời gọi chéo phải có **circuit breaker + fallback** như mô tả ở mục 46/49.

---

## 53. Integration với movie-service (Bổ sung mới)

Promotion cần các trường dữ liệu sau từ `movie-service` để Rule Engine hoạt động, nhưng **không sở hữu** và **không chỉnh sửa** chúng:

```
GET /internal/movies/{movieId}
→ { movieId, title, genre[], format[] (2D/3D/IMAX/4DX/ScreenX),
    releaseDate, distributor, isPremiere, cinemaIds[] }

GET /internal/showtimes/{showtimeId}
→ { showtimeId, movieId, cinemaId, roomType, startTime, isEarlyMorning, isLateNight }
```

- Dữ liệu này được **cache định kỳ** (`acl:movie:{movieId}`, TTL 30 phút) vì phim/suất chiếu ít thay đổi trong ngày, giảm tải gọi trực tiếp mỗi lần checkout.
- Khi `movie-service` phát event `movie.updated`/`showtime.updated`, Promotion invalidate cache tương ứng ngay để tránh áp dụng rule sai (VD phim đổi định dạng chiếu).
- Điều kiện theo **nhà phát hành phim (distributor)** được cấu hình qua `campaign_rules.conditions.movieId`/`distributor`; dữ liệu `distributor` lấy từ movie-service, không nhập tay trùng lặp.

---

## 54. Testing Strategy (Bổ sung mới)

| Loại test | Mục tiêu |
|---|---|
| Unit test | Logic Rule Engine, Stack/Priority/Conflict Resolution (nhiều kịch bản cộng dồn) |
| Integration test | Reserve → Confirm/Rollback với MySQL + Redis thật (testcontainers) |
| **Contract test (Pact)** | Đảm bảo hợp đồng API với score-service, movie-service, booking-service không bị phá vỡ khi từng team deploy độc lập |
| Concurrency/Chaos test | Mô phỏng nhiều request đồng thời redeem coupon giới hạn thấp, kiểm tra không vượt `max_redemptions` |
| Load test | Mô phỏng spike traffic mở bán phim bom tấn (20–50x bình thường) |
| Fault injection test | Ngắt kết nối tạm thời tới score-service/movie-service, xác nhận fallback hoạt động đúng như mục 46/49 |
| Legal validation test | Đảm bảo `legal-compliance-module` chặn đúng các campaign vượt trần 50%/120 ngày |

---

## 55. CI/CD & Rule Versioning (Bổ sung mới)

- Mỗi thay đổi `CampaignRule`/`tier_benefit_mapping` được lưu **versioned** (không update trực tiếp, tạo bản ghi mới kèm `effective_from`) để có thể **audit lại giá đã tính đúng theo rule nào tại thời điểm giao dịch** — quan trọng khi có khiếu nại khách hàng hoặc thanh tra.
- **Canary release** cho Campaign lớn: kích hoạt trước cho một nhóm rạp/khu vực nhỏ, theo dõi metrics (mục 42) trước khi mở rộng toàn hệ thống.
- Rollback rule phải **an toàn với giao dịch đang xử lý dở** — không thay đổi rule đang áp dụng cho reservation đã HELD, chỉ áp dụng rule mới cho request mới.

---

*Tài liệu v2 được xây dựng dựa trên phân tích mô hình vận hành thực tế của CGV Cinemas, Galaxy Cinema tại Việt Nam, kết hợp nguyên tắc DDD/Bounded Context, và quy định pháp luật Việt Nam về khuyến mại (Nghị định 81/2018/NĐ-CP, Thông tư 39/2025/TT-BCT). Ranh giới với `score-service` đã được tách bạch hoàn toàn: score-service là nguồn sự thật duy nhất cho điểm & hạng thành viên; Promotion Service chỉ tiêu thụ dữ liệu đó qua hợp đồng API/event rõ ràng (mục 52).*

## 56. Runtime reconciliation (2026-07-31)

This plan is the target architecture, not a promise that every cross-service
feature is already wired. The current production core supports one coupon or
one voucher per checkout, deterministic rule evaluation, atomic reservation,
confirmation/release/cancel, transactional outbox and campaign lifecycle jobs.
Automatic discovery, multi-benefit stacking and the
Score point saga remain roadmap items until Booking, Payment and Score publish
the agreed contracts. See
`promotion-service-production-readiness-report.md` for the release gate and
the read-only findings from related services.
