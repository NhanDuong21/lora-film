export const FALLBACK_PREVIEW_TIMEZONE = 'UTC';
export const DEFAULT_OPERATIONAL_TIMEZONE = 'Asia/Ho_Chi_Minh';
export const INVALID_PREVIEW_DATE_KEY = '__INVALID_PREVIEW_DATE__';
export const UNKNOWN_SERVICE_DATE_KEY = '__UNKNOWN_SERVICE_DATE__';
export const UNKNOWN_SERVICE_DATE_LABEL = 'Không xác định ngày vận hành';

const DATE_KEY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;
const formatterCache = new Map();
const VIETNAMESE_WEEKDAYS = [
  'Chủ nhật',
  'Thứ hai',
  'Thứ ba',
  'Thứ tư',
  'Thứ năm',
  'Thứ sáu',
  'Thứ bảy',
];

const isLeapYear = year => year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);

const daysInMonth = (year, month) => {
  if (month === 2) return isLeapYear(year) ? 29 : 28;
  return [4, 6, 9, 11].includes(month) ? 30 : 31;
};

const parseCalendarDateKey = value => {
  if (typeof value !== 'string') return null;
  const match = DATE_KEY_PATTERN.exec(value);
  if (!match) return null;

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (year < 1 || month < 1 || month > 12 || day < 1 || day > daysInMonth(year, month)) {
    return null;
  }
  return { year, month, day, dateKey: value };
};

const getCalendarDayNumber = parts => (
  Math.floor(Date.UTC(parts.year, parts.month - 1, parts.day) / 86_400_000)
);

const getGregorianWeekday = ({ year, month, day }) => {
  const daysBeforeMonth = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
  const priorYear = year - 1;
  let elapsedDays = (priorYear * 365)
    + Math.floor(priorYear / 4)
    - Math.floor(priorYear / 100)
    + Math.floor(priorYear / 400)
    + daysBeforeMonth[month - 1]
    + day - 1;
  if (month > 2 && isLeapYear(year)) elapsedDays += 1;
  return (elapsedDays + 1) % 7;
};

export const getServiceDateKey = serviceDate => (
  parseCalendarDateKey(serviceDate)?.dateKey || UNKNOWN_SERVICE_DATE_KEY
);

export const compareServiceDateKeys = (first, second) => {
  if (first === UNKNOWN_SERVICE_DATE_KEY) return second === UNKNOWN_SERVICE_DATE_KEY ? 0 : 1;
  if (second === UNKNOWN_SERVICE_DATE_KEY) return -1;
  return first.localeCompare(second);
};

export const addServiceDateDays = (dateKey, amount) => {
  const parts = parseCalendarDateKey(dateKey);
  const dayAmount = Number(amount);
  if (!parts || !Number.isInteger(dayAmount)) return null;
  const shifted = new Date(Date.UTC(parts.year, parts.month - 1, parts.day + dayAmount, 12));
  return [
    shifted.getUTCFullYear(),
    String(shifted.getUTCMonth() + 1).padStart(2, '0'),
    String(shifted.getUTCDate()).padStart(2, '0'),
  ].join('-');
};

export const buildOperationalDateRange = (startDateKey, count = 7) => {
  const safeCount = Math.max(0, Math.min(Number(count) || 0, 14));
  if (!parseCalendarDateKey(startDateKey) || safeCount === 0) return [];
  return Array.from({ length: safeCount }, (_, index) => addServiceDateDays(startDateKey, index));
};

export const formatServiceDateKey = (dateKey, { weekday = false } = {}) => {
  if (dateKey === UNKNOWN_SERVICE_DATE_KEY) return UNKNOWN_SERVICE_DATE_LABEL;
  const parts = parseCalendarDateKey(dateKey);
  if (!parts) return UNKNOWN_SERVICE_DATE_LABEL;

  const formatted = `${String(parts.day).padStart(2, '0')}/${String(parts.month).padStart(2, '0')}/${String(parts.year).padStart(4, '0')}`;
  return weekday
    ? `${VIETNAMESE_WEEKDAYS[getGregorianWeekday(parts)]}, ${formatted}`
    : formatted;
};

