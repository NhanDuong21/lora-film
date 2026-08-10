export const seatSelectionPath = showtimePublicId =>
  `/seat-selection?showtimeId=${encodeURIComponent(showtimePublicId)}`;

export const formatServiceDate = value => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) return value || '';
  const [year, month, day] = value.split('-').map(Number);
  return new Intl.DateTimeFormat('vi-VN', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    timeZone: 'UTC'
  }).format(new Date(Date.UTC(year, month - 1, day)));
};

export const formatLocalClock = localDateTime =>
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(localDateTime || '')
    ? localDateTime.slice(11, 16)
    : '--:--';

export const isFutureBookableShowtime = (showtime, now = Date.now()) => {
  if (showtime?.status !== 'OPEN_FOR_BOOKING') return false;
  const startTime = Date.parse(showtime?.startTime);
  const currentTime = now instanceof Date ? now.getTime() : Number(now);
  return Number.isFinite(startTime)
    && Number.isFinite(currentTime)
    && startTime > currentTime;
};

export const vietnamDateKey = (date = new Date()) => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    timeZone: 'Asia/Ho_Chi_Minh'
  }).formatToParts(date);
  const value = type => parts.find(part => part.type === type)?.value;
  return `${value('year')}-${value('month')}-${value('day')}`;
};

export const addCalendarDays = (dateKey, days) => {
  const [year, month, day] = dateKey.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day + days));
  return date.toISOString().slice(0, 10);
};
