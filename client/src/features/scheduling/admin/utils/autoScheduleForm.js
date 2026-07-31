const DATE_KEY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;
const DAY_MS = 86_400_000;

const parseDateKey = (value) => {
  const match = DATE_KEY_PATTERN.exec(value || '');
  if (!match) return null;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const epoch = Date.UTC(year, month - 1, day);
  const date = new Date(epoch);
  if (
    date.getUTCFullYear() !== year
    || date.getUTCMonth() !== month - 1
    || date.getUTCDate() !== day
  ) return null;
  return { year, month, day, epoch };
};

export const addCalendarDays = (dateKey, days) => {
  const parsed = parseDateKey(dateKey);
  if (!parsed || !Number.isInteger(days)) return '';
  return new Date(parsed.epoch + (days * DAY_MS)).toISOString().slice(0, 10);
};

export const getInclusiveDayCount = (scheduleFrom, scheduleTo) => {
  const from = parseDateKey(scheduleFrom);
  const to = parseDateKey(scheduleTo);
  if (!from || !to) return null;
  return Math.floor((to.epoch - from.epoch) / DAY_MS) + 1;
};

export const getDateKeyInTimezone = (timezone, now = new Date()) => {
  if (!timezone || !Number.isFinite(now.getTime())) return '';
  try {
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      calendar: 'gregory',
      numberingSystem: 'latn',
    }).formatToParts(now);
    const values = Object.fromEntries(parts.map(part => [part.type, part.value]));
    return `${values.year}-${values.month}-${values.day}`;
  } catch {
    return '';
  }
};

export const validateAutoScheduleDateRange = ({
  scheduleFrom,
  scheduleTo,
  cinemaTimezone,
  now = new Date(),
}) => {
  const errors = {};
  const dayCount = getInclusiveDayCount(scheduleFrom, scheduleTo);
  const cinemaToday = getDateKeyInTimezone(cinemaTimezone, now);
  const suggestedScheduleTo = scheduleFrom ? addCalendarDays(scheduleFrom, 6) : '';

  if (!scheduleFrom) errors.scheduleFrom = 'Vui lòng chọn ngày bắt đầu';
  if (!scheduleTo) errors.scheduleTo = 'Vui lòng chọn ngày kết thúc';
  if (scheduleFrom && !parseDateKey(scheduleFrom)) errors.scheduleFrom = 'Ngày bắt đầu không hợp lệ';
  if (scheduleTo && !parseDateKey(scheduleTo)) errors.scheduleTo = 'Ngày kết thúc không hợp lệ';

  if (!errors.scheduleFrom && cinemaToday && scheduleFrom < cinemaToday) {
    errors.scheduleFrom = `Ngày bắt đầu không được trước ngày hiện tại của cụm rạp (${cinemaToday})`;
  }
  if (!errors.scheduleFrom && !errors.scheduleTo && dayCount !== null && dayCount < 1) {
    errors.scheduleTo = 'Ngày kết thúc phải từ ngày bắt đầu trở đi';
  }
  if (!errors.scheduleFrom && !errors.scheduleTo && dayCount !== null && dayCount > 7) {
    errors.scheduleTo = `Mỗi bản lịch chỉ gồm tối đa 7 ngày. Bạn có thể chọn từ ${scheduleFrom} đến ${suggestedScheduleTo}.`;
  }

  return {
    errors,
    dayCount,
    cinemaToday,
    isTooLong: dayCount !== null && dayCount > 7,
    suggestedScheduleFrom: scheduleFrom || '',
    suggestedScheduleTo,
  };
};

export const buildAutoScheduleRequestFingerprint = (request) => JSON.stringify({
  cinemaPublicId: request.cinemaPublicId,
  scheduleFrom: request.scheduleFrom,
  scheduleTo: request.scheduleTo,
  movieVersionPublicIds: [...(request.movieVersionPublicIds || [])].sort(),
  auditoriumPublicIds: [...(request.auditoriumPublicIds || [])].sort(),
  slotGranularityMinutes: Number(request.slotGranularityMinutes),
  previewTtlMinutes: Number(request.previewTtlMinutes),
});
