export const CAMPAIGN_STATUSES = [
  "DRAFT",
  "SCHEDULED",
  "ACTIVE",
  "PAUSED",
  "COMPLETED",
  "CANCELLED",
];
export const CAMPAIGN_APPROVAL_STATUSES = [
  "DRAFT",
  "PENDING",
  "APPROVED",
  "REJECTED",
];
export const CAMPAIGN_TRANSITIONS = [
  "SUBMIT",
  "PUBLISH",
  "ACTIVATE",
  "PAUSE",
  "KILL_SWITCH",
  "CANCEL",
];
export const LEGAL_STATUSES = ["PENDING", "PASSED", "FAILED"];
export const PROMOTION_TYPES = ["AUTO", "VOUCHER", "COUPON"];
export const PROMOTION_STATUSES = [
  "DRAFT",
  "ACTIVE",
  "PAUSED",
  "DISABLED",
  "EXPIRED",
];
export const WALLET_STATUSES = ["AVAILABLE", "USED", "EXPIRED", "REVOKED"];
export const RESERVATION_STATUSES = [
  "ACTIVE",
  "CONFIRMED",
  "RELEASED",
  "EXPIRED",
];
export const ACTION_TYPES = ["PERCENTAGE", "FIXED_AMOUNT", "FULL_DISCOUNT"];

export const PROMOTION_MODELS = {
  AUTO: {
    key: "system",
    label: "Ưu đãi tự động",
    shortLabel: "Tự động",
    description:
      "Hệ thống tự đánh giá tại checkout và áp dụng phương án giảm tốt nhất; khách hàng không cần chọn hay nhập mã.",
  },
  VOUCHER: {
    key: "event",
    label: "Voucher sự kiện",
    shortLabel: "Cần nhận",
    description:
      "Hiển thị công khai tại sự kiện. Khách hàng cần nhận vào ví trước khi dùng.",
  },
  COUPON: {
    key: "coupon",
    label: "Coupon theo khách hàng",
    shortLabel: "Cấp riêng",
    description:
      "Mã được tạo tự động, cấp riêng cho khách hàng và chỉ dùng khi tự nhập tại checkout.",
  },
};

export const promotionLabels = {
  COUPON: "Coupon theo khách hàng",
  VOUCHER: "Voucher sự kiện",
  AUTO: "Ưu đãi tự động",
  DRAFT: "Đang soạn",
  SCHEDULED: "Đã lên lịch",
  ACTIVE: "Đang áp dụng",
  PAUSED: "Tạm dừng",
  COMPLETED: "Hoàn tất",
  CANCELLED: "Đã hủy",
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
  PASSED: "Đạt pháp lý",
  FAILED: "Không đạt",
  DISABLED: "Vô hiệu hóa",
  AVAILABLE: "Có thể sử dụng",
  USED: "Đã dùng",
  LOCKED: "Đang khóa",
  EXPIRED: "Hết hạn",
  REVOKED: "Đã thu hồi",
  FIXED_AMOUNT: "Giảm tiền cố định",
  PERCENTAGE: "Giảm phần trăm",
  FULL_DISCOUNT: "Miễn phí toàn bộ",
  SUCCESS: "Thành công",
  CONFIRMED: "Đã xác nhận",
  ROLLED_BACK: "Đã hoàn tác",
  REFUNDED: "Đã hoàn tiền",
  RELEASED: "Đã giải phóng",
  SUBMIT: "Gửi duyệt",
  PUBLISH: "Công bố",
  ACTIVATE: "Kích hoạt",
  PAUSE: "Tạm dừng",
  KILL_SWITCH: "Dừng khẩn cấp",
  CANCEL: "Hủy",
};

export const labelFor = (value) =>
  promotionLabels[value] || value || "Chưa xác định";

export const promotionModelFor = (type) =>
  PROMOTION_MODELS[type] || {
    key: String(type || "unknown").toLowerCase(),
    label: labelFor(type),
    shortLabel: labelFor(type),
    description: "",
  };

