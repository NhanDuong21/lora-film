import { getCinemaDateKey } from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';

const INVALID_DATE = Number.NaN;

const toTimestamp = value => {
  const timestamp = new Date(value).getTime();
  return Number.isFinite(timestamp) ? timestamp : INVALID_DATE;
};

export const addDaysToDateKey = (dateKey, days) => {
  const date = new Date(`${dateKey}T12:00:00Z`);
  if (!Number.isFinite(date.getTime())) return null;
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
};

export const getShowtimeDateKeys = (now, timezone, days = 7) => {
  const firstDate = getCinemaDateKey(now, timezone);
  if (!firstDate) return [];
  return Array.from({ length: days }, (_, index) => addDaysToDateKey(firstDate, index))
    .filter(Boolean);
};

export const getNextShowtimeForAuditorium = (showtimes, auditoriumPublicId, now = new Date()) => {
  const nowTimestamp = toTimestamp(now);
  if (!Number.isFinite(nowTimestamp)) return null;

  return (showtimes || [])
    .filter(showtime => {
      const auditoriumId = showtime?.auditorium?.publicId;
      const startTimestamp = toTimestamp(showtime?.startTime);
      const endTimestamp = toTimestamp(showtime?.endTime);
      const isRelevantStatus = !['CANCELLED', 'FINISHED'].includes(showtime?.status);
      const isStillRunning = Number.isFinite(endTimestamp) && endTimestamp > nowTimestamp;
      return auditoriumId === auditoriumPublicId
        && isRelevantStatus
        && Number.isFinite(startTimestamp)
        && (startTimestamp >= nowTimestamp || isStillRunning);
    })
    .sort((left, right) => toTimestamp(left.startTime) - toTimestamp(right.startTime))[0] || null;
};

export const getShowtimeState = (showtime, now = new Date()) => {
  const nowTimestamp = toTimestamp(now);
  const startTimestamp = toTimestamp(showtime?.startTime);
  const endTimestamp = toTimestamp(showtime?.endTime);

  if (Number.isFinite(startTimestamp)
    && startTimestamp <= nowTimestamp
    && (!Number.isFinite(endTimestamp) || endTimestamp > nowTimestamp)) {
    return 'SHOWING';
  }
  return 'UPCOMING';
};
