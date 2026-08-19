const currency = (value) =>
  `${Number(value || 0).toLocaleString("vi-VN")}đ`;

const walletPromotionId = (promotion) =>
  promotion?.walletPublicId ||
  promotion?.selectionPublicId ||
  (promotion?.source === "CUSTOMER_WALLET" ? promotion?.publicId : null) ||
  "";

const quoteAppliesPromotion = (quote, promotion) => {
  const requestedIds = new Set(
    [walletPromotionId(promotion)].filter(Boolean).map(String),
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
  if (!manualApplied && quote?.manualSelectionReplaced) {
    const appliedNames = applied.map((item) => item.name).filter(Boolean).join(" + ");
    const extra = Number(quote?.additionalSavings || 0);
    return {
      applied: false,
      notice: {
        variant: "replaced",
        message: `Hệ thống đã giữ ưu đãi tốt hơn${appliedNames ? ` (${appliedNames})` : ""}. Bạn tiết kiệm thêm ${currency(extra)} và voucher đã chọn vẫn còn trong ví.`,
      },
    };
  }
  if (!manualApplied) return { applied: false, notice: null };
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
