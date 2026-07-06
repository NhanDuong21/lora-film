# Tài Liệu Kiểm Thử Tích Hợp Hệ Thống - Movie Service

## Lịch sử chỉnh sửa

**Ngày:** 06/07/2026 | **Người chỉnh sửa:** Phan Tuấn Thành

* **Cập nhật Backend (Gộp API):** Loại bỏ các API `GET /api/admin/movies` và `GET /api/admin/genres`. Tích hợp phân quyền động (`isAdmin`) trực tiếp vào Public API.
* **Cập nhật Backend (Soft Delete):** Xóa bỏ các API `PATCH /status` thừa thãi, tích hợp luồng Xóa Mềm an toàn (Soft Delete) thông qua `DELETE` HTTP Method cho Movie, Room, Genre với các ràng buộc nghiệp vụ về suất chiếu.

---

## 1. Tổng Quan Kiến Trúc Và Định Tuyến Mạng

Tài liệu này đặc tả quy trình và kết quả kiểm thử tích hợp (E2E) đối với luồng Quản lý Dữ liệu Phim, Thể Loại và Phòng Chiếu trên hệ thống Đặt vé Xem phim trực tuyến LoraFilm. Các thao tác quản trị được kiểm soát an toàn qua API Gateway và `movie-service`.

### Sơ Đồ Định Tuyến Mạng Thực Tế

1. **React Frontend**: Gọi API qua cổng của Gateway `http://localhost:8080`.
2. **API Gateway**: Cấu hình tại cổng `http://localhost:8080`.
3. **Movie-Service**: Hoạt động nội bộ. Gateway ánh tuyến các yêu cầu `/api/movies/**`, `/api/genres/**`, `/api/admin/movies/**`, `/api/admin/rooms/**` về dịch vụ này.

---

## 2. Danh Sách Kịch Bản Kiểm Thử Tích Hợp (E2E Test Scenarios)

Dưới đây là các kịch bản kiểm thử tích hợp chi tiết, cung cấp hướng dẫn từng bước để QA/Tester có thể thực hiện kiểm tra thủ công thông qua Postman hoặc Frontend UI.

### Kịch Bản 1: Luồng Xem Danh Sách Phim Dựa Trên Phân Quyền (Role-Based Content Delivery)
- **Mục đích**: Xác nhận API `GET /api/movies` trả về kết quả khác nhau tùy thuộc vào Role (vai trò) của người gọi. Admin sẽ thấy cả phim đang ẩn (INACTIVE), còn Khách hàng chỉ thấy phim hợp lệ (UPCOMING, NOW_SHOWING, ENDED).
- **Các bước thực hiện**:
  1. Gửi request `GET http://localhost:8080/api/movies` **không có** Access Token (Khách vãng lai) hoặc bằng Access Token của tài khoản Customer.
  2. Gửi request `GET http://localhost:8080/api/movies` **kèm theo** Access Token của tài khoản Admin.
- **Kết quả mong đợi**:
  - Tại bước 1: Phản hồi HTTP 200 OK. Danh sách trả về không chứa bất kỳ phim nào có `status = INACTIVE`.
  - Tại bước 2: Phản hồi HTTP 200 OK. Danh sách trả về bao gồm toàn bộ phim, kể cả các phim bị ẩn (`INACTIVE`).
- **Trạng thái**: ĐẠT

### Kịch Bản 2: Xóa Mềm Phim Không Có Suất Chiếu Tương Lai (Safe Soft Delete Movie)
- **Mục đích**: Xác nhận tính năng Xóa Phim của Admin chỉ là xóa mềm (chuyển trạng thái sang INACTIVE) khi phim không vướng bận suất chiếu tương lai.
- **Các bước thực hiện**:
  1. Sử dụng Access Token Admin.
  2. Tạo mới một bộ phim không có lịch chiếu: `POST /api/admin/movies`.
  3. Gửi request `DELETE http://localhost:8080/api/admin/movies/{movieId}`.
- **Kết quả mong đợi**:
  - Máy chủ trả về HTTP 200/204 No Content/OK.
  - Kiểm tra Database bảng `movies`, bản ghi vẫn tồn tại nhưng trường `status` được cập nhật thành `INACTIVE`.
  - Phim này lập tức biến mất khỏi danh sách hiển thị của Khách hàng ở (Kịch bản 1).
- **Trạng thái**: ĐẠT

### Kịch Bản 3: Chặn Xóa Mềm Phim Có Suất Chiếu Tương Lai (Block Soft Delete with Future Showtimes)
- **Mục đích**: Đảm bảo an toàn dữ liệu và trải nghiệm khách hàng. Không cho phép ẩn một bộ phim nếu nó đang có vé mở bán hoặc lịch chiếu trong tương lai.
- **Các bước thực hiện**:
  1. Dùng Database hoặc API tạo một `Showtime` (Lịch chiếu) cho phim X với `endTime` lớn hơn thời gian hiện tại (`endTime > now()`).
  2. Sử dụng Access Token Admin gọi request `DELETE http://localhost:8080/api/admin/movies/{movieId}` đối với phim X.
