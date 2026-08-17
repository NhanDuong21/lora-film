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

const OPERATIONAL_PRESENTATION = {
  SETUP: {
    key: 'SETUP', label: 'Đang thiết lập', tone: 'zinc',
    className: 'border-zinc-600/40 bg-zinc-700/20 text-zinc-300', priority: 1,
  },
  SUSPENDED: {
    key: 'SUSPENDED', label: 'Tạm ngừng', tone: 'red',
    className: 'border-red-500/35 bg-red-500/10 text-red-300', priority: 1,
  },
  MAINTENANCE: {
    key: 'MAINTENANCE', label: 'Đang bảo trì', tone: 'amber',
    className: 'border-amber-500/35 bg-amber-500/10 text-amber-300', priority: 0,
  },
  IN_SHOW: {
    key: 'IN_SHOW', label: 'Đang chiếu', tone: 'orange',
    className: 'border-brand-orange/35 bg-brand-orange/10 text-brand-orange', priority: 2,
  },
  CLEANING: {
    key: 'CLEANING', label: 'Đang dọn phòng', tone: 'violet',
    className: 'border-violet-500/35 bg-violet-500/10 text-violet-300', priority: 2,
  },
  UPCOMING: {
    key: 'UPCOMING', label: 'Sắp chiếu', tone: 'sky',
    className: 'border-sky-500/35 bg-sky-500/10 text-sky-300', priority: 3,
  },
  ATTENTION: {
    key: 'ATTENTION', label: 'Cần chú ý', tone: 'amber',
    className: 'border-amber-500/35 bg-amber-500/10 text-amber-300', priority: 0,
  },
  IDLE: {
    key: 'IDLE', label: 'Đang trống', tone: 'emerald',
    className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300', priority: 4,
  },
};

const operationalShowtimes = (showtimes, auditoriumPublicId) => (showtimes || [])
  .filter(item => item?.auditorium?.publicId === auditoriumPublicId && item.status !== 'CANCELLED')
  .filter(item => Number.isFinite(toTimestamp(item.startTime)))
  .sort((left, right) => toTimestamp(left.startTime) - toTimestamp(right.startTime));

export const getAuditoriumOperationalState = ({
  room,
  showtimes = [],
  maintenanceWindows = [],
  lockedSeatCount = 0,
  now = new Date(),
}) => {
  const nowTimestamp = toTimestamp(now);
  const roomId = room?.publicId || room?.auditoriumPublicId;
  const roomShowtimes = operationalShowtimes(showtimes, roomId);
  const activeMaintenance = maintenanceWindows.find(item => (
    item.status === 'ACTIVE'
    && toTimestamp(item.startTime) <= nowTimestamp
    && toTimestamp(item.endTime) > nowTimestamp
  ));
  const upcomingMaintenance = maintenanceWindows
    .filter(item => item.status === 'ACTIVE' && toTimestamp(item.startTime) > nowTimestamp)
    .sort((left, right) => toTimestamp(left.startTime) - toTimestamp(right.startTime))[0] || null;
  const currentShowtime = roomShowtimes.find(item => (
    toTimestamp(item.startTime) <= nowTimestamp && toTimestamp(item.endTime) > nowTimestamp
  )) || null;
  const nextShowtime = roomShowtimes.find(item => toTimestamp(item.startTime) > nowTimestamp) || null;
  const lastFinishedShowtime = [...roomShowtimes]
    .reverse()
    .find(item => toTimestamp(item.endTime) <= nowTimestamp) || null;
  const cleaningMinutes = Math.max(0, Number(room?.cleaningBufferMinutes || 0));
  const cleaningUntil = lastFinishedShowtime
    ? toTimestamp(lastFinishedShowtime.endTime) + cleaningMinutes * 60_000
    : INVALID_DATE;

  let presentation;
  if (room?.status === 'DRAFT' || room?.auditoriumStatus === 'DRAFT') {
    presentation = OPERATIONAL_PRESENTATION.SETUP;
  } else if (room?.status === 'INACTIVE' || room?.auditoriumStatus === 'INACTIVE') {
    presentation = OPERATIONAL_PRESENTATION.SUSPENDED;
  } else if (activeMaintenance || room?.status === 'MAINTENANCE' || room?.auditoriumStatus === 'MAINTENANCE') {
    presentation = OPERATIONAL_PRESENTATION.MAINTENANCE;
  } else if (currentShowtime) {
    presentation = OPERATIONAL_PRESENTATION.IN_SHOW;
  } else if (Number.isFinite(cleaningUntil) && cleaningUntil > nowTimestamp) {
    presentation = OPERATIONAL_PRESENTATION.CLEANING;
  } else if (Number(lockedSeatCount) > 0) {
    presentation = OPERATIONAL_PRESENTATION.ATTENTION;
  } else if (nextShowtime && toTimestamp(nextShowtime.startTime) - nowTimestamp <= 60 * 60_000) {
    presentation = OPERATIONAL_PRESENTATION.UPCOMING;
  } else {
    presentation = OPERATIONAL_PRESENTATION.IDLE;
  }

  return {
    ...presentation,
    priority: upcomingMaintenance ? Math.min(presentation.priority, 1) : presentation.priority,
    currentShowtime,
    nextShowtime,
    lastFinishedShowtime,
    cleaningUntil: Number.isFinite(cleaningUntil) ? new Date(cleaningUntil).toISOString() : null,
    activeMaintenance,
    upcomingMaintenance,
    lockedSeatCount: Number(lockedSeatCount || 0),
  };
};
