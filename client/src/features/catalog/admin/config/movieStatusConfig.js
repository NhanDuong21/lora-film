export const ADMIN_MOVIE_STATUS_TABS = [
  { value: 'DRAFT', label: 'Chờ duyệt' },
  { value: 'UPCOMING', label: 'Sắp chiếu' },
  { value: 'NOW_SHOWING', label: 'Đang chiếu' },
  { value: 'ENDED', label: 'Đã kết thúc' },
  { value: 'INACTIVE', label: 'Không hoạt động' },
  { value: 'ALL', label: 'Tất cả' }
];

export const STATUS_MAPPING = {
  DRAFT: { label: 'Chờ duyệt', variant: 'warning', colorClass: 'text-yellow-500 border-yellow-500/30 bg-yellow-500/10' },
  UPCOMING: { label: 'Sắp chiếu', variant: 'info', colorClass: 'text-blue-400 border-blue-400/30 bg-blue-400/10' },
  NOW_SHOWING: { label: 'Đang chiếu', variant: 'success', colorClass: 'text-emerald-500 border-emerald-500/30 bg-emerald-500/10' },
  ENDED: { label: 'Đã kết thúc', variant: 'neutral', colorClass: 'text-zinc-400 border-zinc-700 bg-zinc-900' },
  INACTIVE: { label: 'Không hoạt động', variant: 'error', colorClass: 'text-red-400 border-red-400/30 bg-red-400/10' }
};

export const getStatusConfig = (status) => {
  return STATUS_MAPPING[status] || { label: status || 'Không xác định', variant: 'neutral', colorClass: 'text-zinc-400 border-zinc-700 bg-zinc-900' };
};
