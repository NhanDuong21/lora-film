const blockerMessages = Object.freeze({
  CINEMA_NOT_ACTIVE: 'Rạp phải ở trạng thái đang hoạt động trước khi có thể tạo lịch.',
  NO_ELIGIBLE_VERSIONS: 'Không có phiên bản phim đang hoạt động nào phù hợp với khoảng ngày đã chọn.',
  NO_ELIGIBLE_AUDITORIUMS: 'Không có phòng chiếu đang hoạt động nào có sức chứa và thời gian vệ sinh hợp lệ.',
  NO_COMPATIBLE_PAIRS: 'Định dạng của các phim đủ điều kiện không tương thích với bất kỳ phòng chiếu đang hoạt động nào.',
  MISSING_OPERATING_HOURS: 'Mỗi ngày trong phạm vi lập lịch phải có giờ mở cửa và đóng cửa hợp lệ.',
  PRICING_INCOMPLETE: 'Bảng giá hiện tại chưa bao phủ tất cả phòng chiếu và khung giờ có thể xếp lịch.',
  PRICING_AMBIGUOUS: 'Có nhiều quy tắc giá cùng mức ưu tiên cao nhất; cần điều chỉnh để chỉ còn một quy tắc áp dụng.',
  PLANNING_RANGE_FULLY_BLOCKED: 'Tất cả khung giờ khả dụng đều đang bị chặn bởi lịch chiếu hiện có, thời gian rạp đóng cửa hoặc lịch bảo trì.',
});

export const getAutoScheduleBlockerMessage = blocker => (
  ((blocker?.details || []).length > 0 && blocker?.message)
  || blockerMessages[blocker?.code]
  || blocker?.message
  || 'Phạm vi lập lịch đang có điều kiện chưa được đáp ứng.'
);

export default blockerMessages;
