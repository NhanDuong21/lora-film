const REASON_PRESENTATIONS = Object.freeze({
  SHOWTIME_OVERLAP_CONFLICT: {
    category: 'EXISTING_SCHEDULE',
    label: 'Lịch hiện có đang chiếm khung giờ',
    explanation: 'Các phương án của phim bị trùng với suất chiếu đã có trong phòng. Lịch tự động chỉ bổ sung vào chỗ trống, không di chuyển hoặc hủy lịch cũ.',
    actionLabel: 'Kiểm tra lịch hiện có',
  },
  SHOWTIME_OVERLAPS_EXISTING: {
    category: 'EXISTING_SCHEDULE',
    label: 'Lịch hiện có đang chiếm khung giờ',
    explanation: 'Các phương án của phim bị trùng với suất chiếu đã có trong phòng. Lịch tự động chỉ bổ sung vào chỗ trống, không di chuyển hoặc hủy lịch cũ.',
    actionLabel: 'Kiểm tra lịch hiện có',
  },
  SHOWTIME_OUTSIDE_RELEASE_WINDOW: {
    category: 'RELEASE_WINDOW',
    label: 'Ngoài thời gian phát hành',
    explanation: 'Phim chưa phát hành hoặc đã hết thời gian được phép xếp lịch trong ngày này.',
    actionLabel: 'Kiểm tra ngày phát hành',
  },
  MOVIE_NOT_ELIGIBLE: {
    category: 'MOVIE_CONFIGURATION',
    label: 'Phim chưa đủ điều kiện',
    explanation: 'Trạng thái, thời lượng hoặc phiên bản phim không còn đáp ứng điều kiện tạo suất chiếu.',
    actionLabel: 'Kiểm tra cấu hình phim',
  },
  MOVIE_STATUS_NOT_ELIGIBLE: {
    category: 'MOVIE_CONFIGURATION',
    label: 'Trạng thái phim không hợp lệ',
    explanation: 'Chỉ phim sắp chiếu hoặc đang chiếu mới được tạo suất chiếu.',
    actionLabel: 'Kiểm tra cấu hình phim',
  },
  MOVIE_VERSION_NOT_ACTIVE: {
    category: 'MOVIE_CONFIGURATION',
    label: 'Định dạng phim không hoạt động',
    explanation: 'Định dạng đã chọn không còn ở trạng thái hoạt động.',
    actionLabel: 'Kiểm tra định dạng phim',
  },
  SHOWTIME_OUTSIDE_OPERATING_HOURS: {
    category: 'OPERATING_HOURS',
    label: 'Ngoài giờ hoạt động của rạp',
    explanation: 'Không có phương án nào nằm trọn trong giờ hoạt động đã cấu hình.',
    actionLabel: 'Kiểm tra giờ hoạt động',
  },
  CINEMA_OPERATING_HOURS_NOT_CONFIGURED: {
    category: 'OPERATING_HOURS',
    label: 'Chưa cấu hình giờ hoạt động',
    explanation: 'Rạp chưa có giờ hoạt động cho ngày này nên hệ thống không thể xếp lịch.',
    actionLabel: 'Cấu hình giờ hoạt động',
  },
  SHOWTIME_OVERLAPS_CINEMA_CLOSURE: {
    category: 'CINEMA_CLOSURE',
    label: 'Rạp đang đóng cửa',
    explanation: 'Các phương án bị trùng với khoảng đóng cửa đã cấu hình của rạp.',
    actionLabel: 'Kiểm tra lịch đóng cửa',
  },
  SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE: {
    category: 'AUDITORIUM_MAINTENANCE',
    label: 'Phòng chiếu đang bảo trì',
    explanation: 'Các phương án bị trùng với lịch bảo trì của phòng chiếu.',
    actionLabel: 'Kiểm tra lịch bảo trì',
  },
  AUDITORIUM_UNAVAILABLE: {
    category: 'AUDITORIUM_CONFIGURATION',
    label: 'Phòng chiếu không khả dụng',
    explanation: 'Phòng chiếu không còn đáp ứng điều kiện vận hành.',
    actionLabel: 'Kiểm tra phòng chiếu',
  },
});

