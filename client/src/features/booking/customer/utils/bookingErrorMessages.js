import {
  getCustomerErrorCode,
  getCustomerErrorMessage
} from '@/utils/customerErrorMessages';

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
  BOOKING_ACTIVE_SHOWTIME_EXISTS: 'Bạn đã có một đơn đang giữ ghế cho suất chiếu này. Vui lòng tiếp tục thanh toán hoặc hủy đơn cũ trước khi chọn lại.',
  BOOKING_NOT_FOUND: 'Không tìm thấy đơn đặt vé này.',
  BOOKING_OWNER_REQUIRED: 'Bạn không có quyền xem hoặc thay đổi đơn đặt vé này.',
  BOOKING_NOT_PENDING: 'Đơn không còn ở trạng thái chờ thanh toán.',
  BOOKING_NOT_MODIFIABLE: 'Đơn không còn cho phép thay đổi bắp nước.',
  BOOKING_AMOUNT_LOCKED: 'Đơn đã chốt số tiền nên không thể thay đổi bắp nước.',
  BOOKING_RESERVATIONS_NOT_HELD: 'Ghế của đơn không còn được giữ. Vui lòng chọn lại ghế.',
  BOOKING_SEAT_SELECTION_REQUIRED: 'Vui lòng chọn ít nhất một ghế.',
  BOOKING_RESERVATION_OWNER_MISMATCH: 'Bạn không có quyền sử dụng lượt giữ ghế này.',
  BOOKING_RESERVATION_EXPIRED: 'Lượt giữ ghế đã hết hạn. Vui lòng chọn lại ghế.',
  FOOD_NOT_FOUND: 'Không tìm thấy món bắp nước đã chọn.',
  FOOD_NOT_AVAILABLE: 'Món bắp nước này hiện không còn phục vụ.',
  ORDER_NOT_MODIFIABLE: 'Đơn bắp nước không còn cho phép thay đổi.',
  DUPLICATE_REQUEST: 'Yêu cầu đang được xử lý. Vui lòng chờ trong giây lát.',
  PAYMENT_SERVICE_HANDOFF_REQUIRED: 'Hệ thống thanh toán chưa sẵn sàng. Vui lòng thử lại sau.',
  CONFIRM_VIA_PAYMENT_RESULT_REQUIRED: 'Đơn chỉ được xác nhận từ kết quả hợp lệ của hệ thống thanh toán.',
  REFUND_VIA_PAYMENT_RESULT_REQUIRED: 'Hoàn tiền chỉ được ghi nhận từ kết quả hợp lệ của hệ thống thanh toán.',
  BOOKING_EXPIRY_SYSTEM_OWNED: 'Trạng thái hết hạn được hệ thống tự động quản lý theo thời hạn giữ ghế.',
  BOOKING_PENDING_STATE_IMMUTABLE: 'Không thể đưa đơn trở lại trạng thái chờ thanh toán.',
  ADMIN_LIFECYCLE_COMMAND_NOT_ALLOWED: 'Không thể thực hiện thao tác này với trạng thái hiện tại của đơn.',
  SAME_STATUS_TRANSITION: 'Đơn đã ở trạng thái được chọn.',
  IDEMPOTENCY_PAYLOAD_CONFLICT: 'Phiên đặt vé cũ không còn phù hợp với ghế bạn vừa chọn. Vui lòng bấm tiếp tục lại.',
  BOOKING_IDEMPOTENCY_PAYLOAD_CONFLICT: 'Phiên đặt vé cũ không còn phù hợp với ghế bạn vừa chọn. Vui lòng bấm tiếp tục lại.',
  SHOWTIME_002: 'Không thể giữ ghế cho suất chiếu đã bắt đầu.',
  SEAT_COUPLE_PAIR_REQUIRED: 'Vui lòng chọn đủ hai ghế đôi.',
  SEAT_SINGLE_GAP_NOT_ALLOWED: 'Không được để lại một ghế trống đơn lẻ. Vui lòng chọn lại ghế.',
  SHOWTIME_VALIDATION_UNAVAILABLE: 'Không thể xác thực suất chiếu lúc này. Vui lòng thử lại.',
  INTERNAL_SERVER_ERROR: 'Hệ thống đang bận. Vui lòng thử lại sau.'
};

export const getBookingErrorCode = getCustomerErrorCode;

export const getBookingErrorMessage = (
  error,
  fallback = 'Không thể giữ ghế. Vui lòng thử lại.'
) => {
  return getCustomerErrorMessage(error, fallback, BOOKING_ERROR_MESSAGES);
};

export const seatConflictErrorCodes = new Set([
  'BOOKING_SEAT_CONFLICT',
  'SEAT_SINGLE_GAP_NOT_ALLOWED',
  'SEAT_003',
  'SEAT_004',
  'SEAT_009'
]);
