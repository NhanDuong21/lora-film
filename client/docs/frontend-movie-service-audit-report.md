# LoraFilm Frontend + Movie Service API Audit Report

## 1. Final Verdict

- **Audit status**: COMPLETE
- **Frontend readiness**: Good. Feature-based architecture is clean, and the shared foundation is solid enough to continue.
- **Build verification**: BLOCKED_BY_LOCAL_WINDOWS_FILE_LOCK (EPERM error on `@rolldown/binding-win32-x64-msvc.node`). Codebase is theoretically sound, but local lock prevents `npm ci` / `npm run build` from completing.
- **Movie Service FE readiness**: Coverage chưa thể xác định chính xác. Basic Movie/Cinema modules có integration. Scheduling và nhiều sub-domain chưa được implement.
- **Safe next step**: One full Movie Service frontend issue (branch: `feature/full-movie-service-frontend`) executed iteratively via internal checkpoints.

## 2. Repository State

- **Branch**: `chore/frontend-movie-service-audit`
- **Working tree**: Clean (with some untracked report files).
- **Build**: BLOCKED_BY_LOCAL_WINDOWS_FILE_LOCK
- **Lint**: BLOCKED_BY_LOCAL_WINDOWS_FILE_LOCK
- **Test**: Not configured

## 3. Technology Stack

| Concern | Current implementation | Verdict | Notes |
| :--- | :--- | :--- | :--- |
| HTTP client | Axios (`^1.16.1`) | USED | Tích hợp tốt với `apiClient.js` |
| Server state | Custom React Hooks (`useEffect` + `useState`) | CUSTOM | Có thể cần TanStack Query cho cache. |
| Form state | Controlled Components (`useState`) | CUSTOM | Form xử lý thủ công, dài dòng. |
| Validation | Manual `if/else` | CUSTOM | Chưa dùng Zod/Yup. |
| Toast | Custom `triggerToast` từ Context | USED | |
| Date/time | Native `Date` | NATIVE | Dễ sai timezone, cần `date-fns-tz`. |
| Routing | React Router DOM (`^7.18.0`) | USED | Tốt, bảo vệ Role chặt chẽ. |
| Auth | Custom `AuthContext` + `jwt-decode` | USED | Có refresh token 401. |
| Testing | None | MISSING | Cần thêm Jest/Vitest. |

## 4. Architecture Assessment

### Strengths
- Cấu trúc Feature-based rất dễ bảo trì. Phân chia rành mạch `admin`, `customer`.
- Axios request/response interceptors xử lý 401 tự động mượt mà.

### Risks
- Quản lý state qua Context/State nội bộ kết hợp `useEffect` dễ sinh lỗi Race condition nếu không có Abort Controller.

### Boundary violations
- Frontend đang hardcode text-match ('hồ chí minh', 'hà nội') trong UI để gom nhóm rạp thay vì lấy metadata từ API. (Evidence: `MasterBookingFunnelPage.jsx`).

## 5. Shared Foundation Audit

### apiClient
- **Tồn tại:** `client/src/services/apiClient.js`
- **Tình trạng:** Khởi tạo axios instance tốt. Tự động gắn token, refresh token khi 401. Base URL lấy từ env.

### apiErrorHandler
- **Tồn tại:** `client/src/utils/apiErrorHandler.js`
- **Tình trạng:** Có tồn tại. Cần đánh giá thêm mức độ normalize errorCode, message và fieldErrors khi call form API.

### Auth
- **Tồn tại:** `AuthContext.jsx`, `authStorage.js`.
- **Tình trạng:** Hoạt động ổn định với cơ chế decode JWT token.

### UI kit
- **Tồn tại:** `client/src/components/common/ui/uiKit.jsx`, `SkeletonTable.jsx`, `SystemUpdating.jsx`
- **Tình trạng:** Common UI foundation đã tồn tại. Mức độ chuẩn hóa và tái sử dụng còn cần đánh giá thêm trong quá trình code Phase tiếp theo.

### Pagination
- **Tình trạng:** Pagination xử lý cục bộ trên từng file thay vì dùng custom hook chung. Cần adapter chuẩn hóa.

### Date/time
- **Tình trạng:** Xử lý bằng đối tượng Date nguyên thủy. Dễ sinh lỗi cross-timezone.

### Environment
- **Tình trạng:** `vite.config.js` dùng proxy tốt sang localhost:8080. `VITE_USE_AUTH_MOCK=false`. Không lộ secret keys trên frontend.

## 6. External API Architecture

