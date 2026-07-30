export const ADMIN_MOVIE_STATUS_TABS = [
  { value: 'DRAFT', label: 'Cần hoàn thiện' },
  { value: 'UPCOMING', label: 'Sắp chiếu' },
  { value: 'NOW_SHOWING', label: 'Đang chiếu' },
  { value: 'ENDED', label: 'Đã kết thúc' },
  { value: 'INACTIVE', label: 'Tạm ngừng' },
  { value: 'ALL', label: 'Tất cả' }
];

export const STATUS_MAPPING = {
  DRAFT: {
    label: 'Cần hoàn thiện',
    description: 'Phim chưa hiển thị cho khách hàng.',
    variant: 'warning',
    colorClass: 'text-yellow-500 border-yellow-500/30 bg-yellow-500/10'
  },
  UPCOMING: {
    label: 'Sắp chiếu',
    description: 'Đã sẵn sàng cho lịch chiếu sắp tới.',
    variant: 'info',
    colorClass: 'text-blue-400 border-blue-400/30 bg-blue-400/10'
  },
  NOW_SHOWING: {
    label: 'Đang chiếu',
    description: 'Khách hàng có thể xem và đặt vé.',
    variant: 'success',
    colorClass: 'text-emerald-500 border-emerald-500/30 bg-emerald-500/10'
  },
  ENDED: {
    label: 'Đã kết thúc',
    description: 'Đã kết thúc giai đoạn khai thác.',
    variant: 'neutral',
    colorClass: 'text-zinc-400 border-zinc-700 bg-zinc-900'
  },
  INACTIVE: {
    label: 'Tạm ngừng khai thác',
    description: 'Đang ẩn khỏi khu vực khách hàng.',
    variant: 'error',
    colorClass: 'text-red-400 border-red-400/30 bg-red-400/10'
  }
};

export const getStatusConfig = (status) => {
  return STATUS_MAPPING[status] || {
    label: 'Chưa xác định',
    description: 'Hệ thống chưa nhận diện được tình trạng phục vụ.',
    variant: 'neutral',
    colorClass: 'text-zinc-400 border-zinc-700 bg-zinc-900'
  };
};
