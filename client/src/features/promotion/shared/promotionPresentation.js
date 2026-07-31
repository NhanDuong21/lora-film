export const CAMPAIGN_TYPES = ['COUPON', 'VOUCHER', 'AUTOMATIC_DISCOUNT'];
export const CREATABLE_CAMPAIGN_TYPES = ['COUPON', 'VOUCHER'];
export const CAMPAIGN_STATUSES = ['DRAFT', 'SCHEDULED', 'ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED'];
export const CAMPAIGN_APPROVAL_STATUSES = ['DRAFT', 'PENDING', 'APPROVED', 'REJECTED'];
export const CAMPAIGN_TRANSITIONS = ['SUBMIT', 'PUBLISH', 'ACTIVATE', 'PAUSE', 'KILL_SWITCH', 'CANCEL'];
export const LEGAL_STATUSES = ['PENDING', 'PASSED', 'FAILED'];
export const RULE_TYPES = ['DISCOUNT_TICKET', 'DISCOUNT_COMBO', 'TIER_BENEFIT', 'HAPPY_WEDNESDAY'];
export const COUPON_TYPES = ['PUBLIC', 'PRIVATE', 'SYSTEM', 'COMPENSATION', 'SINGLE_USE'];
export const COUPON_STATUSES = ['DRAFT', 'ACTIVE', 'DISABLED', 'USED', 'LOCKED', 'EXPIRED', 'CANCELLED'];
export const DISTRIBUTION_TYPES = ['PUBLIC', 'PRIVATE', 'TARGETED', 'AUTO'];
export const VOUCHER_TYPES = ['FIXED_AMOUNT', 'PERCENTAGE', 'FREE_TICKET', 'FREE_COMBO', 'CASHBACK', 'REWARD', 'MEMBERSHIP', 'COMPENSATION'];
export const VOUCHER_SOURCES = ['CAMPAIGN', 'MANUAL', 'BIRTHDAY', 'TIER_UPGRADE', 'POINT_REDEEM', 'COMPENSATION', 'SYSTEM'];
export const VOUCHER_STATUSES = ['ISSUED', 'ACTIVE', 'USED', 'REVOKED', 'EXPIRED', 'CANCELLED', 'LOCKED'];
export const REDEMPTION_TYPES = ['COUPON', 'VOUCHER'];
export const REDEMPTION_STATUSES = ['SUCCESS', 'CONFIRMED', 'ROLLED_BACK', 'REFUNDED', 'CANCELLED'];
export const RESERVATION_STATUSES = ['ACTIVE', 'COMPLETED', 'RELEASED', 'EXPIRED', 'CANCELLED'];
export const COMPENSATION_TYPES = ['PAYMENT_FAILURE', 'BOOKING_FAILURE', 'SHOW_CANCELLED', 'SYSTEM_ERROR', 'CUSTOMER_SERVICE', 'MANUAL'];
export const COMPENSATION_STATUSES = ['ISSUED', 'CANCELLED'];
export const ACTION_TYPES = ['PERCENTAGE', 'FIXED_AMOUNT', 'FREE_TICKET', 'FREE_COMBO', 'FREE', 'CASHBACK'];

