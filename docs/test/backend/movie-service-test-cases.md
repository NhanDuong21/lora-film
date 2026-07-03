# Tài Liệu Kiểm Thử Hệ Thống - Movie Service

## Lịch sử chỉnh sửa

**Ngày:** 30/06/2026

---

## 1. Tổng Quan
Tài liệu này đặc tả danh sách các kịch bản kiểm thử (Test Cases) hiện tại của dịch vụ `movie-service`. Các kịch bản này bao phủ các tầng xử lý dữ liệu từ Repository, Service, đến Controller cho các thực thể như Genre (Thể loại), Movie (Phim), Room (Phòng chiếu) và cấu hình xác thực (Security).

---

## 2. Danh Sách Kịch Bản Kiểm Thử Nội Bộ (Unit/Integration Tests)

### 2.1. Phân Hệ Thể Loại (Genre)

**Admin Genre Controller (`AdminGenreControllerTest`)**
- `createGenre_ShouldReturn401_WhenNoToken`: Khởi tạo thể loại thất bại khi không gửi Token (401 Unauthorized).
- `createGenre_ShouldReturn403_WhenNotAdmin`: Khởi tạo thể loại thất bại khi tài khoản thao tác không có quyền Admin (403 Forbidden).
- `createGenre_ShouldReturnCreated_WhenAdmin`: Khởi tạo thể loại thành công với tài khoản Admin hợp lệ (201 Created).
- `createGenre_ShouldReturn400_WhenInvalidRequest`: Khởi tạo thể loại thất bại do payload gửi lên không hợp lệ (400 Bad Request).
- `createGenre_ShouldReturn409_WhenDuplicate`: Khởi tạo thể loại thất bại do bị trùng lặp tên thể loại (409 Conflict).
- `updateGenre_ShouldReturnOk_WhenAdmin`: Cập nhật thông tin thể loại thành công với quyền Admin (200 OK).
- `updateGenre_ShouldReturn404_WhenNotFound`: Cập nhật thể loại thất bại do không tìm thấy ID cung cấp (404 Not Found).

**Public Genre Controller (`PublicGenreControllerTest`)**
- `getGenres_ShouldReturnOk_WithoutToken`: Lấy danh sách các thể loại thành công đối với người dùng chưa đăng nhập.
- `getGenreById_ShouldReturnOk_WithoutToken`: Lấy thông tin một thể loại cụ thể qua ID thành công.

**Genre Service (`GenreServiceImplTest`)**
- `getGenres_ShouldReturnList`: Dịch vụ lấy danh sách trả về mảng kết quả thể loại chuẩn xác.
- `getGenreById_ShouldReturnGenre_WhenExists`: Trả về chính xác thông tin thể loại khi ID tồn tại trong CSDL.
- `getGenreById_ShouldThrowNotFound_WhenNotExists`: Ném ngoại lệ NotFound khi ID thể loại không tồn tại.
- `createGenre_ShouldReturnGenre_WhenValidAndNotExists`: Lưu trữ thành công thể loại mới khi dữ liệu hợp lệ và không trùng lặp.
- `createGenre_ShouldThrowConflict_WhenDuplicate`: Bắt lỗi ngoại lệ Conflict khi cố tình tạo thể loại đã có tên.
- `updateGenre_ShouldReturnGenre_WhenValid`: Sửa đổi thông tin đổi mới thể loại lưu thành công.
- `updateGenre_ShouldThrowConflict_WhenDuplicate`: Chặn cập nhật sang một tên thể loại đã tồn tại (xung đột dữ liệu).
- `updateGenre_ShouldThrowNotFound_WhenNotExists`: Cố gắng cập nhật trên một thể loại không tồn tại thì ném ngoại lệ NotFound.

**Genre Repository (`GenreRepositoryTest`)**
- `shouldSaveAndFindGenre`: Kiểm tra kết nối CSDL khi lưu và truy vấn thể loại hoạt động.
- `shouldFindAllOrderedByName`: Đảm bảo câu lệnh SQL lấy toàn bộ thể loại và sắp xếp đúng theo tên.
- `shouldCheckExistsIgnoreCase`: Hàm rà soát tồn tại tên (không phân biệt ký tự hoa/thường) qua CSDL.
- `shouldCheckExistsIgnoreCaseExcludeId`: Đảm bảo bỏ qua ID hiện hành khi check trùng lặp lúc cập nhật.

---

### 2.2. Phân Hệ Phim (Movie)

**Admin Movie Controller (`AdminMovieControllerTest`)**
- `getAdminMovieDetail_Success`: Truy xuất chi tiết phim cấp quyền Admin thành công.
- `createMovie_Success`: API Controller nhận dữ liệu và chỉ định tạo phim thành công.
- `updateMovieStatus_Success`: Cập nhật thay đổi trạng thái phim thành công.

**Public Movie Controller (`PublicMovieControllerTest`)**
- `testGetMovies`: Phản hồi đúng đắn danh sách phim từ API phía người dùng (Client/Public).
- `testGetMovieDetail`: Phản hồi chi tiết từng bộ phim dựa trên thông tin phía Client gọi.

