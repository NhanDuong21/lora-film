const UNKNOWN_PRESENTATION = Object.freeze({
  label: 'Không xác định',
  description: 'Giá trị này chưa có cách trình bày dành cho người dùng.',
  tone: 'zinc',
});

export const PREVIEW_STATUS_PRESENTATION = Object.freeze({
  GENERATING: { label: 'Đang xếp lịch', tone: 'blue' },
  PREVIEWED: { label: 'Chờ kiểm tra', tone: 'green' },
  APPLYING: { label: 'Đang tạo suất chiếu', tone: 'blue' },
  APPLIED: { label: 'Đã tạo suất chiếu', tone: 'violet' },
  EXPIRED: { label: 'Đã hết hạn', tone: 'amber' },
  FAILED: { label: 'Thất bại', tone: 'red' },
  CANCELLED: { label: 'Đã hủy', tone: 'zinc' },
});

export const CANDIDATE_VALIDATION_PRESENTATION = Object.freeze({
  VALID: {
    label: 'Hợp lệ',
    description: 'Suất đề xuất đã vượt qua kiểm tra tại thời điểm tạo lịch.',
    tone: 'green',
  },
  REJECTED: {
    label: 'Không hợp lệ',
    description: 'Suất đề xuất chưa vượt qua kiểm tra khi tạo lịch.',
    tone: 'red',
  },
});

export const CANDIDATE_APPLY_PRESENTATION = Object.freeze({
  PENDING: {
    label: 'Đang chờ',
    description: 'Suất đề xuất đang chờ bạn lựa chọn.',
    tone: 'blue',
  },
  CREATED: {
    label: 'Đã tạo suất chiếu',
    description: 'Một suất chiếu đang soạn đã được tạo từ đề xuất này.',
    tone: 'green',
  },
  SKIPPED: {
    label: 'Không được chọn',
    description: 'Suất đề xuất không được chọn khi tạo lịch.',
    tone: 'zinc',
  },
  CONFLICT: {
    label: 'Xung đột',
    description: 'Suất đề xuất gặp xung đột khi tạo lịch.',
    tone: 'red',
  },
  FAILED: {
    label: 'Thất bại',
    description: 'Không thể tạo suất chiếu từ đề xuất này.',
    tone: 'red',
  },
});

export const SHOWTIME_STATUS_PRESENTATION = Object.freeze({
  DRAFT: { label: 'Đang soạn', tone: 'zinc' },
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
  coverageSearchAdjustment: 'Điều chỉnh cân bằng phim',
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
    label: 'Chưa có bảng giá đang áp dụng',
  },
  PRICING_AMBIGUOUS: {
    label: 'Có nhiều mức giá phù hợp cùng lúc',
  },
  PRICE_POLICY_OVERLAP: {
    label: 'Có các mức giá bị trùng điều kiện',
  },
  INVALID_CINEMA_TIMEZONE: {
    label: 'Cấu hình giờ của rạp chưa đúng',
  },
  SHOWTIME_OUTSIDE_RELEASE_WINDOW: {
    label: 'Phim nằm ngoài thời gian được phép chiếu',
  },
  MOVIE_NOT_AVAILABLE_FOR_SCHEDULING: {
    label: 'Phim không khả dụng để xếp lịch',
  },
  MOVIE_VERSION_NOT_ACTIVE: {
    label: 'Định dạng phim không còn hoạt động',
  },
  CINEMA_NOT_ACTIVE: {
    label: 'Rạp đang tạm ngừng hoạt động',
  },
  AUDITORIUM_NOT_ACTIVE: {
    label: 'Phòng chiếu đang tạm ngừng hoạt động',
  },
  CINEMA_OPERATING_HOURS_NOT_CONFIGURED: {
    label: 'Rạp chưa thiết lập giờ hoạt động',
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
