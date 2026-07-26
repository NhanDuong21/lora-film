const BOOKING_ERROR_MESSAGES = {
  BOOKING_SEAT_CONFLICT: 'Một hoặc nhiều ghế vừa được khách khác giữ. Sơ đồ ghế đã được cập nhật, vui lòng chọn lại.',
  SEAT_003: 'Một hoặc nhiều ghế đang được khách khác giữ. Vui lòng chọn ghế khác.',
  SEAT_004: 'Một hoặc nhiều ghế đã được bán. Vui lòng chọn ghế khác.',
  SEAT_009: 'Ghế đang được xử lý bởi người khác. Vui lòng thử lại sau giây lát.',
  BOOKING_TOO_MANY_SEATS: 'Bạn đã chọn quá số ghế được phép cho mỗi giao dịch.',
  BOOKING_TOO_MANY_RESERVATIONS: 'Số ghế trong giao dịch vượt quá giới hạn cho phép.',
  BOOKING_SHOWTIME_NOT_OPEN: 'Suất chiếu không còn mở bán.',
  BOOKING_SHOWTIME_STARTED: 'Suất chiếu đã bắt đầu; không thể giữ ghế.',
  BOOKING_SHOWTIME_ENDED: 'Suất chiếu đã kết thúc.',
  BOOKING_EXPIRED: 'Thời gian giữ ghế đã hết. Vui lòng chọn lại ghế.',
  BOOKING_IDEMPOTENCY_PAYLOAD_CONFLICT: 'Yêu cầu đặt vé đã thay đổi. Vui lòng làm mới và chọn lại ghế.',
  SHOWTIME_002: 'Không thể giữ ghế cho suất chiếu đã bắt đầu.',
  SEAT_COUPLE_PAIR_REQUIRED: 'Vui lòng chọn đủ hai ghế đôi.',
  SHOWTIME_VALIDATION_UNAVAILABLE: 'Không thể xác thực suất chiếu lúc này. Vui lòng thử lại.',
  INTERNAL_SERVER_ERROR: 'Hệ thống đang bận. Vui lòng thử lại sau.'
};

export const getBookingErrorCode = error => {
  const payload = error?.response?.data || error;
  return payload?.errorCode || payload?.code || error?.code;
};

export const getBookingErrorMessage = (
  error,
  fallback = 'Không thể giữ ghế. Vui lòng thử lại.'
) => {
  const payload = error?.response?.data || error;
  const code = getBookingErrorCode(error);
  return BOOKING_ERROR_MESSAGES[code] || payload?.message || error?.message || fallback;
};

export const seatConflictErrorCodes = new Set([
  'BOOKING_SEAT_CONFLICT',
  'SEAT_003',
  'SEAT_004',
  'SEAT_009'
]);