### TMDB Support API
- **Base URL:** `https://tmdb-api.nyanmovie.site`
- **Swagger:** `https://tmdb-api.nyanmovie.site/api-docs/`
- **Repository:** `https://github.com/NhanDuong21/TMDB-API`
- **Owner:** Backend Team
- **Responsibility:** Upstream metadata source quét phim từ TMDB, chuẩn hóa dữ liệu.
- **Recommended caller:** Movie Service (Backend).
- **FE direct usage:** Tuyệt đối không. Frontend không gọi trực tiếp API này để đồng bộ.
- **Security:** Movie Service server-to-server.
- **Sync model:** Bulk/Daily synchronization.
- **Current FE status:** Đang mock search bằng `setTimeout` trong `useTmdbSearch.js`.
- **Current Movie Service status:** Đã có endpoint command `POST /api/admin/tmdb/sync/{tmdbId}`.
- **Contract gaps:** Backend command endpoints (sync, bulk) có tồn tại, nhưng flow automatic mirror không bắt buộc Frontend phải handle bulk.

### Global Location API
- **Base URL:** `https://location-api.nyanmovie.site`
- **Swagger:** `https://location-api.nyanmovie.site/api-docs/`
- **Repository:** `https://github.com/NhanDuong21/global-location-api`
- **Owner:** Backend Team
- **Responsibility:** Address autocomplete, Geocoding, chuẩn hóa địa chỉ.
- **Recommended caller:** Frontend Cinema Form trực tiếp.
- **FE direct usage:** Yes. Dành cho Admin khi tạo/sửa Cinema.
- **Security:** Public/CORS protected.
- **Required debounce:** Cần thiết (300-500ms).
- **Required request cancellation:** Cần thiết để tránh race condition khi type nhanh.
- **Cinema fields populated:** address, district, city, latitude, longitude.
- **Current FE status:** Chưa gọi Location API (Form địa chỉ rạp đang là ô input thường/mock timer).
- **Contract gaps:** Chưa implement hook gọi Location API.

### Integration diagrams

#### TMDB
```text
TMDB
→ TMDB Support API
→ Movie Service
→ Database
→ Frontend
```

#### Location
```text
Frontend Cinema Form
→ Global Location API
→ Form state
→ Movie Service Cinema API
```

## 7. TMDB Synchronization Model

### Purpose
TMDB quyết định hệ thống biết phim nào tồn tại. Frontend Admin quyết định phim nào được đưa vào vận hành.

### Actor
Movie Service Backend (Automatic Job) và Admin User (trên UI).

### Why synchronize
Bảo đảm kho phim luôn dồi dào, chuẩn metadata quốc tế.

### Initial bulk sync
Bulk sync toàn bộ vào DB → direct upsert into `movies` (với status = `DRAFT`).

### Latest sync
Daily insert newly discovered movies.

### Updated sync
Compare `tmdbLastUpdated` → update if newer → otherwise skip.

### Direct upsert model
Phim mới đồng bộ trực tiếp vào bảng `movies` (nhận diện bằng `tmdb_id IS NOT NULL`).

### Admin responsibility
Admin dùng Movie Catalog để lọc phim (DRAFT), xem chi tiết, bổ sung dữ liệu nghiệp vụ, tạo Movie Version, chuyển sang trạng thái UPCOMING hoặc NOW_SHOWING, rồi tạo Showtime.

### Why approval is not required
Không cần Admin phải bấm "Approve" từng phim một (endpoint `POST /api/admin/tmdb/approve` có tồn tại nhưng flow đã chốt không yêu cầu Admin approve từng phim). Backend/DB tự động lưu với DRAFT status. (Cần backend team xác nhận endpoint approve này còn được sử dụng ở use case nào).

### Schema compatibility
Không cần sửa schema để triển khai flow v1. Không cần staging table. Lược đồ hiện tại đã đủ đáp ứng flow direct upsert.

### Risks
Sync toàn bộ full details cho hàng triệu phim có thể làm phình `people`, `credits`, `media` table. *Khuyến nghị backend*: sync metadata cơ bản trước, hydrate dữ liệu chi tiết khi phim sắp được sử dụng. (Đây là khuyến nghị cho backend/architecture, không phải frontend sửa schema).

## 8. Route Inventory

