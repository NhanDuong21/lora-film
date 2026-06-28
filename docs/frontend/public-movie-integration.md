# LoraFilm Frontend Public Movie Integration Documentation

Tài liệu này hướng dẫn cách tích hợp các API phim công khai (public movie APIs) vào Trang chủ (Homepage) và Trang Chi tiết Phim (Movie Detail Page) của LoraFilm.

---

## 1. Kiến trúc kết nối API (Gateway URL)

Tất cả các API được gọi từ phía Frontend tới dịch vụ backend (Movie Service) đều phải đi qua **API Gateway**:

* **Gateway Base URL:** `http://localhost:8080` (Được cấu hình thông qua biến môi trường `VITE_API_BASE_URL` trong file `.env`).
* **Lưu ý quan trọng:** Không được gọi trực tiếp tới cổng của Movie Service (`http://localhost:8082`).

---

## 2. Luồng Trang chủ (Homepage Public Movie Flow)

Trang chủ hiển thị danh sách các phim dưới 2 tab chính: **PHIM ĐANG CHIẾU** và **PHIM SẮP CHIẾU**.

### 2.1. Tham số truy vấn (Query Parameters)
* **Kích thước trang (Pagination Size):** Cố định là `size=8` (hiển thị tối đa 8 phim mỗi trang, xếp theo cấu trúc lưới 4 cột x 2 dòng trên màn hình máy tính).
* **API Phim Đang Chiếu (NOW_SHOWING):**
  ```http
  GET /api/movies?page=0&size=8&status=NOW_SHOWING&sort=releaseDate,desc
  ```
* **API Phim Sắp Chiếu (UPCOMING):**
  ```http
  GET /api/movies?page=0&size=8&status=UPCOMING&sort=releaseDate,asc
  ```

### 2.2. Phân trang hoạt động (Pagination Strategy)
* **Phân trang độc lập:** Mỗi tab có trạng thái phân trang độc lập (`page` bắt đầu từ `0`). Việc chuyển đổi tab giữ nguyên trang của mỗi tab và không ảnh hưởng hay làm sai lệch dữ liệu của tab kia.
* **Hiển thị giao diện:** Số trang hiển thị trên UI bắt đầu từ `1` (Backend trang `0` tương ứng UI trang `1`).
* **Trạng thái nút phân trang:** Nút **Trang trước (Previous)** bị vô hiệu hóa khi `first === true`. Nút **Trang sau (Next)** bị vô hiệu hóa khi `last === true`.
* **Trải nghiệm người dùng:** Khi chuyển đổi trang, màn hình sẽ cuộn mượt (smooth scroll) trực tiếp đến khu vực danh sách phim (`#phim`).

---

## 3. Luồng Chi tiết Phim (Movie Detail Flow)

Khi người dùng nhấn vào bất kỳ thẻ phim nào ở Trang chủ, ứng dụng sẽ điều hướng đến trang chi tiết.

* **Định tuyến (Route):** `/movies/:movieId` (và hỗ trợ `/movie/:movieId` để đảm bảo tính tương thích).
* **API Chi tiết:**
  ```http
  GET /api/movies/{movieId}
  ```
* **Hỗ trợ truy cập trực tiếp:** Người dùng có thể truy cập trực tiếp bằng liên kết URL hoặc thực hiện tải lại trang (browser refresh) mà không bị mất dữ liệu.
* **Xử lý Trailer:** Nếu phim có liên kết trailer (`trailerUrl`), hiển thị nút **XEM TRAILER** để mở một cửa sổ trình phát video Youtube dạng popup/modal. Nếu không có trailer, nút này sẽ tự động ẩn đi một cách mượt mà.
* **Lịch chiếu & Đặt vé:** Khu vực này hiển thị thông báo trung lập *"Lịch chiếu đang được cập nhật."* để phục vụ cho các tích hợp tiếp theo.

---

## 4. Xử lý trạng thái Loading / Empty / Error

### 4.1. Trạng thái Loading (Tải dữ liệu)
* Sử dụng bộ khung xương **Skeleton Grid** (gồm 8 thẻ khung xương dạng xung hoạt họa nhấp nháy aspect aspect-[2/3]) hiển thị thay thế lưới phim trong quá trình tải dữ liệu.
* Không hiển thị các dữ liệu giả (mock data) cũ trong lúc API đang tải.

### 4.2. Trạng thái Trống (Empty State)
* Khi danh sách trả về rỗng (`content: []`), giao diện hiển thị thông báo thích hợp:
  * Tab Đang Chiếu: *"Hiện chưa có phim đang chiếu."*
  * Tab Sắp Chiếu: *"Hiện chưa có phim sắp chiếu."*

### 4.3. Trạng thái Lỗi (Error State)
* Khi xảy ra lỗi kết nối hoặc lỗi từ máy chủ (ví dụ: `MOVIE_INVALID_QUERY`, `INTERNAL_SERVER_ERROR`), giao diện sẽ hiển thị thông điệp thân thiện với người dùng: *"Không thể tải danh sách phim."*
* Cung cấp nút **Thử lại (Retry)** để chỉ yêu cầu lại dữ liệu bị lỗi của riêng tab hiện tại.
* Dành cho chi tiết phim, nếu gặp lỗi `MOVIE_NOT_FOUND` hoặc mã phim không hợp lệ, màn hình hiển thị thông báo *"Không tìm thấy phim hoặc phim không còn khả dụng."* kèm hai nút hành động: **Quay lại** và **Về trang chủ**.

---

## 5. Quyền truy cập công khai (Guest & Customer Access)

Trang chủ và Trang Chi tiết Phim được thiết lập để có thể truy cập hoàn toàn công khai:
* **Guest (Khách vãng lai):** Không cần đăng nhập vẫn xem được danh sách phim, chuyển trang, mở xem chi tiết và xem trailer.
* **Customer (Khách hàng đã đăng nhập):** Có trải nghiệm tương tự nhưng có quyền thực hiện các bước đặt vé (booking) sau này.
* **Quy tắc bảo mật:** Hai tuyến đường `/` (hoặc `/home`) và `/movies/:movieId` **không** được bọc trong bộ lọc bảo vệ `ProtectedRoute`.
