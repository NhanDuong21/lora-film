import { normalizeApiError } from '@/utils/apiErrorHandler';

const ERROR_MESSAGES = Object.freeze({
  AUTO_SCHEDULE_DATE_RANGE_TOO_LARGE: 'Khoảng ngày vượt quá giới hạn 7 ngày cho một bản xem trước.',
  AUTO_SCHEDULE_DATE_RANGE_INVALID: 'Khoảng ngày lập lịch không hợp lệ.',
  AUTO_SCHEDULE_INVALID_DATE_RANGE: 'Khoảng ngày lập lịch không hợp lệ.',
  AUTO_SCHEDULE_DATE_IN_PAST: 'Ngày bắt đầu đã qua theo múi giờ của cụm rạp.',
  AUTO_SCHEDULE_CINEMA_NOT_FOUND: 'Không tìm thấy cụm rạp đã chọn.',
  CINEMA_NOT_FOUND: 'Không tìm thấy cụm rạp đã chọn.',
  AUTO_SCHEDULE_AUDITORIUM_NOT_FOUND: 'Một hoặc nhiều phòng chiếu không còn khả dụng.',
  AUDITORIUM_NOT_FOUND: 'Một hoặc nhiều phòng chiếu không còn khả dụng.',
  AUTO_SCHEDULE_MOVIE_VERSION_NOT_FOUND: 'Một hoặc nhiều định dạng phim không còn khả dụng.',
  MOVIE_VERSION_NOT_FOUND: 'Một hoặc nhiều định dạng phim không còn khả dụng.',
  AUTO_SCHEDULE_CANDIDATE_LIMIT_EXCEEDED: 'Phạm vi đã chọn tạo quá nhiều ứng viên. Hãy giảm số ngày, phòng hoặc định dạng phim.',
  AUTO_SCHEDULE_TOO_MANY_CANDIDATES: 'Phạm vi đã chọn tạo quá nhiều ứng viên. Hãy giảm số ngày, phòng hoặc định dạng phim.',
  AUTO_SCHEDULE_IDEMPOTENCY_CONFLICT: 'Yêu cầu thử lại không khớp cấu hình ban đầu. Hãy kiểm tra cấu hình và gửi lại.',
  IDEMPOTENCY_KEY_REUSED: 'Yêu cầu thử lại không khớp cấu hình ban đầu. Hãy kiểm tra cấu hình và gửi lại.',
});

export const getAutoScheduleError = (error, fallback = 'Không thể tạo bản xem trước xếp lịch.') => {
  const normalized = normalizeApiError(error);
  const isPastDate = normalized.code === 'AUTO_SCHEDULE_INVALID_DATE_RANGE'
    && /cannot schedule in the past/i.test(normalized.message || '');
  return {
    ...normalized,
    message: isPastDate
      ? ERROR_MESSAGES.AUTO_SCHEDULE_DATE_IN_PAST
      : ERROR_MESSAGES[normalized.code] || normalized.message || fallback,
  };
};
