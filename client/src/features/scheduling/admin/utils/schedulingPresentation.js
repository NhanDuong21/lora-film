const UNKNOWN_PRESENTATION = Object.freeze({
  label: 'Không xác định',
  description: 'Giá trị này chưa có cách trình bày dành cho người dùng.',
  tone: 'zinc',
});

export const PREVIEW_STATUS_PRESENTATION = Object.freeze({
  GENERATING: { label: 'Đang tạo bản xem trước', tone: 'blue' },
  PREVIEWED: { label: 'Sẵn sàng rà soát', tone: 'green' },
  APPLYING: { label: 'Đang áp dụng', tone: 'blue' },
  APPLIED: { label: 'Đã áp dụng', tone: 'violet' },
  EXPIRED: { label: 'Đã hết hạn', tone: 'amber' },
  FAILED: { label: 'Thất bại', tone: 'red' },
  CANCELLED: { label: 'Đã hủy', tone: 'zinc' },
});

export const CANDIDATE_VALIDATION_PRESENTATION = Object.freeze({
  VALID: {
    label: 'Hợp lệ',
    description: 'Ứng viên đã vượt qua kiểm tra tại thời điểm tạo bản xem trước.',
    tone: 'green',
  },
  REJECTED: {
    label: 'Không hợp lệ',
    description: 'Ứng viên không vượt qua kiểm tra khi tạo bản xem trước.',
    tone: 'red',
  },
});

export const CANDIDATE_APPLY_PRESENTATION = Object.freeze({
  PENDING: {
    label: 'Đang chờ',
    description: 'Ứng viên đang chờ lựa chọn hoặc áp dụng.',
    tone: 'blue',
  },
  CREATED: {
    label: 'Đã tạo suất chiếu',
    description: 'Một suất chiếu chính thức đã được tạo từ ứng viên này.',
    tone: 'green',
  },
  SKIPPED: {
    label: 'Không được chọn',
    description: 'Ứng viên không được chọn khi bản xem trước được áp dụng.',
    tone: 'zinc',
  },
  CONFLICT: {
    label: 'Xung đột',
    description: 'Ứng viên gặp xung đột trong quá trình áp dụng.',
    tone: 'red',
  },
  FAILED: {
    label: 'Thất bại',
    description: 'Không thể tạo suất chiếu từ ứng viên này.',
    tone: 'red',
  },
});

export const SHOWTIME_STATUS_PRESENTATION = Object.freeze({
  DRAFT: { label: 'Bản nháp', tone: 'zinc' },
  OPEN_FOR_BOOKING: { label: 'Đang mở bán', tone: 'green' },
  CLOSED: { label: 'Đã đóng bán', tone: 'amber' },
  CANCELLED: { label: 'Đã hủy', tone: 'red' },
  FINISHED: { label: 'Đã chiếu xong', tone: 'zinc' },
});

export const SHOWTIME_TRANSITION_ACTION_PRESENTATION = Object.freeze({
  OPEN_FOR_BOOKING: { label: 'Mở bán' },
  CLOSED: { label: 'Đóng bán' },
  CANCELLED: { label: 'Hủy suất chiếu' },
  FINISHED: { label: 'Đánh dấu đã chiếu xong' },
});

export const SHOWTIME_SOURCE_PRESENTATION = Object.freeze({
  AUTO: { label: 'Tạo tự động', batchLabel: 'Đợt tạo tự động', tone: 'blue' },
  MANUAL: { label: 'Tạo thủ công', batchLabel: 'Đợt tạo thủ công', tone: 'zinc' },
});

export const APPLY_MODE_PRESENTATION = Object.freeze({
  ALL_OR_NOTHING: {
    label: 'Tất cả hoặc không tạo',
    description: 'Toàn bộ lựa chọn được tạo trong một giao dịch; nếu có lỗi, không suất chiếu nào được tạo.',
  },
});

export const SCORE_COMPONENT_PRESENTATION = Object.freeze({
  base: 'Điểm cơ bản',
  primeTime: 'Khung giờ cao điểm',
  offPeak: 'Khung giờ thấp điểm',
  earlySlot: 'Đầu ca vận hành',
  auditoriumFit: 'Mức phù hợp phòng chiếu',
  scheduleContinuity: 'Độ liền mạch lịch phòng',
});

