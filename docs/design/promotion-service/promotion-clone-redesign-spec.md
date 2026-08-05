# Spec thiết kế lại tính năng "Clone Promotion"

**Phạm vi:** `promotion-service` (backend, Java/Spring) + `features/promotion/admin` (frontend, React)
**Đối tượng đọc:** coding agent triển khai trực tiếp trên codebase hiện tại
**Trạng thái hiện tại:** đã audit code, xác nhận có bug nghiêm trọng — xem Phần 1

---

## 1. Đánh giá logic hiện tại (root cause)

File liên quan:
- `promotion/service/PromotionCatalogService.java` — method `clonePromotion()` (dòng 134-162)
- `promotion/controller/AdminPromotionController.java` — endpoint `POST /{id}/clone` (dòng 109-116)
- `features/promotion/admin/pages/AdminPromotionCenterPage.jsx` — dòng 563-568
- `features/promotion/admin/services/adminPromotionService.js` — dòng 33

### 1.1 Bug nghiêm trọng (must-fix)

| # | Vấn đề | Hậu quả |
|---|---|---|
| B1 | `clonePromotion()` không gọi `requireCampaign()` + `campaignPolicy.requireEditable()` như `create()`/`update()` | Có thể tạo promotion mới vào campaign đã ACTIVE/COMPLETED/CANCELLED/đã khóa cấu hình — phá vỡ rule khóa cấu hình sau khi campaign submit |
| B2 | Không gọi `validate()` | Bản sao có thể có `validFrom/validTo` nằm ngoài khung campaign hiện tại, hoặc đã ở quá khứ, không hề bị chặn |
| B3 | Sinh code cho VOUCHER (`cloneCode()`) không loop-check trùng DB, trong khi COUPON (`generatedCouponCode()`) có | Rủi ro vi phạm unique constraint → lỗi 500 không kiểm soát thay vì lỗi nghiệp vụ rõ ràng |
| B4 | API không nhận `campaignPublicId` đích, luôn gắn bản sao vào **đúng campaign nguồn** | Không thể clone promotion sang campaign khác; nếu fix B1 đúng chuẩn thì tính năng gần như luôn fail vì campaign nguồn thường đã bị khóa |
| B5 (Frontend) | Bấm "Clone" là gọi API ngay lập tức, không xác nhận, không cho xem/sửa trước khi lưu | Người dùng không kiểm soát được tên, code, ngày hiệu lực, campaign đích trước khi bản ghi được tạo thật trong DB |

### 1.2 Vấn đề thiết kế (product-level)

- Không có cơ chế "clone sang campaign khác" — nhu cầu thực tế phổ biến nhất (dùng lại một promotion mẫu đang chạy tốt cho campaign mới) lại không làm được.
- Không có liên kết truy vết bản sao được tạo từ đâu (`cloned from`) → khó audit khi cần trace nguồn gốc một promotion.
- Tên mặc định `"{name} (Copy)"` không kiểm tra trùng, clone 2 lần liên tiếp ra 2 promotion cùng tên `"X (Copy)"`.
- Toàn bộ logic được viết tay lại lần 2 (duplicate business rules) thay vì tái dùng `create()` — mọi rule mới thêm vào `create()` sau này sẽ **không tự động áp dụng cho clone**, tạo drift lâu dài.

### 1.3 Nguyên tắc thiết kế lại

1. **Clone không phải là một mutation riêng — nó là "tạo mới với dữ liệu prefill".** Việc lưu thật sự phải đi qua đúng pipeline `create()` (validate + policy + audit) để không bao giờ lệch rule.
2. **Không bao giờ ghi DB chỉ vì người dùng bấm nút Clone.** Phải luôn có bước xem lại / chỉnh sửa trước khi persist.
3. **Campaign đích là lựa chọn bắt buộc, có gợi ý mặc định thông minh**, không phải copy cứng.
4. Giữ nguyên toàn bộ audit/event pattern đã có (`PromotionCatalogEventService`), chỉ bổ sung field truy vết nguồn.

---

## 2. Thiết kế Backend

### 2.1 Data model — thêm trường truy vết nguồn gốc

Thêm vào `Promotion` entity (migration mới):

```java
@Column(name = "cloned_from_public_id", length = 36)
private String clonedFromPublicId; // null nếu tạo mới bình thường
```

Migration SQL (Flyway/Liquibase theo convention hiện có trong repo):
```sql
ALTER TABLE promotions ADD COLUMN cloned_from_public_id VARCHAR(36) NULL;
CREATE INDEX idx_promotions_cloned_from ON promotions (cloned_from_public_id);
```