**Movie Service (`MovieServiceTest`)**
- `getMovieDetail_Success`: Nghiệp vụ lấy thông tin phim trả về DTO tương ứng thành công.
- `getMovieDetail_NotFound`: Xử lý chặn nếu khách lấy thông tin phim với ID sai lệch.
- `getMovieDetail_Inactive`: Xử lý chặn, ẩn thông tin đối với các bộ phim đã không còn hoạt động (ngừng chiếu).
- `getMovieDetail_InvalidFormat`: Phản hồi lỗi khi truy vấn lấy phim cung cấp định dạng bị sai.
- `getMovies_Success`: Hệ thống xử lý phân trang tốt khi lấy danh sách phim.
- `getMovies_InvalidPagination`: Phản ứng trước những tham số phân trang nằm ngoài phạm vi cho phép.
- `getMovies_GenreNotFound`: Không tìm thấy dữ liệu hoặc thông báo lỗi nếu lọc theo Thể loại bị sai.
- `getAdminMovieDetail_Success`: Dữ liệu phân tách cho Admin thể hiện đủ thông tin phim (kể cả logic ẩn/hiện).
- `getAdminMovieDetail_Inactive_Success`: Quản trị viên (Admin) xem chi tiết phim ngay cả khi nó đang Inactive.
- `createMovie_Success`: Chạy nghiệp vụ tính toán và lưu đầy đủ thông tin phim tạo mới.
- `createMovie_InvalidDateRange`: Bắt ràng buộc logic thời gian khởi chiếu, thời gian kết thúc không đúng trình tự.
- `updateMovieStatus_Success`: Thay đổi trạng thái bộ phim hoàn thành như kỳ vọng.
- `updateMovieStatus_InvalidTransition`: Bắt lỗi đối với những bước chuyển đổi trạng thái không khớp với quy trình luồng trạng thái của phim.
- `updateMovie_DurationChangeBlocked`: Chặn cập nhật thay đổi thông tin về "thời lượng phim" để tránh gây rủi ro lệch lịch chiếu.

**Movie Repository (`MovieRepositoryTest`)**
- `testSaveAndFindById`: Kiểm tra chức năng Save và Find hoạt động liên kết với Database.
- `testFindAllWithSpec_PaginationAndJoin`: Đảm bảo các hàm tìm kiếm cùng Specification có nối bảng tương ứng và phân trang chuẩn.
- `testFindAllWithSpec_InactiveVisibility`: Bộ lọc Specification tính toán đúng với trạng thái Inactive của phim (được nhìn thấy hay bị ẩn trên các role).

---

### 2.3. Phân Hệ Phòng Chiếu (Room)

**Admin Room Controller (`AdminRoomControllerTest`)**
- `testCreateRoom_Success`: Controller phòng chiếu nhận thông tin và tạo thành công thông qua API.
- `testCreateRoom_ValidationError`: API phản hồi lỗi (ValidationError) khi dữ liệu truyền lên không đủ hoặc sai quy chuẩn.

**Room Service (`RoomServiceImplTest`)**
- `testCreateRoom_Success`: Nghiệp vụ tạo phòng chiếu hoạt động xuất sắc.
- `testCreateRoom_DuplicateName`: Service từ chối yêu cầu tạo phòng chiếu mới nếu tên bị trùng với phòng chiếu đang có.
- `testCreateRoom_DataIntegrityException`: Đề phòng trường hợp lỗi ràng buộc dữ liệu tại CSDL ném lên từ DataIntegrityViolationException.
- `testUpdateRoomStatus_InvalidTransition`: Ngăn chặn thay đổi trạng thái theo những kiểu bất hợp lý (ví dụ: đang bảo trì sang hoạt động khi chưa sẵn sàng).
- `testUpdateRoomStatus_FutureShowtimes`: Không cho phép ẩn/vô hiệu hóa phòng chiếu nếu nó vẫn còn đang gánh vác các suất chiếu trong tương lai.

**Room Repository (`RoomRepositoryTest`)**
- `testExistsByRoomNameIgnoreCase`: Rà soát trực tiếp tại DB xem tên phòng chiếu đã được sở hữu hay chưa.
- `testExistsByRoomNameIgnoreCaseAndIdNot`: Tương tự như Genre, check tên bị trùng lặp lúc cập nhật nhưng bỏ qua phòng đó.

---

### 2.4. Xác Thực Và Uỷ Quyền (Security/Auth)

**JWT Provider Integration (`JwtProviderIntegrationTest`)**
- `validateToken_ShouldReturnTrue_WhenTokenIsGeneratedByAuthServiceLogic`: Khẳng định sự đồng bộ khóa JWT giữa `auth-service` và `movie-service`. Mã Token được phát hành bởi dịch vụ xác thực hoàn toàn được giải mã định dạng và chấp thuận trên dịch vụ hệ thống phim.
