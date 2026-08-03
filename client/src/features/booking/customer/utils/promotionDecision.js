const currency = (value) =>
  `${Number(value || 0).toLocaleString("vi-VN")}đ`;

const isSystemPromotion = (promotion) =>
  promotion?.promotionType === "AUTO" || promotion?.source === "SYSTEM_AUTO";

const walletPromotionId = (promotion) =>
  promotion?.walletPublicId ||
  promotion?.selectionPublicId ||
  (promotion?.source === "CUSTOMER_WALLET" ? promotion?.publicId : null) ||
  "";

const systemPromotionId = (promotion) =>
  isSystemPromotion(promotion)
    ? promotion?.promotionPublicId || promotion?.publicId || ""
    : "";

const quoteAppliesPromotion = (quote, promotion) => {
  const requestedIds = new Set(
    [walletPromotionId(promotion), systemPromotionId(promotion)]
      .filter(Boolean)
      .map(String),
  );
  return (quote?.appliedPromotions || []).some((item) =>
    [item?.userPromotionPublicId, item?.promotionPublicId]
      .filter(Boolean)
      .some((id) => requestedIds.has(String(id))),
  );
};

const quoteAppliesCoupon = (quote, couponCode) =>
  (quote?.appliedPromotions || []).some(
    (item) =>
      item?.promotionType === "COUPON" &&
      String(item?.code || "").toUpperCase() ===
        String(couponCode || "").toUpperCase(),
  );

export const promotionDecision = (
  quote,
  { promotion = null, couponCode = "" } = {},
) => {
  const applied = quote?.appliedPromotions || [];
  if (!promotion && !couponCode) return { applied: true, notice: null };

  const manualApplied = promotion
    ? quoteAppliesPromotion(quote, promotion)
    : quoteAppliesCoupon(quote, couponCode);
  if (!manualApplied && applied.length > 0) {
    const retainedNames = applied
      .map((item) => item?.name)
      .filter(Boolean)
      .join(", ");
    const consumable = couponCode || !isSystemPromotion(promotion);
    return {
      applied: false,
      notice: {
        variant: "protected",
        message: `Hệ thống giữ ${retainedNames || "ưu đãi tự động tốt hơn"} vì giảm ${currency(quote?.discountAmount)} — có lợi hơn lựa chọn vừa chọn.${consumable ? " Voucher/coupon chưa bị sử dụng." : ""}`,
      },
    };
  }
  if (manualApplied && applied.length > 1) {
    return {
      applied: true,
      notice: {
        variant: "stacked",
        message: `Đã cộng dồn ${applied.map((item) => item.name).filter(Boolean).join(" + ")}. Tổng giảm ${currency(quote?.discountAmount)}.`,
      },
    };
  }
  return { applied: manualApplied, notice: null };
};
