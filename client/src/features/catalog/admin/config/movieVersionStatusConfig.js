export const VERSION_STATUS = {
  ACTIVE: 'Đang hoạt động',
  INACTIVE: 'Không hoạt động'
};

export const getVersionStatusConfig = (status) => {
  const label = VERSION_STATUS[status] || status || 'Không xác định';
  const colorClass = status === 'ACTIVE'
    ? 'text-green-400 border-green-500/30 bg-green-500/10'
    : 'text-zinc-400 border-zinc-700 bg-zinc-800/50';

  return { label, colorClass };
};