Field này **chỉ để hiển thị/audit** (vd: badge "Nhân bản từ: {name}" trên UI, hoặc lọc "các bản sao của promotion X"), không ảnh hưởng nghiệp vụ.

### 2.2 API contract mới

Bỏ endpoint `POST /{id}/clone` hiện tại (mutating, không an toàn). Thay bằng 2 endpoint:

#### a) `GET /api/admin/promotions/{id}/clone-draft` — read-only, KHÔNG ghi DB

Trả về dữ liệu **prefill** cho form tạo mới, ở đúng shape của `PromotionUpsertRequest` cộng thêm metadata cảnh báo:

```java
public record PromotionCloneDraftResponse(
        String sourcePublicId,
        String sourceName,
        String suggestedCampaignPublicId,   // = campaign nguồn NẾU còn editable, ngược lại null
        boolean sourceCampaignEditable,     // để FE hiển thị cảnh báo + ép chọn campaign khác
        PromotionType promotionType,
        String suggestedCode,               // null cho AUTO; gợi ý mới cho VOUCHER/COUPON, CHƯA persist
        String suggestedName,               // "{name} (Copy)", đã check & tăng số nếu trùng — xem 2.3
        String description,
        Boolean publicVisible,              // kế thừa nguồn; admin có thể tắt trước khi lưu
        Integer priority,
        Boolean stackable,                  // kế thừa; admin có thể đổi trước khi lưu
        JsonNode conditionsJson,
        JsonNode actionsJson,
        JsonNode metadataJson,
        Integer maxRedemptions,
        Integer maxRedemptionsPerUser,
        Instant suggestedValidFrom,         // xem logic 2.4
        Instant suggestedValidTo,
        boolean validityWindowShifted       // true nếu hệ thống đã tự đẩy ngày vì window gốc đã qua
) {}
```

Endpoint này **không thay đổi state**, có thể gọi nhiều lần vô hại (idempotent by nature vì là GET).

```java
@GetMapping("/{id}/clone-draft")
@PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
public ResponseEntity<ApiResponse<PromotionCloneDraftResponse>> cloneDraft(
        @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId) {
    return ResponseEntity.ok(ApiResponse.success(service.buildCloneDraft(publicId)));
}
```

#### b) Tái sử dụng `POST /api/admin/promotions` (create hiện có) để lưu thật

Không có endpoint create riêng cho clone. FE build `PromotionUpsertRequest` từ dữ liệu người dùng đã review/chỉnh trong form (prefill từ `clone-draft`), rồi gọi đúng API `create()` đã có sẵn — tự động thừa hưởng `requireCampaign`, `requireEditable`, `validate`. Chỉ cần bổ sung 1 field optional để lưu lineage:

```java
public record PromotionUpsertRequest(
        @NotBlank @Size(max = 36) String campaignPublicId,
        ...
        @NotNull @Min(1) Integer maxRedemptionsPerUser,
        @NotNull Instant validFrom,
        @NotNull Instant validTo,
        @Size(max = 36) String clonedFromPublicId   // MỚI, optional, null nếu tạo thường
) {}
```

Trong `PromotionCatalogService.create()`, set thêm dòng:
```java
promotion.setClonedFromPublicId(request.clonedFromPublicId());
```

Event audit vẫn là `PROMOTION_CREATED` như bình thường (không cần event `PROMOTION_CLONED` riêng nữa) — nhưng nếu team muốn giữ phân biệt rõ trong audit log/analytics, có thể emit thêm 1 event phụ khi `clonedFromPublicId != null`:
```java
if (request.clonedFromPublicId() != null) {
    eventService.record("PROMOTION", saved.getPublicId(),
            "PROMOTION_CLONED_FROM", Map.of("sourcePublicId", request.clonedFromPublicId()), actor);
}
```

### 2.3 Logic sinh tên gợi ý (trong `buildCloneDraft`)

```java
private String suggestName(Promotion source, String campaignPublicId) {
    String base = source.getName() + " (Copy)";
    if (!promotionRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(base)) {
        return base;
    }
    int n = 2;
    String candidate;
    do {
        candidate = source.getName() + " (Copy " + n + ")";
        n++;
    } while (promotionRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(candidate));
    return candidate;
}
```
(Cần thêm method `existsByNameIgnoreCaseAndDeletedAtIsNull` vào `PromotionRepository` nếu chưa có; tên không unique ở DB level nên đây chỉ là gợi ý UX, **không** phải validate bắt buộc — người dùng vẫn có thể sửa tên tự do.)