export const promotionSourceLabel = (promotion) => {
  if (promotion?.promotionType === "AUTO")
    return "Ưu đãi hệ thống - tự động tại checkout";
  if (
    promotion?.source === "PUBLIC_EVENT" ||
    promotion?.ownershipType === "CLAIMABLE"
  ) {
    return "Voucher sự kiện - chưa thuộc ví";
  }
  if (promotion?.promotionType === "COUPON")
    return "Coupon cấp riêng, dùng bằng mã tại checkout";
  return "Voucher cá nhân trong ví";
};

export const currency = (value, code = "VND") =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: code || "VND",
  }).format(Number(value || 0));

export const formatDateTime = (value) => {
  if (!value) return "Chưa có";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    timeZoneName: "short",
  }).format(date);
};

export const toDateTimeLocal = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

export const fromDateTimeLocal = (value) => {
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

export const badgeClass = (status) => {
  const normalized = String(status || "").toUpperCase();
  if (
    [
      "ACTIVE",
      "AVAILABLE",
      "APPROVED",
      "PASSED",
      "SUCCESS",
      "CONFIRMED",
      "COMPLETED",
    ].includes(normalized)
  ) {
    return "border-emerald-500/30 bg-emerald-500/10 text-emerald-300";
  }
  if (["DRAFT", "PENDING", "SCHEDULED", "LOCKED"].includes(normalized)) {
    return "border-amber-500/30 bg-amber-500/10 text-amber-300";
  }
  if (["PAUSED", "RELEASED", "ROLLED_BACK", "REFUNDED"].includes(normalized)) {
    return "border-sky-500/30 bg-sky-500/10 text-sky-300";
  }
  if (
    ["CANCELLED", "REJECTED", "FAILED", "DISABLED", "REVOKED"].includes(
      normalized,
    )
  ) {
    return "border-red-500/30 bg-red-500/10 text-red-300";
  }
  return "border-zinc-700 bg-zinc-800 text-zinc-300";
};

export const safeJsonParse = (value, fallback = {}) => {
  if (value && typeof value === "object") return { value, error: "" };
  if (!value || !String(value).trim()) return { value: fallback, error: "" };
  try {
    return { value: JSON.parse(value), error: "" };
  } catch {
    return { value: fallback, error: "JSON chưa đúng định dạng." };
  }
};

export const jsonString = (value) => {
  if (value === undefined || value === null || value === "") return "{}";
  if (typeof value === "string") return value;
  return JSON.stringify(value, null, 2);
};

export const defaultConditions = (minimumOrderAmount = 0) => ({
  ...(Number(minimumOrderAmount) > 0
    ? { minimumOrderAmount: Number(minimumOrderAmount) }
    : {}),
});

export const actionFromPreset = ({
  actionType,
  actionValue,
  maxDiscountAmount,
}) => {
  const type = actionType || "PERCENTAGE";
  const action = { discountType: type };
  if (["PERCENTAGE", "FIXED_AMOUNT", "CASHBACK"].includes(type)) {
    action.discountValue = Number(actionValue || 0);
  }
  if (type === "PERCENTAGE" && Number(maxDiscountAmount) > 0) {
    action.maxDiscountAmount = Number(maxDiscountAmount);
  }
  return action;
};

export const friendlyPromotionError = (error) => {
  const data = error?.response?.data || error?.data || {};
  if (Array.isArray(data.details) && data.details.length > 0) {
    return data.details.map((item) => item.message).join(" ");
  }
  const message =
    data.message || error?.message || "Không thể hoàn tất thao tác khuyến mãi.";
  return message
    .replace("Invalid request parameters", "Thông tin nhập chưa hợp lệ")
    .replace("Access denied", "Bạn chưa có quyền thực hiện thao tác này")
    .replace("Promotion is not claimable", "Voucher này hiện không thể nhận")
    .replace(
      "Promotion is not active",
      "Ưu đãi chưa hoạt động hoặc đã hết hiệu lực",
    )
    .replace(
      "Coupon is invalid or unavailable",
      "Coupon không hợp lệ hoặc đã hết hiệu lực",
    )
    .replace("Coupon code was not found", "Không tìm thấy coupon này")
    .replace(
      "At least one customer is required",
      "Hãy chọn ít nhất một khách hàng",
    );
};

export const fieldErrors = (error) => {
  const details = error?.response?.data?.details || error?.data?.details || [];
  return details.reduce(
    (result, item) => ({
      ...result,
      [String(item.field || "")
        .split(".")
        .pop()]: item.message,
    }),
    {},
  );
};

export const voucherDiscountSummary = (voucher) => {
  const actions =
    typeof voucher?.actionsJson === "string"
      ? safeJsonParse(voucher.actionsJson).value
      : voucher?.actionsJson;
  const action = Array.isArray(actions) ? actions[0] : actions;
  const type =
    action?.discountType ||
    action?.type ||
    action?.actionType ||
    voucher?.voucherType;
  if (type === "PERCENTAGE" || type === "PERCENT") {
    const value = action?.discountValue ?? action?.value ?? action?.percentage;
    const cap =
      action?.maxDiscountAmount ??
      action?.maximumDiscountAmount ??
      action?.maxAmount;
    return `${value || 0}%${cap ? `, tối đa ${currency(cap)}` : ""}`;
  }
  if (["FIXED_AMOUNT", "AMOUNT", "CASHBACK"].includes(type)) {
    return currency(
      action?.discountValue ??
        action?.value ??
        action?.amount ??
        voucher?.faceValue,
    );
  }
  if (type === "FREE_TICKET") return "Miễn phí vé đủ điều kiện";
  if (type === "FREE_COMBO") return "Miễn phí combo đủ điều kiện";
  if (type === "FREE" || type === "FULL_DISCOUNT")
    return "Giảm toàn bộ đơn đủ điều kiện";
  return voucher?.faceValue ? currency(voucher.faceValue) : labelFor(type);
};

export const estimatedDiscountAmount = (promotion, bookingAmount = 0) => {
  const actions =
    typeof promotion?.actionsJson === "string"
      ? safeJsonParse(promotion.actionsJson).value
      : promotion?.actionsJson;
  const action = Array.isArray(actions) ? actions[0] : actions;
  const amount = Math.max(0, Number(bookingAmount) || 0);
  const type = action?.discountType || action?.type || action?.actionType;
  const value = Math.max(
    0,
    Number(
      action?.discountValue ??
        action?.value ??
        action?.amount ??
        action?.percentage ??
        0,
    ),
  );
  if (type === "PERCENTAGE" || type === "PERCENT") {
    const maximum = Number(
      action?.maxDiscountAmount ??
        action?.maximumDiscountAmount ??
        action?.maxAmount,
    );
    const discount = (amount * value) / 100;
    return Number.isFinite(maximum) && maximum > 0
      ? Math.min(discount, maximum)
      : discount;
  }
  if (["FIXED_AMOUNT", "AMOUNT", "CASHBACK"].includes(type))
    return Math.min(amount, value);
  if (["FREE", "FULL_DISCOUNT", "FREE_TICKET", "FREE_COMBO"].includes(type))
    return amount;
  return 0;
};

const UNUSABLE_WALLET_STATUSES = new Set([
  "USED",
  "REDEEMED",
  "EXPIRED",
  "REVOKED",
  "DISABLED",
  "CANCELLED",
]);

export const walletUsageRemaining = (promotion) => {
  if (promotion?.remainingUsage !== undefined && promotion?.remainingUsage !== null) {
    const remaining = Number(promotion.remainingUsage);
    return Number.isFinite(remaining) ? Math.max(0, remaining) : null;
  }
  if (promotion?.remainingUses !== undefined && promotion?.remainingUses !== null) {
    const remaining = Number(promotion.remainingUses);
    return Number.isFinite(remaining) ? Math.max(0, remaining) : null;
  }
  if (promotion?.maxUsage === undefined || promotion?.maxUsage === null) {
    return null;
  }
  const maxUsage = Number(promotion.maxUsage);
  const usageCount = Number(promotion.usageCount || 0);
  if (!Number.isFinite(maxUsage)) return null;
  return Math.max(0, maxUsage - (Number.isFinite(usageCount) ? usageCount : 0));
};

export const isWalletPromotion = (promotion) =>
  promotion?.source === "CUSTOMER_WALLET" ||
  Boolean(promotion?.walletPublicId || promotion?.selectionPublicId);

export const isWalletPromotionUsable = (promotion, now = Date.now()) => {
  if (!isWalletPromotion(promotion)) return true;
  if (promotion?.runtimeAvailable === false) return false;

  const status = String(promotion?.status || "").toUpperCase();
  if (UNUSABLE_WALLET_STATUSES.has(status)) return false;

  const remaining = walletUsageRemaining(promotion);
  if (remaining !== null && remaining <= 0) return false;

  if (promotion?.validFrom) {
    const validFrom = new Date(promotion.validFrom).getTime();
    if (Number.isFinite(validFrom) && validFrom > now) return false;
  }

  if (promotion?.validTo) {
    const validTo = new Date(promotion.validTo).getTime();
    if (Number.isFinite(validTo) && validTo <= now) return false;
  }

  return true;
};

export const walletPromotionUnavailableReason = (promotion) => {
  if (promotion?.unavailableReason) return promotion.unavailableReason;
  const status = String(promotion?.status || "").toUpperCase();
  if (["USED", "REDEEMED"].includes(status)) {
    return "Voucher đã được sử dụng hết lượt.";
  }
  if (status === "EXPIRED") return "Voucher đã hết hạn.";
  if (status === "REVOKED") return "Voucher đã được thu hồi.";
  if (["DISABLED", "CANCELLED"].includes(status)) {
    return "Voucher hiện không còn khả dụng.";
  }
  const remaining = walletUsageRemaining(promotion);
  if (remaining !== null && remaining <= 0) {
    return "Voucher đã hết số lượt sử dụng.";
  }
  if (promotion?.validFrom) {
    const validFrom = new Date(promotion.validFrom).getTime();
    if (Number.isFinite(validFrom) && validFrom > Date.now()) {
      return "Voucher chưa đến thời gian sử dụng.";
    }
  }
  if (promotion?.validTo) {
    const validTo = new Date(promotion.validTo).getTime();
    if (Number.isFinite(validTo) && validTo <= Date.now()) {
      return "Voucher đã hết hạn.";
    }
  }
  return "";
};

export const conditionSummary = (conditionsJson) => {
  const conditions =
    typeof conditionsJson === "string"
      ? safeJsonParse(conditionsJson).value
      : conditionsJson;
  if (!conditions || Object.keys(conditions).length === 0)
    return "Không có điều kiện bổ sung.";
  const parts = [];
  const minimum = conditions.minimumOrderAmount ?? conditions.minOrderAmount;
  if (minimum) parts.push(`Đơn tối thiểu ${currency(minimum)}`);
  if (
    Array.isArray(conditions.allowedUserIds) &&
    conditions.allowedUserIds.length
  ) {
    parts.push(`Chỉ ${conditions.allowedUserIds.length} khách hàng được chọn`);
  }
  if (
    (Array.isArray(conditions.movieIds) && conditions.movieIds.length) ||
    (Array.isArray(conditions.moviePublicIds) &&
      conditions.moviePublicIds.length)
  )
    parts.push("Giới hạn theo phim");
  if (
    (Array.isArray(conditions.cinemaIds) && conditions.cinemaIds.length) ||
    (Array.isArray(conditions.cinemaPublicIds) &&
      conditions.cinemaPublicIds.length)
  )
    parts.push("Giới hạn theo rạp");
  if (Array.isArray(conditions.dayOfWeek) && conditions.dayOfWeek.length) {
    parts.push(`Áp dụng ${conditions.dayOfWeek.join(", ")}`);
  }
  if (conditions.requiredTierCode)
    parts.push(`Yêu cầu hạng ${conditions.requiredTierCode}`);
  if (conditions.requiresVerification) parts.push("Cần xác thực khách hàng");
  return parts.length ? parts.join(" · ") : "Có điều kiện nâng cao trong JSON.";
};
