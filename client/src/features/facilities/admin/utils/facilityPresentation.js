export const CINEMA_STATUS = {
  DRAFT: {
    label: 'Bản nháp',
    description: 'Chưa hiển thị cho khách hàng và chưa tham gia vận hành.',
    className: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
  },
  ACTIVE: {
    label: 'Đang vận hành',
    description: 'Được phép tham gia vận hành; khả dụng thực tế còn phụ thuộc giờ hoạt động, closure và maintenance.',
    className: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
  },
  MAINTENANCE: {
    label: 'Đang bảo trì',
    description: 'Trạng thái cũ đang được giữ để tương thích; nên dùng khoảng thời gian khả dụng thay cho lifecycle.',
    className: 'bg-orange-500/10 text-orange-400 border-orange-500/30',
  },
  TEMPORARILY_CLOSED: {
    label: 'Tạm đóng cửa',
    description: 'Trạng thái cũ đang được giữ để tương thích; lịch đóng cửa mới là dữ liệu thời gian authoritative.',
    className: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
  },
  INACTIVE: {
    label: 'Tạm ngừng',
    description: 'Không tham gia vận hành hiện tại.',
    className: 'bg-zinc-800 text-zinc-400 border-zinc-700',
  },
  PERMANENTLY_CLOSED: {
    label: 'Ngừng khai thác',
    description: 'Trạng thái cuối: không còn sử dụng lâu dài, dữ liệu lịch sử vẫn được giữ lại.',
    className: 'bg-red-500/10 text-red-400 border-red-500/30',
  },
};

export const AUDITORIUM_STATUS = {
  DRAFT: {
    label: 'Đang thiết lập',
    description: 'Có thể chỉnh sửa cấu hình và sơ đồ ghế.',
    className: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
  },
  ACTIVE: {
    label: 'Sẵn sàng phục vụ',
    description: 'Phòng được phép nhận suất chiếu; việc mở bán thuộc về từng suất chiếu cụ thể.',
    className: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
  },
  MAINTENANCE: {
    label: 'Đang bảo trì',
    description: 'Phòng tạm ngừng phục vụ trong thời gian bảo trì.',
    className: 'bg-orange-500/10 text-orange-400 border-orange-500/30',
  },
  INACTIVE: {
    label: 'Tạm ngừng',
    description: 'Phòng không tham gia xếp lịch hoặc bán vé.',
    className: 'bg-zinc-800 text-zinc-400 border-zinc-700',
  },
};

export const MEDIA_TYPE_LABELS = {
  BANNER: 'Ảnh bìa',
  LOGO: 'Biểu trưng',
  GALLERY: 'Thư viện ảnh',
  MAP: 'Sơ đồ rạp',
};

export const SCREEN_TYPE_LABELS = {
  STANDARD: 'Tiêu chuẩn',
  IMAX: 'IMAX',
  '4DX': '4DX',
  SCREENX: 'ScreenX',
};

export const SOUND_TYPE_LABELS = {
  STANDARD: 'Tiêu chuẩn',
  DOLBY_ATMOS: 'Dolby Atmos',
};

export function getCinemaStatus(status) {
  return CINEMA_STATUS[status] || {
    label: 'Chưa xác định',
    description: 'Trạng thái chưa được hỗ trợ trên giao diện.',
    className: 'bg-zinc-800 text-zinc-400 border-zinc-700',
  };
}

export function getAuditoriumStatus(status) {
  return AUDITORIUM_STATUS[status] || {
    label: 'Chưa xác định',
    description: 'Trạng thái chưa được hỗ trợ trên giao diện.',
    className: 'bg-zinc-800 text-zinc-400 border-zinc-700',
  };
}

export function isOvernight(openTime, closeTime) {
  if (!openTime || !closeTime) return false;
  return closeTime <= openTime;
}

export function getHoursDescription(hours) {
  if (!hours || hours.isClosed) return 'Nghỉ cả ngày';
  const suffix = isOvernight(hours.openTime, hours.closeTime) ? ' hôm sau' : '';
  return `${hours.openTime || '--:--'} – ${hours.closeTime || '--:--'}${suffix}`;
}

export function getCinemaReadiness(cinema) {
  const hours = cinema?.operatingHours || [];
  const media = cinema?.gallery || [];
  const rooms = cinema?.activeAuditoriums || [];
  const readyRooms = rooms.filter(
    (room) => room.status === 'ACTIVE' && Number(room.capacity || 0) > 0,
  );
  const checks = [
    {
      id: 'basic',
      label: 'Thông tin và địa chỉ',
      complete: Boolean(cinema?.name && cinema?.address && cinema?.city),
    },
    {
      id: 'hours',
      label: 'Giờ hoạt động đủ 7 ngày',
      complete: hours.length >= 7,
    },
    {
      id: 'media',
      label: 'Có ảnh nhận diện',
      complete: media.some((item) => item.isPrimary) || media.length > 0,
    },
    {
      id: 'rooms',
      label: 'Có phòng sẵn sàng phục vụ',
      complete: readyRooms.length > 0,
    },
  ];

  return {
    checks,
    completed: checks.filter((item) => item.complete).length,
    total: checks.length,
    readyRooms: readyRooms.length,
    totalRooms: rooms.length,
    ready: checks.every((item) => item.complete),
  };
}

export function getAuditoriumReadiness(room) {
  const capacity = Number(room?.capacity || 0);
  const status = room?.status || room?.auditoriumStatus;
  return {
    hasSeatLayout: capacity > 0,
    seatLayoutLabel: capacity > 0 ? `Đã thiết lập ${capacity} ghế` : 'Chưa có sơ đồ ghế',
    canServe: status === 'ACTIVE' && capacity > 0,
  };
}