export const promotionLabels = {
  COUPON: 'Coupon',
  VOUCHER: 'Voucher',
  AUTOMATIC_DISCOUNT: 'Tự động giảm giá',
  DRAFT: 'Đang soạn',
  SCHEDULED: 'Đã lên lịch',
  ACTIVE: 'Đang áp dụng',
  PAUSED: 'Tạm dừng',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  PASSED: 'Đạt pháp lý',
  FAILED: 'Không đạt',
  DISCOUNT_TICKET: 'Giảm tiền vé',
  DISCOUNT_COMBO: 'Giảm combo',
  TIER_BENEFIT: 'Ưu đãi hạng thẻ',
  HAPPY_WEDNESDAY: 'Happy Wednesday',
  PUBLIC: 'Công khai',
  PRIVATE: 'Riêng tư',
  SYSTEM: 'Hệ thống',
  COMPENSATION: 'Bồi thường',
  SINGLE_USE: 'Dùng một lần',
  TARGETED: 'Theo nhóm khách',
  AUTO: 'Tự động',
  DISABLED: 'Vô hiệu hóa',
  USED: 'Đã dùng',
  LOCKED: 'Đang khóa',
  EXPIRED: 'Hết hạn',
  ISSUED: 'Đã phát hành',
  REVOKED: 'Đã thu hồi',
  FIXED_AMOUNT: 'Giảm tiền cố định',
  PERCENTAGE: 'Giảm phần trăm',
  FREE_TICKET: 'Miễn phí vé',
  FREE_COMBO: 'Miễn phí combo',
  CASHBACK: 'Hoàn tiền',
  REWARD: 'Phần thưởng',
  MEMBERSHIP: 'Thành viên',
  MANUAL: 'Thủ công',
  BIRTHDAY: 'Sinh nhật',
  TIER_UPGRADE: 'Nâng hạng',
  POINT_REDEEM: 'Đổi điểm',
  SUCCESS: 'Thành công',
  CONFIRMED: 'Đã xác nhận',
  ROLLED_BACK: 'Đã hoàn tác',
  REFUNDED: 'Đã hoàn tiền',
  RELEASED: 'Đã giải phóng',
  PAYMENT_FAILURE: 'Lỗi thanh toán',
  BOOKING_FAILURE: 'Lỗi đặt vé',
  SHOW_CANCELLED: 'Hủy suất chiếu',
  SYSTEM_ERROR: 'Lỗi hệ thống',
  CUSTOMER_SERVICE: 'CSKH',
  SUBMIT: 'Gửi duyệt',
  PUBLISH: 'Công bố',
  ACTIVATE: 'Kích hoạt',
  PAUSE: 'Tạm dừng',
  KILL_SWITCH: 'Dừng khẩn cấp',
  CANCEL: 'Hủy',
  FREE: 'Miễn phí toàn bộ',
};

export const labelFor = value => promotionLabels[value] || value || 'Chưa xác định';

export const currency = (value, code = 'VND') =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: code || 'VND' })
    .format(Number(value || 0));

export const formatDateTime = value => {
  if (!value) return 'Chưa có';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date);
};

export const toDateTimeLocal = value => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

export const fromDateTimeLocal = value => {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
};

export const normalizePage = (page, fallbackSize = 20) => ({
  content: Array.isArray(page?.content) ? page.content : [],
  page: page?.page ?? page?.number ?? 0,
  size: page?.size ?? fallbackSize,
  totalElements: page?.totalElements ?? 0,
  totalPages: page?.totalPages ?? 0,
  last: page?.last ?? true,
});

export const badgeClass = status => {
  const normalized = String(status || '').toUpperCase();
  if (['ACTIVE', 'APPROVED', 'PASSED', 'SUCCESS', 'CONFIRMED', 'COMPLETED', 'ISSUED'].includes(normalized)) {
    return 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300';
  }
  if (['DRAFT', 'PENDING', 'SCHEDULED', 'LOCKED'].includes(normalized)) {
    return 'border-amber-500/30 bg-amber-500/10 text-amber-300';
  }
  if (['PAUSED', 'RELEASED', 'ROLLED_BACK', 'REFUNDED'].includes(normalized)) {
    return 'border-sky-500/30 bg-sky-500/10 text-sky-300';
  }
  if (['CANCELLED', 'REJECTED', 'FAILED', 'DISABLED', 'REVOKED'].includes(normalized)) {
    return 'border-red-500/30 bg-red-500/10 text-red-300';
  }
  return 'border-zinc-700 bg-zinc-800 text-zinc-300';
};

export const safeJsonParse = (value, fallback = {}) => {
  if (value && typeof value === 'object') return { value, error: '' };
  if (!value || !String(value).trim()) return { value: fallback, error: '' };
  try {
    return { value: JSON.parse(value), error: '' };
  } catch {
    return { value: fallback, error: 'JSON chưa đúng định dạng.' };
  }
};

export const jsonString = value => {
  if (value === undefined || value === null || value === '') return '{}';
  if (typeof value === 'string') return value;
  return JSON.stringify(value, null, 2);
};

export const defaultConditions = (minimumOrderAmount = 0) => ({
  ...(Number(minimumOrderAmount) > 0 ? { minimumOrderAmount: Number(minimumOrderAmount) } : {}),
});

