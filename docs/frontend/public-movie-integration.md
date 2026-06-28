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
* **Trạng thái nút phân trang:** Nút **Trang trước (Previous)** bị vô hiệu hóa khi `first === true` hoặc dữ liệu đang được làm mới (`isRefreshing`). Nút **Trang sau (Next)** bị vô hiệu hóa khi `last === true` hoặc `isRefreshing`.
* **Hiển thị nút "Xem thêm":** Nút này chỉ hiển thị khi tổng số trang phim từ backend của tab hiện tại lớn hơn 1 (tức là `totalPages > 1`). Nếu tổng số phim nhỏ hơn hoặc bằng 8 (chỉ có 1 trang duy nhất), nút "Xem thêm" sẽ tự động ẩn đi để giao diện trông gọn gàng.
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

### 4.1. Mô hình Trạng thái Loading mới (Loading State Model)
Để ngăn ngừa tình trạng chớp nháy (UI flickering) màn hình khi đổi trang hoặc chuyển tab, hệ thống áp dụng mô hình hai trạng thái tải dữ liệu:
* **Tải lần đầu (`isInitialLoading`):** Kích hoạt khi danh sách phim hiện tại trống (`movies.length === 0`). Grid phim sẽ hiển thị 8 thẻ khung xương Shimmer (`MovieSectionSkeleton`).
* **Làm mới/Tải trang tiếp theo (`isRefreshing`):** Kích hoạt khi người dùng chuyển trang hoặc đổi tab mà trước đó đã có dữ liệu phim. Giao diện sẽ **giữ nguyên** lưới phim cũ nhưng làm mờ đi (`opacity-40`) và tắt tương tác (`pointer-events-none`) để báo hiệu đang tải. Khi dữ liệu mới tải xong, danh sách phim cũ mới bị thay thế bởi phim mới một lần duy nhất.

### 4.2. Cơ chế chống Vòng lặp Render/Request (Loop & Race Prevention)
Để ngăn chặn vòng lặp gọi API vô tận trong React do các phụ thuộc không ổn định (unstable dependencies) trong `useEffect` và `useCallback`:
* **Sử dụng React Refs:** Hàm gọi API (`fetchMovies`) sử dụng `useRef` để theo dõi các giá trị động thường thay đổi như hàm phản hồi (`onDataLoaded`) và mảng dữ liệu phim (`movies`). Việc này giúp `fetchMovies` chỉ phụ thuộc vào các tham số nguyên thủy cố định (`status`, `sort`, `size`).
* **Tránh hàm inline làm Dependency:** Hàm callback `onDataLoaded` truyền từ Trang chủ vào hook được bọc lại trong một `useCallback` ổn định, tránh việc tạo mới con trỏ hàm sau mỗi lần render gây lặp vô hạn.
* **Chống Race Condition:** Sử dụng biến tham chiếu yêu cầu cuối (`lastRequestRef`) để loại bỏ hoàn toàn các phản hồi API cũ trả về chậm hơn các yêu cầu mới.

### 4.3. Hoạt ảnh Khung xương Shimmer & Chống giật khung hình
* Thay thế hoạt ảnh nhấp nháy toàn bộ thẻ (`animate-pulse`) bằng hiệu ứng dải sáng trượt qua mặt phẳng ngang mượt mà (`.movie-skeleton`) dựa trên CSS keyframes. Hoạt ảnh tự động tắt nếu thiết bị người dùng bật chế độ giảm chuyển động (`prefers-reduced-motion: reduce`).
* Khung xương Shimmer sử dụng tỷ lệ kích thước (`aspect-[2/3]`), cấu trúc lưới cột, khoảng cách cột và các placeholder chữ y hệt như thẻ phim thật, giúp loại bỏ hoàn toàn hiện tượng sụt lún bố cục (Layout Shift) khi dữ liệu xuất hiện.

### 4.4. Trạng thái Trống (Empty State)
* Khi danh sách trả về rỗng (`content: []`), giao diện hiển thị thông báo thích hợp:
  * Tab Đang Chiếu: *"Hiện chưa có phim đang chiếu."*
  * Tab Sắp Chiếu: *"Hiện chưa có phim sắp chiếu."*

### 4.5. Trạng thái Lỗi (Error State)
* Khi xảy ra lỗi kết nối hoặc lỗi từ máy chủ (ví dụ: `MOVIE_INVALID_QUERY`, `INTERNAL_SERVER_ERROR`), giao diện sẽ hiển thị thông điệp thân thiện với người dùng: *"Không thể tải danh sách phim."*
* Cung cấp nút **Thử lại (Retry)** để chỉ yêu cầu lại dữ liệu bị lỗi của riêng tab hiện tại.
* Dành cho chi tiết phim, nếu gặp lỗi `MOVIE_NOT_FOUND` hoặc mã phim không hợp lệ, màn hình hiển thị thông báo *"Không tìm thấy phim hoặc phim không còn khả dụng."* kèm hai nút hành động: **Quay lại** và **Về trang chủ**.

---

## 5. Quyền truy cập công khai (Guest & Customer Access)

Trang chủ và Trang Chi tiết Phim được thiết lập để có thể truy cập hoàn toàn công khai:
* **Guest (Khách vãng lai):** Không cần đăng nhập vẫn xem được danh sách phim, chuyển trang, mở xem chi tiết và xem trailer.
* **Customer (Khách hàng đã đăng nhập):** Có trải nghiệm tương tự nhưng có quyền thực hiện các bước đặt vé (booking) sau này.
* **Quy tắc bảo mật:** Hai tuyến đường `/` (hoặc `/home`) và `/movies/:movieId` **không** được bọc trong bộ lọc bảo vệ `ProtectedRoute`.