Với `code`: **không tự sinh chuỗi ngẫu nhiên đoán bừa như hiện tại**. Với COUPON → dùng lại `generatedCouponCode()` hiện có (đã an toàn). Với VOUCHER → để trống, bắt người dùng nhập code mới tường minh trong form (giữ nguyên UX nhập code như khi tạo mới, tránh sinh code khó nhớ `PROMO123_COPY_A1B2C3`). Với AUTO → luôn `null`.

### 2.4 Logic điều chỉnh ngày hiệu lực

```java
private ValidityWindow suggestValidity(Promotion source, PromotionCampaign targetCampaign) {
    Duration length = Duration.between(source.getValidFrom(), source.getValidTo());
    Instant now = Instant.now();
    boolean expired = !source.getValidTo().isAfter(now);
    Instant from = expired ? now : source.getValidFrom();
    Instant to = from.plus(length);
    // Ép về trong khung campaign đích nếu có target campaign hợp lệ
    if (targetCampaign != null) {
        if (from.isBefore(targetCampaign.getStartAt())) from = targetCampaign.getStartAt();
        if (to.isAfter(targetCampaign.getEndAt())) to = targetCampaign.getEndAt();
    }
    return new ValidityWindow(from, to, expired);
}
```
Mục tiêu: gợi ý luôn là **một khung hợp lệ theo `validate()`**, nhưng người dùng vẫn thấy rõ và có thể sửa tay trong form trước khi submit — không âm thầm tạo bản ghi với ngày sai như hiện tại.

### 2.5 `buildCloneDraft` — tổng hợp

```java
@Transactional(readOnly = true)
public PromotionCloneDraftResponse buildCloneDraft(String publicId) {
    Promotion source = requirePromotion(publicId);
    PromotionCampaign sourceCampaign = requireCampaign(source.getCampaignPublicId());
    boolean editable = isCampaignEditable(sourceCampaign); // dùng lại rule của campaignPolicy, không throw
    ValidityWindow window = suggestValidity(source, editable ? sourceCampaign : null);
    return new PromotionCloneDraftResponse(
        source.getPublicId(), source.getName(),
        editable ? sourceCampaign.getPublicId() : null, editable,
        source.getPromotionType(),
        source.getPromotionType() == PromotionType.COUPON ? generatedCouponCode() : null,
        suggestName(source, source.getCampaignPublicId()),
        source.getDescription(), source.getPublicVisible(), source.getPriority(), source.getStackable(),
        parseJson(source.getConditionsJson()), parseJson(source.getActionsJson()),
        parseJson(source.getMetadataJson()), source.getMaxRedemptions(),
        source.getMaxRedemptionsPerUser(),
        window.from(), window.to(), window.shifted()
    );
}
```
Thêm helper `isCampaignEditable()` non-throwing (tách logic điều kiện ra khỏi `CampaignConfigurationPolicy.requireEditable()` để tái dùng, hoặc bọc try/catch quanh `requireEditable` — ưu tiên tách điều kiện thuần để tránh dùng exception cho control flow).

### 2.6 Danh sách campaign hợp lệ để chọn làm đích

FE cần 1 API để load dropdown "chọn campaign đích" khi campaign nguồn không editable hoặc người dùng muốn đổi. Dùng lại endpoint list campaign hiện có (`GET /api/admin/campaigns` — kiểm tra tên chính xác trong `PromotionCampaignController` nếu có), lọc phía FE hoặc thêm query param `editableOnly=true` để backend lọc theo `status=DRAFT && approvalStatus in (DRAFT, REJECTED)`.

### 2.7 Checklist test backend (agent nên viết test cho từng dòng này)

- [ ] `clone-draft` không ghi DB (verify repository.save không được gọi)
- [ ] `clone-draft` với source có campaign đã ACTIVE → `sourceCampaignEditable=false`, `suggestedCampaignPublicId=null`
- [ ] `clone-draft` với `validTo` gốc đã qua → `validityWindowShifted=true`, `suggestedValidFrom>=now`
- [ ] `create()` với `clonedFromPublicId` set → field được lưu đúng trong entity
- [ ] `create()` clone sang campaign KHÔNG editable → vẫn bị chặn 409 (đảm bảo bug B1 đã được fix vì giờ đi qua đúng `create()`)
- [ ] `create()` clone với code trùng → 409 đúng như flow tạo thường (đảm bảo bug B3 hết vì không còn sinh code random riêng cho VOUCHER)
- [ ] Xóa/deprecate hoàn toàn test cũ cho `POST /{id}/clone` cũ, thêm test cho endpoint mới