export const actionFromPreset = ({ actionType, actionValue, maxDiscountAmount }) => {
  const type = actionType || 'PERCENTAGE';
  const action = { discountType: type };
  if (['PERCENTAGE', 'FIXED_AMOUNT', 'CASHBACK'].includes(type)) {
    action.discountValue = Number(actionValue || 0);
  }
  if (type === 'PERCENTAGE' && Number(maxDiscountAmount) > 0) {
    action.maxDiscountAmount = Number(maxDiscountAmount);
  }
  return action;
};

export const friendlyPromotionError = error => {
  const data = error?.response?.data || error?.data || {};
  if (Array.isArray(data.details) && data.details.length > 0) {
    return data.details.map(item => item.message).join(' ');
  }
  const message = data.message || error?.message || 'Không thể hoàn tất thao tác khuyến mãi.';
  return message
    .replace('Invalid request parameters', 'Thông tin nhập chưa hợp lệ')
    .replace('Access denied', 'Bạn chưa có quyền thực hiện thao tác này')
    .replace(
      'AUTOMATIC_DISCOUNT is not supported by the current checkout runtime',
      'Checkout hiện chưa hỗ trợ chiến dịch giảm giá tự động. Hãy dùng chiến dịch Coupon hoặc Voucher.'
    );
};

export const fieldErrors = error => {
  const details = error?.response?.data?.details || error?.data?.details || [];
  return details.reduce((result, item) => ({
    ...result,
    [String(item.field || '').split('.').pop()]: item.message,
  }), {});
};

export const voucherDiscountSummary = voucher => {
  const actions = typeof voucher?.actionsJson === 'string'
    ? safeJsonParse(voucher.actionsJson).value
    : voucher?.actionsJson;
  const action = Array.isArray(actions) ? actions[0] : actions;
  const type = action?.discountType || action?.type || action?.actionType || voucher?.voucherType;
  if (type === 'PERCENTAGE' || type === 'PERCENT') {
    const value = action?.discountValue ?? action?.value ?? action?.percentage;
    const cap = action?.maxDiscountAmount ?? action?.maximumDiscountAmount ?? action?.maxAmount;
    return `${value || 0}%${cap ? `, tối đa ${currency(cap)}` : ''}`;
  }
  if (['FIXED_AMOUNT', 'AMOUNT', 'CASHBACK'].includes(type)) {
    return currency(action?.discountValue ?? action?.value ?? action?.amount ?? voucher?.faceValue);
  }
  if (type === 'FREE_TICKET') return 'Miễn phí vé đủ điều kiện';
  if (type === 'FREE_COMBO') return 'Miễn phí combo đủ điều kiện';
  if (type === 'FREE' || type === 'FULL_DISCOUNT') return 'Giảm toàn bộ đơn đủ điều kiện';
  return voucher?.faceValue ? currency(voucher.faceValue) : labelFor(type);
};

export const conditionSummary = conditionsJson => {
  const conditions = typeof conditionsJson === 'string'
    ? safeJsonParse(conditionsJson).value
    : conditionsJson;
  if (!conditions || Object.keys(conditions).length === 0) return 'Không có điều kiện bổ sung.';
  const parts = [];
  const minimum = conditions.minimumOrderAmount ?? conditions.minOrderAmount;
  if (minimum) parts.push(`Đơn tối thiểu ${currency(minimum)}`);
  if (Array.isArray(conditions.allowedUserIds) && conditions.allowedUserIds.length) {
    parts.push(`Chỉ ${conditions.allowedUserIds.length} khách hàng được chọn`);
  }
  if (Array.isArray(conditions.movieIds) && conditions.movieIds.length) parts.push('Giới hạn theo phim');
  if (Array.isArray(conditions.cinemaIds) && conditions.cinemaIds.length) parts.push('Giới hạn theo rạp');
  if (Array.isArray(conditions.dayOfWeek) && conditions.dayOfWeek.length) {
    parts.push(`Áp dụng ${conditions.dayOfWeek.join(', ')}`);
  }
  if (conditions.requiredTierCode) parts.push(`Yêu cầu hạng ${conditions.requiredTierCode}`);
  if (conditions.requiresVerification) parts.push('Cần xác thực khách hàng');
  return parts.length ? parts.join(' · ') : 'Có điều kiện nâng cao trong JSON.';
};
