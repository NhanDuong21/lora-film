export const FALLBACK_PREVIEW_TIMEZONE = 'UTC';
export const INVALID_PREVIEW_DATE_KEY = '__INVALID_PREVIEW_DATE__';
export const UNKNOWN_SERVICE_DATE_KEY = '__UNKNOWN_SERVICE_DATE__';
export const UNKNOWN_SERVICE_DATE_LABEL = 'Không xác định ngày vận hành';
export const TIMELINE_START_HOUR = 8;
export const TIMELINE_END_HOUR = 24;

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

const parseCalendarDateKey = (value) => {
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

export const getServiceDateKey = serviceDate =>
  parseCalendarDateKey(serviceDate)?.dateKey || UNKNOWN_SERVICE_DATE_KEY;

export const compareServiceDateKeys = (first, second) => {
  if (first === UNKNOWN_SERVICE_DATE_KEY) return second === UNKNOWN_SERVICE_DATE_KEY ? 0 : 1;
  if (second === UNKNOWN_SERVICE_DATE_KEY) return -1;
  return first.localeCompare(second);
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

const toValidDate = (value) => {
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

export const resolveCinemaTimezone = (timezone) => {
  if (typeof timezone === 'string' && timezone.trim()) {
    const requestedTimezone = timezone.trim();
    try {
      getFormatter('en-US', { timeZone: requestedTimezone }).format(0);
      return {
        timezone: requestedTimezone,
        requestedTimezone,
        usedFallback: false,
      };
    } catch {
      // Fall through to the deterministic fallback.
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
    parts.filter((part) => part.type !== 'literal').map((part) => [part.type, part.value]),
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

export const getCinemaDateKey = (instant, timezone) =>
  getCinemaParts(instant, timezone, false)?.dateKey || null;

export const getCinemaTimeParts = (instant, timezone) =>
  getCinemaParts(instant, timezone, true);

export const formatCinemaDate = (instant, timezone, { weekday = false } = {}) => {
  const date = toValidDate(instant);
  if (!date) return '—';
  const effectiveTimezone = resolveCinemaTimezone(timezone).timezone;
  return getFormatter('vi-VN', {
    timeZone: effectiveTimezone,
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
  const effectiveTimezone = resolveCinemaTimezone(timezone).timezone;
  return getFormatter('vi-VN', {
    timeZone: effectiveTimezone,
    numberingSystem: 'latn',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(date);
};

export const formatCinemaDateTime = (instant, timezone) => {
  const date = toValidDate(instant);
  if (!date) return '—';
  const effectiveTimezone = resolveCinemaTimezone(timezone).timezone;
  return getFormatter('vi-VN', {
    timeZone: effectiveTimezone,
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
  const match = DATE_KEY_PATTERN.exec(dateKey || '');
  if (!match) return '—';

  const [, yearText, monthText, dayText] = match;
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  const date = new Date(Date.UTC(year, month - 1, day, 12));
  if (
    date.getUTCFullYear() !== year
    || date.getUTCMonth() !== month - 1
    || date.getUTCDate() !== day
  ) {
    return '—';
  }

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

export const formatCinemaTimeRange = (startTime, endTime, timezone) => {
  const startParts = getCinemaTimeParts(startTime, timezone);
  const endParts = getCinemaTimeParts(endTime, timezone);
  if (!startParts || !endParts) return '—';

  const range = `${formatCinemaTime(startTime, timezone)} - ${formatCinemaTime(endTime, timezone)}`;
  if (endParts.dateKey === startParts.dateKey) return range;

  const startDate = new Date(`${startParts.dateKey}T12:00:00Z`);
  const endDate = new Date(`${endParts.dateKey}T12:00:00Z`);
  const dayDifference = Math.round((endDate.getTime() - startDate.getTime()) / 86_400_000);
  return dayDifference === 1
    ? `${range} (+1 ngày)`
    : `${range} (${formatPreviewDateKey(endParts.dateKey)})`;
};

export const getTimelineRange = (
  startTime,
  endTime,
  timezone,
  { startHour = TIMELINE_START_HOUR, endHour = TIMELINE_END_HOUR } = {},
) => {
  const startDate = toValidDate(startTime);
  const endDate = toValidDate(endTime);
  const startParts = getCinemaTimeParts(startTime, timezone);
  const endParts = getCinemaTimeParts(endTime, timezone);
  const totalMinutes = (endHour - startHour) * 60;

  if (!startDate || !endDate || endDate <= startDate || !startParts || !endParts || totalMinutes <= 0) {
    return { isVisible: false, isMalformed: true, left: '0%', width: '0%' };
  }

  const windowStartMinutes = startHour * 60;
  const windowEndMinutes = endHour * 60;
  let localEndMinutes;

  if (endParts.dateKey === startParts.dateKey) {
    localEndMinutes = endParts.minutesSinceMidnight;
  } else if (endParts.dateKey > startParts.dateKey) {
    localEndMinutes = 24 * 60;
  } else {
    return { isVisible: false, isMalformed: true, left: '0%', width: '0%' };
  }

  const visibleStartMinutes = Math.max(windowStartMinutes, startParts.minutesSinceMidnight);
  const visibleEndMinutes = Math.min(windowEndMinutes, localEndMinutes);
  if (visibleEndMinutes <= visibleStartMinutes) {
    return {
      isVisible: false,
      isMalformed: false,
      isOutsideRange: true,
      startDateKey: startParts.dateKey,
      left: '0%',
      width: '0%',
    };
  }

  const leftPercent = ((visibleStartMinutes - windowStartMinutes) / totalMinutes) * 100;
  const naturalWidth = ((visibleEndMinutes - visibleStartMinutes) / totalMinutes) * 100;
  const widthPercent = Math.min(100 - leftPercent, Math.max(2, naturalWidth));

  return {
    isVisible: true,
    isMalformed: false,
    isOutsideRange: false,
    isClippedAtStart: startParts.minutesSinceMidnight < windowStartMinutes,
    isClippedAtEnd: localEndMinutes > windowEndMinutes || endParts.dateKey !== startParts.dateKey,
    startDateKey: startParts.dateKey,
    left: `${leftPercent}%`,
    width: `${widthPercent}%`,
  };
};