---

## 3. Thiết kế Frontend & UX Flow

### 3.1 Nguyên tắc UX

- Clone **luôn mở ra một form review**, không bao giờ tạo bản ghi ngay khi bấm nút trong danh sách.
- Form clone tái dùng **đúng component form tạo/sửa promotion hiện có** (`type: "promotion"` modal), chỉ khác ở dữ liệu prefill + banner "Đang nhân bản từ: {tên gốc}" + field `clonedFromPublicId` ẩn.
- Không tạo thêm 1 modal riêng biệt về UI — giảm code trùng lặp, đảm bảo mọi validate/format hiển thị (lỗi field, gợi ý…) giống hệt màn tạo mới.

### 3.2 Entry point

Trong `PromotionTable` (dòng ~973), nút "Clone" ở menu hành động từng dòng — giữ nguyên vị trí, đổi hành vi.

### 3.3 Screen flow chi tiết

```
[Danh sách Promotion]
        │  user click "Clone" trên 1 dòng
        ▼
[Loading nhẹ trên nút / skeleton nhỏ]
        │  gọi GET /promotions/{id}/clone-draft
        ▼
┌───────────────────────────────────────────┐
│ Modal "Nhân bản Promotion"                 │
│ (tái dùng PromotionForm, mode=clone)       │
│                                             │
│ Banner: "Nhân bản từ: {sourceName}"        │
│                                             │
│ ⚠ Nếu sourceCampaignEditable=false:        │
│   Banner cảnh báo màu vàng:                │
│   "Chiến dịch gốc đã khóa cấu hình.        │
│    Vui lòng chọn chiến dịch khác."         │
│   → field Campaign bắt buộc phải chọn,     │
│     không cho để trống, không prefill      │
│                                             │
│ ⚠ Nếu validityWindowShifted=true:          │
│   Ghi chú nhỏ dưới field ngày:             │
│   "Khoảng thời gian gốc đã hết hạn,        │
│    hệ thống đã tự đề xuất khoảng mới."     │
│                                             │
│ [Campaign ▾] (prefill nếu editable,        │
│               bắt buộc chọn nếu không)     │
│ [Tên]        (prefill "{name} (Copy)")     │
│ [Loại]       (readonly, giống bản gốc)     │
│ [Code]       (prefill theo rule 2.3;       │
│               VOUCHER để trống, bắt nhập)  │
│ [Mô tả] [Điều kiện] [Hành động] ...        │
│   → tất cả prefill, editable như form thường│
│ [Public visible] → kế thừa voucher nguồn,   │
│                     có thể đổi trước khi lưu│
│ [Ngày hiệu lực từ - đến] (prefill 2.4)     │
│                                             │
│  [Hủy]              [Tạo bản sao] ─────────┼──► POST /promotions (create bình thường)
└───────────────────────────────────────────┘        với clonedFromPublicId = sourcePublicId
        │ success                                     │ error (409 trùng code/tên, ngoài khung campaign...)
        ▼                                              ▼
  Toast "Đã tạo bản sao promotion"           Hiển thị lỗi field-level ngay trong form
  Đóng modal, refresh danh sách              (dùng chung error handling đã có của form tạo/sửa)
  Highlight dòng mới tạo trong bảng           user sửa lại, KHÔNG mất dữ liệu đã nhập
  (scroll tới + flash background 1-2s)
```

### 3.4 Trạng thái lỗi cần xử lý riêng

| Tình huống | Xử lý UI |
|---|---|
| Gọi `clone-draft` lỗi (promotion đã bị xóa/không tồn tại) | Toast lỗi, không mở modal |
| Không campaign nào khác editable để chọn (dropdown rỗng) | Trong modal hiển thị empty-state: "Không có chiến dịch nào đang ở trạng thái nháp để nhân bản vào. Hãy tạo chiến dịch mới trước." + nút tắt "Tạo chiến dịch mới" mở luôn modal tạo campaign |
| Submit bị 409 do trùng code/tên | Field-level error dưới đúng input, giữ nguyên toàn bộ dữ liệu đã nhập |
| Submit bị 409 do campaign vừa bị khóa (race condition — ai đó submit campaign trong lúc đang mở modal clone) | Banner lỗi ở đầu form: "Chiến dịch đã bị khóa cấu hình, vui lòng chọn chiến dịch khác" + tự động clear field campaign, reload lại danh sách campaign editable |

