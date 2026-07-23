export const SHOWTIME_FALLBACK_TIMEZONE = 'UTC';

const formatterCache = new Map();
const timezoneResolutionCache = new Map();

const getFormatter = (locale, options) => {
  const key = `${locale}:${JSON.stringify(options)}`;
  if (!formatterCache.has(key)) {
    formatterCache.set(key, new Intl.DateTimeFormat(locale, options));
  }
  return formatterCache.get(key);
};

const toValidDate = value => {
  if (!value) return null;
  const date = value instanceof Date ? new Date(value.getTime()) : new Date(value);
  return Number.isFinite(date.getTime()) ? date : null;
};

export const resolveShowtimeCinemaTimezone = timezone => {
  const requestedTimezone = typeof timezone === 'string' && timezone.trim()
    ? timezone.trim()
    : null;
  const cacheKey = requestedTimezone || '__MISSING__';
  if (timezoneResolutionCache.has(cacheKey)) {
    return timezoneResolutionCache.get(cacheKey);
  }

  let resolution;
  if (requestedTimezone) {
    try {
      getFormatter('en-US', { timeZone: requestedTimezone }).format(0);
      resolution = { timezone: requestedTimezone, requestedTimezone, usedFallback: false };
    } catch {
      // Fall through to the deterministic UTC presentation below.
    }
  }
  if (!resolution) {
    resolution = {
      timezone: SHOWTIME_FALLBACK_TIMEZONE,
      requestedTimezone,
      usedFallback: true,
    };
  }
  const cachedResolution = Object.freeze(resolution);
  timezoneResolutionCache.set(cacheKey, cachedResolution);
  return cachedResolution;
};

const formatInstant = (instant, timezone, options) => {
  const date = toValidDate(instant);
  if (!date) return '—';
  const resolution = resolveShowtimeCinemaTimezone(timezone);
  return getFormatter('vi-VN', {
    timeZone: resolution.timezone,
    calendar: 'gregory',
    numberingSystem: 'latn',
    ...options,
  }).format(date);
};

export const formatShowtimeCinemaDate = (instant, timezone) => formatInstant(
  instant,
  timezone,
  { day: '2-digit', month: '2-digit', year: 'numeric' },
);

export const formatShowtimeCinemaTime = (instant, timezone) => formatInstant(
  instant,
  timezone,
  { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' },
);

export const formatShowtimeCinemaDateTime = (instant, timezone) => formatInstant(
  instant,
  timezone,
  {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  },
);
