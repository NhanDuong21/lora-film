import {
  getBookingOptions,
  getMovieById,
  getMovies
} from '@/features/catalog/customer/services/movieService';

const MOVIE_DETAIL_TTL_MS = 10 * 60 * 1000;
const MOVIE_DETAIL_MAX_STALE_MS = 60 * 60 * 1000;
const RELATED_MOVIES_TTL_MS = 3 * 60 * 1000;
const RELATED_MOVIES_MAX_STALE_MS = 30 * 60 * 1000;
const BOOKING_OPTIONS_TTL_MS = 30 * 1000;
const BOOKING_OPTIONS_MAX_STALE_MS = 2 * 60 * 1000;

const movieDetailCache = new Map();
const relatedMoviesCache = new Map();
const bookingOptionsCache = new Map();

const readSnapshot = (cache, key, ttlMs, maxStaleMs) => {
  const entry = cache.get(key);
  if (!entry) return null;

  const ageMs = Date.now() - entry.updatedAt;
  if (ageMs > maxStaleMs) {
    cache.delete(key);
    return null;
  }

  return {
    data: entry.data,
    isFresh: ageMs <= ttlMs,
    updatedAt: entry.updatedAt
  };
};

const loadWithCache = async ({ cache, key, ttlMs, maxStaleMs, loader }) => {
  const snapshot = readSnapshot(cache, key, ttlMs, maxStaleMs);
  if (snapshot?.isFresh) return snapshot.data;

  const data = await loader();
  cache.set(key, { data, updatedAt: Date.now() });
  return data;
};

const movieDetailKey = movieId => String(movieId || '');
const relatedMoviesKey = status => status === 'UPCOMING' ? 'UPCOMING' : 'NOW_SHOWING';
const bookingOptionsKey = (movieIdentifier, from, to) => JSON.stringify([
  String(movieIdentifier || ''),
  from || '',
  to || ''
]);

export const getMovieDetailCacheSnapshot = movieId => readSnapshot(
  movieDetailCache,
  movieDetailKey(movieId),
  MOVIE_DETAIL_TTL_MS,
  MOVIE_DETAIL_MAX_STALE_MS
);

export const getMovieDetailWithCache = movieId => loadWithCache({
  cache: movieDetailCache,
  key: movieDetailKey(movieId),
  ttlMs: MOVIE_DETAIL_TTL_MS,
  maxStaleMs: MOVIE_DETAIL_MAX_STALE_MS,
  loader: () => getMovieById(movieId)
});

export const getRelatedMoviesCacheSnapshot = status => readSnapshot(
  relatedMoviesCache,
  relatedMoviesKey(status),
  RELATED_MOVIES_TTL_MS,
  RELATED_MOVIES_MAX_STALE_MS
);

export const getRelatedMoviesWithCache = (status, { signal } = {}) => loadWithCache({
  cache: relatedMoviesCache,
  key: relatedMoviesKey(status),
  ttlMs: RELATED_MOVIES_TTL_MS,
  maxStaleMs: RELATED_MOVIES_MAX_STALE_MS,
  loader: () => getMovies({
    page: 0,
    size: 8,
    status: relatedMoviesKey(status),
    sort: 'releaseDate,desc',
    signal
  })
});

export const getBookingOptionsCacheSnapshot = (movieIdentifier, { from, to } = {}) => readSnapshot(
  bookingOptionsCache,
  bookingOptionsKey(movieIdentifier, from, to),
  BOOKING_OPTIONS_TTL_MS,
  BOOKING_OPTIONS_MAX_STALE_MS
);

export const getBookingOptionsWithCache = (movieIdentifier, { from, to, signal } = {}) => loadWithCache({
  cache: bookingOptionsCache,
  key: bookingOptionsKey(movieIdentifier, from, to),
  ttlMs: BOOKING_OPTIONS_TTL_MS,
  maxStaleMs: BOOKING_OPTIONS_MAX_STALE_MS,
  loader: () => getBookingOptions(movieIdentifier, { from, to, signal })
});

export const clearMovieDetailCaches = () => {
  movieDetailCache.clear();
  relatedMoviesCache.clear();
  bookingOptionsCache.clear();
};