- **Kết quả mong đợi**:
  - Hệ thống từ chối và trả về HTTP 409 Conflict hoặc 400 Bad Request.
  - Thông báo lỗi hiển thị rõ ràng: "Không thể xóa phim vì vẫn còn lịch chiếu trong tương lai."
  - Trạng thái phim trong Database vẫn giữ nguyên (ACTIVE/NOW_SHOWING).
- **Trạng thái**: ĐẠT

### Kịch Bản 4: Chặn Xóa Phòng Chiếu Trái Phép (Block Soft Delete Room)
- **Mục đích**: Xác minh quy tắc nghiệp vụ nghiêm ngặt: Phòng chiếu không thể bị đóng/xóa (chuyển sang INACTIVE) nếu phòng đó đã lên lịch suất chiếu trong tương lai (để tránh phải hủy vé khách đã mua).
- **Các bước thực hiện**:
  1. Thêm một suất chiếu vào phòng chiếu số Y cho tuần tới.
  2. Gọi request `DELETE http://localhost:8080/api/admin/rooms/{roomId}` đối với phòng Y.
- **Kết quả mong đợi**:
  - Hệ thống trả về lỗi HTTP 409 Conflict.
  - Thông báo lỗi: "Không thể xóa/đóng phòng vì phòng này đang có suất chiếu chưa hoàn thành."
- **Trạng thái**: ĐẠT

### Kịch Bản 5: Xóa Mềm Thể Loại Phim (Soft Delete Genre & Cascade)
- **Mục đích**: Đảm bảo Thể loại phim có thể bị vô hiệu hóa an toàn mà không gây sập cấu trúc CSDL do lỗi Khóa Ngoại (Foreign Key constraint).
- **Các bước thực hiện**:
  1. Sử dụng Access Token Admin, gọi `DELETE http://localhost:8080/api/admin/genres/{genreId}`.
- **Kết quả mong đợi**:
  - Trả về HTTP 200 OK.
  - Bảng `genres` cập nhật `status = INACTIVE`.
  - Các phim đang gắn với Thể loại này vẫn hiển thị bình thường, nhưng Thể loại đó sẽ bị ẩn khỏi bộ lọc công khai.
- **Trạng thái**: ĐẠT

---

## 3. Danh Sách Kịch Bản Kiểm Thử Nội Bộ (Unit/Integration Tests)

Bên cạnh các kịch bản kiểm định đầu cuối (E2E), đội ngũ lập trình viên đã viết sẵn các bộ kiểm tra tự động chạy ngầm dưới Spring Boot (MockMvc & Mockito) để bảo vệ luồng nghiệp vụ.

**Controller Level**
- `getMovies_Admin_Success`: Phân quyền Admin xem chi tiết phim bao gồm INACTIVE.
- `createGenre_ShouldReturn403_WhenNotAdmin`: Đảm bảo các API quản trị từ chối request nếu thiếu Role Admin.
- `deleteMovie_Success`: Kiểm tra định tuyến xóa mềm.

**Service Level**
- `getMovieDetail_Admin_Inactive_Success`: Xác nhận Service nhận cờ `isAdmin = true` và cho phép xuất dữ liệu INACTIVE.
- `softDeleteMovie_Success`: Nghiệp vụ xác thực thời gian `endTime` của Showtime trả về kết quả chuẩn xác.
- `testSoftDeleteRoom_FutureShowtimes`: Bắt lỗi Constraint cho Phòng chiếu qua Exception Handling.

**Repository Level**
- `existsByMovieIdAndEndTimeAfter`: Kiểm tra JPA Query truy vấn đúng các lịch chiếu nằm ở tương lai dựa theo thông số `LocalDateTime`.
- `existsByRoomIdAndEndTimeAfter`: Đảm bảo kiểm tra chéo Lịch chiếu với ID của phòng.

---

## 4. Kết Quả Nghiệm Thu (Acceptance Sign-off)

| Tiêu Chí Đánh Giá           | Kết Quả Đạt Được                                                                                                                                                                                                                                                                     | Kết Luận |
| :-------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| **Gộp Chức Năng Cấp API**  | Endpoint Public tự động thay đổi kết quả dựa trên mã Token mà không làm rò rỉ Security Context vào Service Layer. Mã Code tuân thủ Clean Architecture. | ĐẠT      |
| **Bảo Toàn Nghiệp Vụ Xóa (Soft Delete)** | Quá trình xóa mềm được chặn an toàn khi dữ liệu (Room/Movie) dính dáng tới các suất chiếu ở tương lai. | ĐẠT |
| **Bảo Toàn Schema Dữ Liệu** | Các thay đổi trên Database (`movieId`, `endTime` vào bảng `showtimes`) chạy ổn định với Script Migration mới nhất. | ĐẠT |
| **Bảo Trì & Coverage** | 53/53 Unit Test và Integration Test chạy thành công. Không có lỗi biên dịch. | ĐẠT |

*Kết luận chung*: Phân hệ `movie-service` đã tối ưu hóa toàn diện, sẵn sàng triển khai tích hợp mà không để lại rủi ro nghiệp vụ nào về việc thất thoát vé đã bán của Khách hàng.