### 3.5 Thay đổi cụ thể trong code frontend

**`adminPromotionService.js`** — thay:
```js
clonePromotion: async id => unwrap(await apiClient.post(`/api/admin/promotions/${id}/clone`)),
```
bằng:
```js
getCloneDraft: async id =>
  unwrap(await apiClient.get(`/api/admin/promotions/${id}/clone-draft`)),
```
(việc "tạo" dùng lại `createPromotion` sẵn có trong service, không cần thêm hàm mới).

**`AdminPromotionCenterPage.jsx`** — thay đoạn `onClone` (dòng 563-568):
```jsx
onClone={(item) => openCloneModal(item)}
```
với `openCloneModal` mới:
```jsx
const openCloneModal = async (item) => {
  setBusy(true);
  try {
    const draft = await adminPromotionService.getCloneDraft(item.publicId);
    setModal({
      type: "promotion",
      mode: "clone",
      record: draft,          // PromotionForm nhận draft này để prefill
      promotionType: draft.promotionType,
      cloneWarning: !draft.sourceCampaignEditable,
    });
  } catch (error) {
    setMessage({ kind: "error", text: errorText(error) });
  } finally {
    setBusy(false);
  }
};
```

`PromotionForm` component (component render modal `type: "promotion"`) cần nhận thêm prop `mode` (`"create" | "edit" | "clone"`) để:
- hiển thị banner nguồn gốc khi `mode === "clone"`
- ép campaign field thành required-select (không prefill sẵn) khi `cloneWarning === true`
- submit gọi `createPromotion({ ...formValues, clonedFromPublicId: record.sourcePublicId })` thay vì `updatePromotion`

### 3.6 Checklist test frontend

- [ ] Click "Clone" → modal mở với dữ liệu đúng từ `clone-draft`, không có request POST nào được gọi trước khi user bấm "Tạo bản sao"
- [ ] Campaign nguồn không editable → dropdown campaign trống, không prefill, submit bị disable tới khi chọn
- [ ] Tên gợi ý tăng dần đúng khi trùng (`(Copy)`, `(Copy 2)`, ...)
- [ ] Đóng modal (Hủy/click outside) không tạo bản ghi nào
- [ ] Lỗi 409 khi submit hiển thị đúng field, dữ liệu form không bị reset
- [ ] Sau khi tạo thành công, danh sách có dòng mới và được highlight

---

## 4. Kế hoạch triển khai (rollout)

1. **Backend trước:** thêm migration cột `cloned_from_public_id`, thêm `clone-draft` endpoint, thêm field `clonedFromPublicId` vào `PromotionUpsertRequest` + `create()`. Viết unit test theo checklist 2.7. Deploy, giữ endpoint cũ `POST /{id}/clone` tồn tại song song nhưng đánh dấu `@Deprecated` (không xóa ngay để tránh vỡ FE đang chạy production).
2. **Frontend sau:** đổi UI theo mục 3, trỏ sang API mới hoàn toàn.
3. **Xóa endpoint cũ** `POST /{id}/clone` và method `PromotionCatalogService.clonePromotion()` sau khi FE đã deploy ổn định (1 release sau, tránh xóa cùng lúc để dễ rollback).
4. Không cần backfill dữ liệu cũ (`cloned_from_public_id` để `NULL` cho các bản ghi tạo trước đó — chấp nhận được vì đây chỉ là field audit/hiển thị).

---

## 5. Tóm tắt cho agent triển khai

- **Không sửa vá `clonePromotion()` hiện tại** — thay thế hoàn toàn bằng flow "GET draft (read-only) → user review trong form tạo mới → POST create() có sẵn". Đây là fix gốc rễ cho toàn bộ bug ở Phần 1, vì nó loại bỏ hẳn code path riêng đang bị thiếu validate/policy.
- Backend: 1 migration, 1 endpoint mới (`GET .../clone-draft`), 1 field mới trong `PromotionUpsertRequest`, vài helper method trong service (suggestName, suggestValidity, isCampaignEditable). Không động vào `validate()`/`campaignPolicy` hiện có — chỉ tái sử dụng.
- Frontend: đổi hành vi nút Clone từ gọi API trực tiếp → mở form tạo mới đã prefill, dùng lại component form hiện có với 1 prop `mode` mới.
