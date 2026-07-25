export const PRICING_REASON_PRESENTATIONS = {
  PRICING_INCOMPLETE: {
    label: 'Thiếu chính sách hoặc quy tắc giá hiệu lực',
    guidance: 'Bổ sung quy tắc giá cho tất cả loại ghế có thể bán trước khi áp dụng lịch.',
    tone: 'red',
  },
  PRICING_AMBIGUOUS: {
    label: 'Có nhiều quy tắc giá cùng mức ưu tiên',
    guidance: 'Điều chỉnh phạm vi, ngày, khung giờ hoặc độ ưu tiên để chỉ còn một quy tắc thắng.',
    tone: 'red',
  },
  PRICE_POLICY_NOT_FOUND: {
    label: 'Không có chính sách giá hiệu lực',
    guidance: 'Tạo hoặc kích hoạt chính sách giá bao phủ ngày chiếu này.',
    tone: 'red',
  },
  PRICE_RULE_MISSING: {
    label: 'Thiếu quy tắc giá cho một hoặc nhiều loại ghế',
    guidance: 'Bổ sung quy tắc cho các loại ghế được liệt kê.',
    tone: 'red',
  },
  PRICE_POLICY_OVERLAP: {
    label: 'Các quy tắc giá xung đột cùng hạng',
    guidance: 'Điều chỉnh phạm vi, loại ngày, khung giờ hoặc độ ưu tiên trước khi kích hoạt.',
    tone: 'red',
  },
  INVALID_CINEMA_TIMEZONE: {
    label: 'Múi giờ của rạp không hợp lệ',
    guidance: 'Cập nhật múi giờ IANA hợp lệ cho rạp trước khi phân giải giá.',
    tone: 'red',
  },
};

export const getPricingReasonPresentation = reasonCode => (
  PRICING_REASON_PRESENTATIONS[reasonCode] || {
    label: 'Không xác định — xem chi tiết kỹ thuật',
    guidance: 'Kiểm tra mã chẩn đoán kỹ thuật hoặc liên hệ quản trị hệ thống.',
    tone: 'zinc',
  }
);

export const getRuleScopePresentation = rule => {
  if (rule?.scope === 'AUDITORIUM' || rule?.auditoriumId) {
    return {
      code: 'AUDITORIUM',
      label: 'Phòng chiếu cụ thể',
      detail: rule?.auditoriumName || rule?.auditoriumId || 'Chưa chọn phòng',
    };
  }
  if (rule?.scope === 'SCREEN_TYPE' || rule?.screenType) {
    return {
      code: 'SCREEN_TYPE',
      label: 'Theo loại màn hình',
      detail: rule?.screenType || 'Chưa chọn loại màn hình',
    };
  }
  return { code: 'CINEMA', label: 'Toàn rạp', detail: null };
};

export const getDayTypeLabel = dayType => ({
  ALL_DAYS: 'Mọi ngày',
  WEEKDAY: 'Ngày thường',
  WEEKEND: 'Cuối tuần',
}[dayType] || dayType || 'Mọi ngày');

export const getTimeBandLabel = (start, end) => (
  start && end ? `${start}–${end}` : 'Cả ngày'
);

export const getConflictPresentation = conflict => {
  const reason = getPricingReasonPresentation(conflict?.reasonCode || 'PRICE_POLICY_OVERLAP');
  const scope = getRuleScopePresentation(conflict);
  const seatType = conflict?.seatTypeName
    ? `${conflict.seatTypeName}${conflict.seatTypeCode ? ` (${conflict.seatTypeCode})` : ''}`
    : 'Loại ghế chưa xác định';
  return {
    title: `${seatType} · ${scope.label}${scope.detail ? `: ${scope.detail}` : ''}`,
    facts: `${getDayTypeLabel(conflict?.dayType)} · ${getTimeBandLabel(conflict?.timeBandStart, conflict?.timeBandEnd)} · ${conflict?.conflictingRuleCount || 2} quy tắc xung đột`,
    guidance: reason.guidance,
    technical: {
      reasonCode: conflict?.reasonCode || 'PRICE_POLICY_OVERLAP',
      ruleIds: [conflict?.firstRuleId, conflict?.secondRuleId].filter(Boolean),
      message: conflict?.message,
    },
  };
};