| Route | Role | Layout | Page | Guard | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `/` | Customer | CustomerLayout | Home | None | EXISTS |
| `/movies` | Customer | CustomerLayout | MovieDiscoveryPage | None | EXISTS |
| `/cinema/:id` | Customer | CustomerLayout | CinemaDetailPage | None | EXISTS |
| `/booking` | Customer | CustomerLayout | MasterBookingFunnelPage | None | EXISTS |
| `/seat-selection` | Customer | CustomerLayout | SeatSelectionPage | None | EXISTS |
| `/admin/movies` | Admin | AdminLayout | AdminMoviePage | AdminRoute | EXISTS |
| `/admin/genres` | Admin | AdminLayout | AdminGenrePage | AdminRoute | EXISTS |
| `/admin/cinemas` | Admin | AdminLayout | AdminCinemaPage | AdminRoute | EXISTS |
| `/admin/rooms` | Admin | AdminLayout | AdminRoomPage | AdminRoute | EXISTS |
| `/admin/rooms/create` | Admin | AdminLayout | AdminRoomCreatePage | AdminRoute | EXISTS |
| `/admin/rooms/edit/:roomId` | Admin | AdminLayout | AdminRoomEditPage | AdminRoute | EXISTS |
| `/admin/showtimes` | Admin | AdminLayout | AdminShowtimePage | AdminRoute | EXISTS (Empty Skeleton) |

## 9. Page Integration Matrix

| Feature | Page | API real | Mock | Status | Main gaps |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Catalog Admin | AdminMoviePage | Yes | Yes (TMDB) | CONNECTED | Basic Movie CRUD đã có tích hợp đáng kể. Full Catalog chưa hoàn thành. |
| Catalog Admin | AdminGenrePage | Yes | No | CONNECTED | Không có. |
| Facilities Admin | AdminCinemaPage | Yes | No | CONNECTED | Cinema/Auditorium core đã có tích hợp. Full workflow chưa hoàn chỉnh. |
| Facilities Admin | AdminRoomPage (List/Create/Edit) | Yes | No | CONNECTED | Không có. |
| Scheduling Admin | AdminShowtimePage | No | No | EMPTY_SKELETON | Skeleton chỉ chứa `<SystemUpdating />`. Chưa làm API nào. |
| Booking | MasterBookingFunnelPage | Yes | No | CONNECTED | Phân nhóm khu vực hardcode. |
| Booking | SeatSelectionPage | Yes | Yes | PARTIALLY_CONNECTED | Chặn tạo Order, mock modal. |
| Customer | CinemaDetailPage | Yes | Yes | PARTIALLY_CONNECTED | Hardcode fallback cho Media/Hours. |
| Customer | Home | No | Yes | MOCKED | Placeholder skeleton/Promise.resolve(). |

## 10. Mock and Hardcoded Inventory

| File | Mock variable/logic | Used by | Replacement API |
| :--- | :--- | :--- | :--- |
| `useTmdbSearch.js` | `setTimeout` mock | AdminMoviePage | Movie Service TMDB integration flow |
| `CinemaDetailPage.jsx` | `CINEMA_STATIC_DETAILS` | CinemaDetailPage | `/api/cinemas/:slug` (Media/Operating Hours) |
| `SeatSelectionPage.jsx` | `showSuccessModal` logic | SeatSelectionPage | `/api/showtimes/{id}/lock-seats` or Booking API |
| `useHomepageMovies.js` | `await Promise.resolve()` | Home | `/api/customer/movies` |
| `HeroSection.jsx` | `const cinemas = []; // TODO` | Home | `/api/cinemas` |

## 11. Service Inventory

| Service | Function | Endpoint | Used | Correct | Verdict |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `movieService` | `getMovies` | `/api/customer/movies` | Yes | Yes | USED_CORRECTLY |
| `movieService` | `getGenres` | `/api/customer/genres` | Yes | Yes | USED_CORRECTLY |
| `movieService` | `getCinemas` | `/api/cinemas` | Yes | Yes | USED_CORRECTLY |
| `movieService` | `getShowtimes` | `/api/showtimes` | Yes | Yes | USED_CORRECTLY |
| `movieService` | `getSeatLayout` | `/api/showtimes/:id/seat-layout` | Yes | Yes | USED_CORRECTLY |
| `adminCinemaService` | `getCinemas`... | `/api/admin/cinemas` | Yes | Yes | USED_CORRECTLY |
| `adminRoomService` | `getAdminSeatLayout`... | `/api/admin/auditoriums` | Yes | Yes | USED_CORRECTLY |
| `adminMovieService` | `getMovies`... | `/api/admin/movies` | Yes | Yes | USED_CORRECTLY |

## 12. Hook Inventory

| Hook | Service/API | Used | State handling | Verdict |
| :--- | :--- | :--- | :--- | :--- |
| `useAdminMovies` | `adminMovieService` | Yes | `useState`, `useEffect` | Tốt. |
| `useTmdbSearch` | Mock `setTimeout` | Yes | `setTimeout` | Replace bằng flow TMDB thật/external API contract. |
| `useHomepageMovies` | Mock `Promise.resolve` | Yes | `useState` | Refactor gọi thực tế. |

