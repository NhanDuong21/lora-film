const FRESH_FOR_MS = 30_000;
const RETAIN_FOR_MS = 5 * 60_000;
const MAX_ENTRIES = 40;

const responseCache = new Map();
const inFlightRequests = new Map();

const trimCache = () => {
  while (responseCache.size > MAX_ENTRIES) {
    responseCache.delete(responseCache.keys().next().value);
  }
};

export const buildShowtimeQueryCacheKey = (scope, params = {}) => {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&');
  return `${scope}?${query}`;
};

export const readShowtimeQueryCache = (key, now = Date.now()) => {
  const cached = responseCache.get(key);
  if (!cached) return null;

  const age = Math.max(0, now - cached.updatedAt);
  if (age > RETAIN_FOR_MS) {
    responseCache.delete(key);
    return null;
  }

  responseCache.delete(key);
  responseCache.set(key, cached);
  return {
    response: cached.response,
    isFresh: age <= FRESH_FOR_MS,
    updatedAt: cached.updatedAt,
  };
};

export const writeShowtimeQueryCache = (key, response, now = Date.now()) => {
  responseCache.delete(key);
  responseCache.set(key, { response, updatedAt: now });
  trimCache();
};

export const runShowtimeQueryOnce = (key, request) => {
  const pending = inFlightRequests.get(key);
  if (pending) return pending;

  const next = Promise.resolve()
    .then(request)
    .finally(() => {
      if (inFlightRequests.get(key) === next) inFlightRequests.delete(key);
    });
  inFlightRequests.set(key, next);
  return next;
};

export const invalidateShowtimeQueryCache = (scopePrefix = '') => {
  Array.from(responseCache.keys()).forEach(key => {
    if (!scopePrefix || key.startsWith(scopePrefix)) responseCache.delete(key);
  });
};

export const clearShowtimeQueryCache = () => {
  responseCache.clear();
  inFlightRequests.clear();
};

export const SHOWTIME_QUERY_CACHE_POLICY = Object.freeze({
  freshForMs: FRESH_FOR_MS,
  retainForMs: RETAIN_FOR_MS,
  maxEntries: MAX_ENTRIES,
});