export const BATCH_STATUS_REASON_PRESENTATION = Object.freeze({
  INVALID_SHOWTIME_STATUS_TRANSITION: {
    label: 'Trạng thái hiện tại không cho phép mở bán',
  },
  SHOWTIME_CANNOT_OPEN_AFTER_START: {
    label: 'Suất chiếu đã bắt đầu hoặc thời điểm bắt đầu đã qua',
  },
  SHOWTIME_PRICE_MISSING: {
    label: 'Chưa có bảng giá đầy đủ',
  },
  PRICING_INCOMPLETE: {
    label: 'Chưa có bảng giá đầy đủ',
  },
  PRICE_POLICY_NOT_FOUND: {
    label: 'Không có chính sách giá hiệu lực',
  },
  PRICING_AMBIGUOUS: {
    label: 'Có nhiều quy tắc giá cùng mức ưu tiên',
  },
  PRICE_POLICY_OVERLAP: {
    label: 'Các quy tắc giá đang bị chồng lấn',
  },
  INVALID_CINEMA_TIMEZONE: {
    label: 'Múi giờ của rạp không hợp lệ',
  },
  SHOWTIME_OUTSIDE_RELEASE_WINDOW: {
    label: 'Phim nằm ngoài thời gian được phép chiếu',
  },
  MOVIE_NOT_AVAILABLE_FOR_SCHEDULING: {
    label: 'Phim không khả dụng để xếp lịch',
  },
  MOVIE_VERSION_NOT_ACTIVE: {
    label: 'Phiên bản phim không hoạt động',
  },
  CINEMA_NOT_ACTIVE: {
    label: 'Rạp chiếu phim không hoạt động',
  },
  AUDITORIUM_NOT_ACTIVE: {
    label: 'Phòng chiếu không hoạt động',
  },
  CINEMA_OPERATING_HOURS_NOT_CONFIGURED: {
    label: 'Rạp chưa cấu hình giờ hoạt động',
  },
  SHOWTIME_OUTSIDE_OPERATING_HOURS: {
    label: 'Suất chiếu nằm ngoài giờ hoạt động của rạp',
  },
  SHOWTIME_OVERLAPS_CINEMA_CLOSURE: {
    label: 'Suất chiếu trùng thời gian rạp đóng cửa',
  },
  SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE: {
    label: 'Suất chiếu trùng thời gian bảo trì phòng chiếu',
  },
});

const getPresentation = (map, value) => ({
  ...UNKNOWN_PRESENTATION,
  ...(map[value] || {}),
  technicalValue: value || null,
});

export const getPreviewStatusPresentation = value => getPresentation(
  PREVIEW_STATUS_PRESENTATION,
  value,
);

export const getCandidateValidationPresentation = value => getPresentation(
  CANDIDATE_VALIDATION_PRESENTATION,
  value,
);

export const getCandidateApplyPresentation = value => getPresentation(
  CANDIDATE_APPLY_PRESENTATION,
  value,
);

export const getShowtimeStatusPresentation = value => getPresentation(
  SHOWTIME_STATUS_PRESENTATION,
  value,
);

export const getShowtimeTransitionActionPresentation = value => getPresentation(
  SHOWTIME_TRANSITION_ACTION_PRESENTATION,
  value,
);

export const getShowtimeSourcePresentation = value => getPresentation(
  SHOWTIME_SOURCE_PRESENTATION,
  value,
);

export const getApplyModePresentation = value => getPresentation(
  APPLY_MODE_PRESENTATION,
  value,
);

export const getBatchStatusReasonPresentation = value => {
  const presentation = BATCH_STATUS_REASON_PRESENTATION[value];
  if (presentation) {
    return {
      ...UNKNOWN_PRESENTATION,
      ...presentation,
      technicalValue: value,
      isFallback: false,
    };
  }
  return {
    ...UNKNOWN_PRESENTATION,
    label: 'Không xác định — xem chi tiết kỹ thuật',
    technicalValue: value || null,
    isFallback: true,
  };
};

export const getScoreBreakdownRows = breakdown => Object.entries(breakdown || {}).map(
  ([key, value]) => ({
    key,
    label: SCORE_COMPONENT_PRESENTATION[key] || key,
    value,
  }),
);

export const getPreviewShortCode = previewPublicId => {
  const normalized = String(previewPublicId || '').replace(/[^a-zA-Z0-9]/g, '');
  return normalized ? normalized.slice(0, 8).toUpperCase() : '—';
};

export const getHistoryActorLabel = changedBy => (
  changedBy == null ? 'Không xác định' : `Người dùng #${changedBy}`
);

export const getLocalizedHistoryReason = reason => {
  const normalized = String(reason || '').trim();
  if (!normalized) return 'Không ghi nhận lý do';
  if (normalized === 'Showtime created') return 'Đã tạo suất chiếu';
  return normalized;
};