const DEFAULT_REASON_PRESENTATION = Object.freeze({
  category: 'OTHER',
  label: 'Không còn phương án hợp lệ',
  explanation: 'Mở danh sách phương án bị loại để xem điều kiện cụ thể mà hệ thống đã kiểm tra.',
  actionLabel: 'Xem phương án bị loại',
});

export const getOperationalReasonPresentation = code => ({
  code: code || 'UNKNOWN',
  ...(REASON_PRESENTATIONS[code] || DEFAULT_REASON_PRESENTATION),
});

const rejectionCodeOf = candidate => candidate?.technicalDetails?.rejectionCode
  || candidate?.rejectionCode
  || candidate?.technicalDetails?.applyErrorCode
  || candidate?.applyErrorCode
  || null;

const addReason = (counts, candidate) => {
  if (candidate.validationStatus === 'VALID') return;
  const code = rejectionCodeOf(candidate) || 'UNKNOWN';
  counts.set(code, (counts.get(code) || 0) + 1);
};

export const summarizeRejectionReasons = candidates => {
  const counts = new Map();
  (candidates || []).forEach(candidate => addReason(counts, candidate));
  return Array.from(counts.entries())
    .map(([code, count]) => ({ ...getOperationalReasonPresentation(code), count }))
    .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label, 'vi'));
};

export const getMovieOperationalState = movie => {
  if (movie.scheduledCount > 0) {
    return {
      code: 'SCHEDULED',
      tone: 'success',
      label: 'Đã có suất đề xuất',
      explanation: `${movie.scheduledCount} suất được chọn trong phạm vi đang xem.`,
    };
  }
  if (movie.validCount > 0) {
    return {
      code: 'VALID_NOT_SELECTED',
      tone: 'warning',
      label: 'Hợp lệ nhưng chưa được chọn',
      explanation: `Có ${movie.validCount} phương án hợp lệ, nhưng tổ hợp khác đạt cân bằng điểm và mức sử dụng phòng tốt hơn.`,
      actionLabel: 'Xem phương án hợp lệ',
    };
  }

  const primaryReason = movie.rejectionReasons?.[0] || getOperationalReasonPresentation();
  return {
    code: primaryReason.category,
    tone: 'blocked',
    label: primaryReason.label,
    explanation: primaryReason.explanation,
    actionLabel: primaryReason.actionLabel,
    reasonCode: primaryReason.code,
  };
};

export const getDailyOperationalSummaries = candidates => {
  const groups = new Map();
  (candidates || []).forEach(candidate => {
    const date = candidate.serviceDate;
    if (!date) return;
    if (!groups.has(date)) groups.set(date, []);
    groups.get(date).push(candidate);
  });

  return Array.from(groups.entries())
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([serviceDate, rows]) => {
      const scheduledCount = rows.filter(row => row.applyStatus === 'CREATED' || row.selected).length;
      const validCount = rows.filter(row => row.validationStatus === 'VALID').length;
      const rejectedCount = rows.length - validCount;
      const rejectionReasons = summarizeRejectionReasons(rows);
      let state = 'HAS_RECOMMENDATIONS';
      let label = `${scheduledCount} suất đề xuất`;
      if (scheduledCount === 0 && validCount > 0) {
        state = 'VALID_NOT_SELECTED';
        label = 'Có phương án nhưng chưa được chọn';
      } else if (validCount === 0) {
        state = 'NO_VALID_OPTIONS';
        label = rejectionReasons[0]?.label || 'Không còn khung giờ hợp lệ';
      }
      return {
        serviceDate,
        generatedCount: rows.length,
        validCount,
        rejectedCount,
        scheduledCount,
        rejectionReasons,
        state,
        label,
      };
    });
};