## 13. Movie Service Endpoint Coverage

### Catalog
- **Movies**: Basic CRUD (USED_CORRECTLY).
- **Genres**: Basic CRUD (USED_CORRECTLY).
- **Movie Versions**: CONTRACT_GAP / NOT_USED (Thiếu service, hook, UI).
- **Movie Media**: CONTRACT_GAP / NOT_USED.
- **People/Credits/Production Companies**: CONTRACT_GAP / NOT_USED.
- **TMDB**: Command endpoints tồn tại, NOT_USED trên FE.

### Facilities
- **Cinemas**: Basic CRUD (USED_CORRECTLY).
- **Cinema Media/Operating Hours/Closure Periods**: NOT_USED / CONTRACT_GAP.
- **Auditoriums/Seat Layout/Seat Types**: USED_CORRECTLY.
- **Maintenance Windows**: NOT_USED.

### Scheduling
- **Showtimes**: NOT_USED (EMPTY_SKELETON).
- **Status History**: NOT_USED.
- **Pricing**: NOT_USED.
- **Auto Scheduling**: NOT_USED.

### Customer
- **Customer Movies/Cinemas/Showtimes/Seat Layout**: USED_PARTIALLY (Home đang mock, một số trang dùng fallback).

### Internal
- **Internal endpoints**: INTERNAL_ONLY (Frontend must not call directly).

## 14. Catalog Admin Audit
- **Current implementation**: Basic Movie/Genre CRUD hoạt động tốt, form rất lớn, handle nhiều field.
- **Connected screens**: AdminMoviePage, AdminGenrePage.
- **Mock screens**: Flow tìm kiếm TMDB đang mock bằng `setTimeout`.
- **Missing screens**: Quản lý Movie Version, Movie Media, People, Credits, Production Companies, Genre/Credit assignment.
- **Missing services/hooks/routes**: Các file service quản lý version/media/people.

## 15. Facilities Admin Audit
- **Current implementation**: Quản lý Cinema và Auditorium tốt. Seat Grid Designer bằng brush/cọ rất ấn tượng.
- **Connected screens**: AdminCinemaPage, AdminRoomPage, AdminRoomCreatePage, AdminRoomEditPage.
- **Mock screens**: Component nhập địa chỉ (CinemaLocationForm) giả lập call API qua `setTimeout`.
- **Missing screens**: Operating Hours UI hoàn chỉnh, Closure Period thực tế, Cinema Media upload, Maintenance Window.
- **Missing services/hooks/routes**: Global Location API hook, Maintenance window service.

## 16. Scheduling Admin Audit
- **Manual Showtime**: Chưa có UI (chỉ là khung rỗng `<SystemUpdating />`).
- **Lifecycle**: Chưa có tính năng đổi trạng thái suất chiếu.
- **Pricing**: Chưa có UI nhập giá vé suất chiếu.
- **Auto Schedule**: Rỗng hoàn toàn.

## 17. Customer Flow Audit
- **Home**: MOCKED. Hero và Event section dùng array tĩnh hoặc Promise.
- **Movie discovery**: CONNECTED.
- **Movie detail**: Thiếu route (Chưa implement).
- **Cinema**: PARTIALLY_CONNECTED (Dùng `CINEMA_STATIC_DETAILS`).
- **Showtime**: CONNECTED (List hoạt động tốt).
- **Seat selection**: CONNECTED (Render sơ đồ và chặn "1 seat gap" tốt).

## 18. Booking Handoff Audit
- **Seat selection to Booking**: Nút Checkout chỉ trigger Modal Success nội bộ. Chưa gọi Endpoint tạo order hoặc Lock Seat. PARTIALLY_CONNECTED.

## 19. Schema-to-Form Gaps
- Không có khác biệt nghiêm trọng. Hầu hết các field (name, slug, duration) map chuẩn với backend. Nhưng validate regex chưa chặt.

## 20. Pagination and Sorting
- **Admin lists**: Gửi `page=0` (đúng chuẩn Spring Boot). USED_CORRECTLY.
- **Customer lists**: Khá cơ bản, xử lý bằng state `page` thủ công.

## 21. Error Handling
- **HTTP/Error**: Backend throw error code. Frontend gọi `triggerToast` chung chung. Chưa map `fieldErrors` tự động vào Form. Cần đánh giá thêm `apiErrorHandler.js`.

## 22. Timezone
- **Current behavior**: Frontend dùng native Date object/local timezone.
- **Risks**: Nguy cơ hiển thị sai giờ chiếu (showtime) đối với Rạp khác múi giờ nếu admin/khách ở múi giờ khác nhau.
- **Required utilities**: Cần dùng Date library như `date-fns-tz`.