const toValidDate = value => {
  if (!value) return null;
  const date = value instanceof Date ? new Date(value.getTime()) : new Date(value);
  return Number.isFinite(date.getTime()) ? date : null;
};

const getFormatter = (locale, options) => {
  const key = `${locale}:${JSON.stringify(options)}`;
  if (!formatterCache.has(key)) {
    formatterCache.set(key, new Intl.DateTimeFormat(locale, options));
  }
  return formatterCache.get(key);
};

export const resolveCinemaTimezone = timezone => {
  if (typeof timezone === 'string' && timezone.trim()) {
    const requestedTimezone = timezone.trim();
    try {
      getFormatter('en-US', { timeZone: requestedTimezone }).format(0);
      return { timezone: requestedTimezone, requestedTimezone, usedFallback: false };
    } catch {
      // Use the deterministic UTC fallback below.
    }
  }

  return {
    timezone: FALLBACK_PREVIEW_TIMEZONE,
    requestedTimezone: timezone || null,
    usedFallback: true,
  };
};

const getCinemaParts = (instant, timezone, includeTime) => {
  const date = toValidDate(instant);
  if (!date) return null;

  const effectiveTimezone = resolveCinemaTimezone(timezone).timezone;
  const options = {
    timeZone: effectiveTimezone,
    calendar: 'gregory',
    numberingSystem: 'latn',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  };
  if (includeTime) {
    options.hour = '2-digit';
    options.minute = '2-digit';
    options.hourCycle = 'h23';
  }

  const parts = getFormatter('en-CA', options).formatToParts(date);
  const values = Object.fromEntries(
    parts.filter(part => part.type !== 'literal').map(part => [part.type, part.value]),
  );
  const year = Number(values.year);
  const month = Number(values.month);
  const day = Number(values.day);
  if (![year, month, day].every(Number.isFinite)) return null;

  const dateKey = `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  if (!includeTime) return { year, month, day, dateKey };

  const hour = Number(values.hour);
  const minute = Number(values.minute);
  if (![hour, minute].every(Number.isFinite)) return null;
  return {
    year,
    month,
    day,
    dateKey,
    hour,
    minute,
    minutesSinceMidnight: (hour * 60) + minute,
  };
};

export const getCinemaDateKey = (instant, timezone) => (
  getCinemaParts(instant, timezone, false)?.dateKey || null
);

export const getOperationalTodayDateKey = (
  timezone = DEFAULT_OPERATIONAL_TIMEZONE,
  now = new Date(),
) => (
  getCinemaDateKey(now, timezone)
  || getCinemaDateKey(now, DEFAULT_OPERATIONAL_TIMEZONE)
);

export const getCinemaTimeParts = (instant, timezone) => (
  getCinemaParts(instant, timezone, true)
);

export const formatCinemaDate = (instant, timezone, { weekday = false } = {}) => {
  const date = toValidDate(instant);
  if (!date) return '—';
  return getFormatter('vi-VN', {
    timeZone: resolveCinemaTimezone(timezone).timezone,
    calendar: 'gregory',
    numberingSystem: 'latn',
    ...(weekday ? { weekday: 'long' } : {}),
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
};

export const formatCinemaTime = (instant, timezone) => {
  const date = toValidDate(instant);
  if (!date) return '—';
  return getFormatter('vi-VN', {
    timeZone: resolveCinemaTimezone(timezone).timezone,
    numberingSystem: 'latn',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(date);
};

export const formatCinemaDateTime = (instant, timezone) => {
  const date = toValidDate(instant);
  if (!date) return '—';
  return getFormatter('vi-VN', {
    timeZone: resolveCinemaTimezone(timezone).timezone,
    calendar: 'gregory',
    numberingSystem: 'latn',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(date);
};

export const formatPreviewDateKey = (dateKey, { weekday = false } = {}) => {
  if (dateKey === INVALID_PREVIEW_DATE_KEY) return 'Ngày không hợp lệ';
  const parts = parseCalendarDateKey(dateKey);
  if (!parts) return '—';

  const date = new Date(Date.UTC(parts.year, parts.month - 1, parts.day, 12));
  return getFormatter('vi-VN', {
    timeZone: 'UTC',
    calendar: 'gregory',
    numberingSystem: 'latn',
    ...(weekday ? { weekday: 'long' } : {}),
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
};

export const formatPreviewDateRange = (scheduleFrom, scheduleTo) => {
  const from = formatPreviewDateKey(scheduleFrom);
  const to = formatPreviewDateKey(scheduleTo);
  if (from === '—') return to;
  if (to === '—') return from;
  return `${from} – ${to}`;
};

export const formatCinemaTimeRange = (startTime, endTime, timezone) => {
  const startParts = getCinemaTimeParts(startTime, timezone);
  const endParts = getCinemaTimeParts(endTime, timezone);
  if (!startParts || !endParts) return '—';

  const range = `${formatCinemaTime(startTime, timezone)} – ${formatCinemaTime(endTime, timezone)}`;
  if (endParts.dateKey === startParts.dateKey) return range;
  const difference = getCalendarDayNumber(endParts) - getCalendarDayNumber(startParts);
  return difference === 1
    ? `${range} (+1 ngày)`
    : `${range} (${formatPreviewDateKey(endParts.dateKey)})`;
};

export const getCinemaMinuteOffset = (instant, serviceDate, timezone) => {
  const authoritativeDate = parseCalendarDateKey(serviceDate);
  const localParts = getCinemaTimeParts(instant, timezone);
  if (!authoritativeDate || !localParts) return null;

  const dayOffset = getCalendarDayNumber(localParts) - getCalendarDayNumber(authoritativeDate);
  return (dayOffset * 24 * 60) + localParts.minutesSinceMidnight;
};

export const getCandidateTimelineOffsets = (candidate, serviceDate, timezone) => {
  const startInstant = toValidDate(candidate?.startTime);
  const endInstant = toValidDate(candidate?.endTime);
  const occupancyInstant = toValidDate(candidate?.occupancyEndTime);
  const startMinute = getCinemaMinuteOffset(candidate?.startTime, serviceDate, timezone);
  const endMinute = getCinemaMinuteOffset(candidate?.endTime, serviceDate, timezone);
  const occupancyEndMinute = getCinemaMinuteOffset(candidate?.occupancyEndTime, serviceDate, timezone);

  if (
    !startInstant
    || !endInstant
    || !occupancyInstant
    || endInstant <= startInstant
    || occupancyInstant < endInstant
    || ![startMinute, endMinute, occupancyEndMinute].every(Number.isFinite)
    || endMinute <= startMinute
    || occupancyEndMinute < endMinute
  ) {
    return {
      valid: false,
      startMinute: null,
      endMinute: null,
      occupancyEndMinute: null,
    };
  }

  return { valid: true, startMinute, endMinute, occupancyEndMinute };
};

export const buildDynamicTimelineWindow = candidates => {
  const eligible = (candidates || []).filter(candidate => (
    candidate?.timelineEligible
    && Number.isFinite(candidate.startMinuteOffset)
    && Number.isFinite(candidate.occupancyEndMinuteOffset)
  ));
  if (eligible.length === 0) return null;

  const earliestStart = Math.min(...eligible.map(candidate => candidate.startMinuteOffset));
  const latestOccupancyEnd = Math.max(...eligible.map(candidate => candidate.occupancyEndMinuteOffset));
  const startMinute = Math.floor(earliestStart / 60) * 60;
  let endMinute = Math.ceil(latestOccupancyEnd / 60) * 60;
  if (endMinute <= startMinute) endMinute = startMinute + 60;

  return {
    startMinute,
    endMinute,
    totalMinutes: endMinute - startMinute,
    ticks: Array.from(
      { length: Math.floor((endMinute - startMinute) / 60) + 1 },
      (_, index) => startMinute + (index * 60),
    ),
  };
};

export const formatTimelineMinute = minuteOffset => {
  if (!Number.isFinite(minuteOffset)) return '—';
  const sign = minuteOffset < 0 ? '−' : '';
  const absolute = Math.abs(minuteOffset);
  const hours = Math.floor(absolute / 60);
  const minutes = absolute % 60;
  return `${sign}${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
};
