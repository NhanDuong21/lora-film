export const PRICING_REASON_PRESENTATIONS = {
  PRICING_INCOMPLETE: {
    label: 'Chưa đủ giá cho tất cả loại ghế',
    guidance: 'Bổ sung giá cho tất cả loại ghế có thể bán trước khi tạo suất chiếu.',
    tone: 'red',
  },
  PRICING_AMBIGUOUS: {
    label: 'Có nhiều mức giá phù hợp cùng lúc',
    guidance: 'Giới hạn lại theo phòng, ngày hoặc khung giờ để mỗi loại ghế chỉ còn một mức giá.',
    tone: 'red',
  },
  PRICE_POLICY_NOT_FOUND: {
    label: 'Chưa có bảng giá đang áp dụng',
    guidance: 'Tạo hoặc kích hoạt một bảng giá bao phủ ngày chiếu này.',
    tone: 'red',
  },
  PRICE_RULE_MISSING: {
    label: 'Thiếu giá cho một hoặc nhiều loại ghế',
    guidance: 'Bổ sung giá cho các loại ghế được liệt kê.',
    tone: 'red',
  },
  PRICE_POLICY_OVERLAP: {
    label: 'Có các mức giá bị trùng điều kiện',
    guidance: 'Điều chỉnh phòng, loại ngày hoặc khung giờ để mỗi trường hợp chỉ còn một mức giá.',
    tone: 'red',
  },
  INVALID_CINEMA_TIMEZONE: {
    label: 'Cấu hình giờ của rạp chưa đúng',
    guidance: 'Cập nhật lại cấu hình giờ của rạp trước khi kiểm tra giá.',
    tone: 'red',
  },
};

export const getPricingReasonPresentation = reasonCode => (
  PRICING_REASON_PRESENTATIONS[reasonCode] || {
    label: 'Không xác định được nguyên nhân',
    guidance: 'Thử kiểm tra lại; nếu lỗi vẫn còn, hãy liên hệ quản trị hệ thống.',
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
    facts: `${getDayTypeLabel(conflict?.dayType)} · ${getTimeBandLabel(conflict?.timeBandStart, conflict?.timeBandEnd)} · ${conflict?.conflictingRuleCount || 2} mức giá bị trùng`,
    guidance: reason.guidance,
    technical: {
      reasonCode: conflict?.reasonCode || 'PRICE_POLICY_OVERLAP',
      ruleIds: [conflict?.firstRuleId, conflict?.secondRuleId].filter(Boolean),
      message: conflict?.message,
    },
  };
};