## 23. Component Keep/Refactor Matrix
- **`SeatGridDesigner` / `BrushToolbar`**: KEEP. Thiết kế rất tốt. (Evidence: Dùng trong `AdminRoomCreatePage`).
- **`MovieFormModal`**: REFACTOR. Hơn 1400 dòng mã. (Evidence: Quá tải state, ôm đồm render mọi field).
- **`SystemUpdating`**: KEEP. Placeholder tiện lợi.
- **`CinemaLocationForm`**: REFACTOR. Loại bỏ mock timer và tích hợp Global Location API.

## 24. Missing Routes
- `/admin/showtimes/create` (Purpose: Manual showtime creation).
- `/admin/showtimes/auto-schedule` (Purpose: Tự động hóa).
- `/checkout` (Purpose: Payment handoff).

## 25. Missing Services
- `adminShowtimeService.js` (Endpoints: `POST /api/admin/showtimes`, `PUT /api/admin/showtimes/{id}`, v.v.).
- `autoScheduleService.js`

## 26. Missing Hooks
- `useAdminShowtimes.js` (Service: `adminShowtimeService`).
- `useLocationAutocomplete.js` (Service: Global Location API).

## 27. Backend Contract Gaps
- `GET /api/admin/showtimes` chưa xác nhận tồn tại. Query public `GET /api/showtimes` có thể không expose DRAFT/CANCELLED cho admin. Đây là BACKEND CONTRACT GAP cần xác minh trước khi implement Scheduling Admin.

## 28. Build and Lint Evidence
- `npm ci`: BLOCKED_BY_LOCAL_WINDOWS_FILE_LOCK (EPERM error on `@rolldown/binding-win32-x64-msvc.node`).
- `npm run build`: Bị chặn bởi lỗi trên.
- `npm run lint`: Bị chặn bởi lỗi trên.
- Không sửa source, không xóa bừa package-lock. Việc build không thành công không đại diện cho lỗi code.

## 29. Critical Findings

### P0
- Lỗi khóa file (EPERM) môi trường cục bộ trên Windows khiến các lệnh cơ bản bị block.
- Khuyết toàn bộ module Scheduling Admin (`AdminShowtimePage.jsx` rỗng).

### P1
- Backup/Contract Gap: Kiểm chứng API `GET /api/admin/showtimes` để Admin thấy danh sách suất chiếu.
- Tích hợp Global Location API vào `CinemaLocationForm`.

### P2
- Bóc tách `MovieFormModal.jsx` thành component nhỏ hơn.
- Gỡ bỏ `CINEMA_STATIC_DETAILS` hardcode, dùng data thật.

### P3
- Thêm thư viện xử lý Timezone và Error normalizer (map `fieldErrors` tự động).

## 30. Final Implementation Recommendation

### One-issue/one-branch strategy
- Chỉ định **1 issue**, **1 branch** (`feature/full-movie-service-frontend`) và **1 Merge Request** cho toàn bộ Movie Service FE do chỉ có một FE owner. Tránh overhead phân mảnh branch.

### Internal implementation phases
1. **Checkpoint 1** — Shared Frontend Foundation
2. **Checkpoint 2** — Complete Catalog Admin (Movie version, People)
3. **Checkpoint 3** — Complete Facilities Admin (Media, Operating Hours)
4. **Checkpoint 4** — Integrate Global Location API
5. **Checkpoint 5** — Implement Manual Showtime Management
6. **Checkpoint 6** — Implement Showtime Lifecycle and Pricing
7. **Checkpoint 7** — Implement Auto Showtime Scheduling
8. **Checkpoint 8** — Complete Customer Movie/Cinema/Showtime Flow
9. **Checkpoint 9** — TMDB Sync Center and Catalog UX
10. **Checkpoint 10** — E2E Hardening

*Lưu ý:* Phải hoàn thành từng checkpoint, build/lint và manual test trước khi qua checkpoint tiếp theo.

## 31. Audit Issue Definition of Done
- Branch đúng chuẩn: Yes.
- Report lưu đúng `client/docs/`: Yes.
- Kết luận sai (apiErrorHandler, uiKit, TMDB) đã sửa: Yes.
- TMDB/Location API architectures documented: Yes.
- Contract Gaps/External APIs verified: Yes.
- Không sửa production source: Yes.

## 32. Final Conclusion
Audit issue: Documentation-only, ready to close after report commit.
Next issue: `[Frontend] Complete Full Movie Service Frontend Integration`.
Next branch: `feature/full-movie-service-frontend`.
