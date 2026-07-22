export const ADMIN_MOVIE_QUERY_DEFAULTS = Object.freeze({
  status: 'DRAFT',
  keyword: '',
  page: 0,
  size: 10,
  sort: 'releaseDate,desc',
  source: '',
  healthStatus: '',
  hasPrimaryPoster: '',
  hasActiveVersion: '',
  hasShowtime: '',
  genrePublicId: '',
  country: '',
  releaseDateFrom: '',
  releaseDateTo: '',
  tmdbUpdatedFrom: '',
  tmdbUpdatedTo: '',
});

export const ADVANCED_FILTER_KEYS = Object.freeze([
  'source',
  'healthStatus',
  'hasPrimaryPoster',
  'hasActiveVersion',
  'hasShowtime',
  'genrePublicId',
  'country',
  'releaseDateFrom',
  'releaseDateTo',
  'tmdbUpdatedFrom',
  'tmdbUpdatedTo',
]);

const SUPPORTED_PARAMS = new Set(Object.keys(ADMIN_MOVIE_QUERY_DEFAULTS));
const STATUSES = new Set(['ALL', 'DRAFT', 'UPCOMING', 'NOW_SHOWING', 'ENDED', 'INACTIVE']);
const SOURCES = new Set(['TMDB', 'MANUAL']);
const HEALTH_STATUSES = new Set(['READY', 'WARNING', 'BLOCKED']);
const BOOLEAN_VALUES = new Set(['true', 'false']);
const PAGE_SIZES = new Set([5, 10, 20, 50]);
const SORT_FIELDS = new Set(['updatedAt', 'releaseDate', 'title', 'tmdbLastUpdated', 'createdAt']);

const normalizedText = value => (typeof value === 'string' ? value.trim() : '');

const normalizedEnum = (value, allowed, fallback = '') => {
  const normalized = normalizedText(value).toUpperCase();
  return allowed.has(normalized) ? normalized : fallback;
};

const normalizedBoolean = value => {
  const normalized = normalizedText(value).toLowerCase();
  return BOOLEAN_VALUES.has(normalized) ? normalized : '';
};

const normalizedDate = value => {
  const normalized = normalizedText(value);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(normalized)) return '';
  const [year, month, day] = normalized.split('-').map(Number);
  const parsed = new Date(Date.UTC(year, month - 1, day));
  return parsed.getUTCFullYear() === year
    && parsed.getUTCMonth() === month - 1
    && parsed.getUTCDate() === day
    ? normalized
    : '';
};

const normalizedSort = value => {
  const normalized = normalizedText(value);
  const parts = normalized.split(',');
  if (parts.length !== 2) return ADMIN_MOVIE_QUERY_DEFAULTS.sort;
  const [field, rawDirection] = parts;
  const direction = rawDirection.toLowerCase();
  return SORT_FIELDS.has(field) && (direction === 'asc' || direction === 'desc')
    ? `${field},${direction}`
    : ADMIN_MOVIE_QUERY_DEFAULTS.sort;
};

export function parseAdminMovieQuery(searchParams) {
  const page = Number.parseInt(searchParams.get('page') || '', 10);
  const size = Number.parseInt(searchParams.get('size') || '', 10);

  return {
    status: normalizedEnum(searchParams.get('status'), STATUSES, ADMIN_MOVIE_QUERY_DEFAULTS.status),
    keyword: normalizedText(searchParams.get('keyword')),
    page: Number.isInteger(page) && page >= 0 ? page : ADMIN_MOVIE_QUERY_DEFAULTS.page,
    size: PAGE_SIZES.has(size) ? size : ADMIN_MOVIE_QUERY_DEFAULTS.size,
    sort: normalizedSort(searchParams.get('sort')),
    source: normalizedEnum(searchParams.get('source'), SOURCES),
    healthStatus: normalizedEnum(searchParams.get('healthStatus'), HEALTH_STATUSES),
    hasPrimaryPoster: normalizedBoolean(searchParams.get('hasPrimaryPoster')),
    hasActiveVersion: normalizedBoolean(searchParams.get('hasActiveVersion')),
    hasShowtime: normalizedBoolean(searchParams.get('hasShowtime')),
    genrePublicId: normalizedText(searchParams.get('genrePublicId')),
    country: normalizedText(searchParams.get('country')),
    releaseDateFrom: normalizedDate(searchParams.get('releaseDateFrom')),
    releaseDateTo: normalizedDate(searchParams.get('releaseDateTo')),
    tmdbUpdatedFrom: normalizedDate(searchParams.get('tmdbUpdatedFrom')),
    tmdbUpdatedTo: normalizedDate(searchParams.get('tmdbUpdatedTo')),
  };
}

export function serializeAdminMovieQuery(query) {
  const params = new URLSearchParams();
  const normalized = { ...ADMIN_MOVIE_QUERY_DEFAULTS, ...query };

  if (normalized.status !== ADMIN_MOVIE_QUERY_DEFAULTS.status) params.set('status', normalized.status);
  if (normalized.keyword) params.set('keyword', normalized.keyword.trim());
  if (normalized.page > 0) params.set('page', String(normalized.page));
  if (normalized.size !== ADMIN_MOVIE_QUERY_DEFAULTS.size) params.set('size', String(normalized.size));
  if (normalized.sort !== ADMIN_MOVIE_QUERY_DEFAULTS.sort) params.set('sort', normalized.sort);

  ADVANCED_FILTER_KEYS.forEach(key => {
    if (normalized[key]) params.set(key, String(normalized[key]));
  });
  return params;
}

export function toMovieApiParams(query) {
  const params = {};
  Object.entries(query).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params[key] = value;
  });
  return params;
}

export function hasUnsupportedMovieParams(searchParams) {
  return [...searchParams.keys()].some(key => !SUPPORTED_PARAMS.has(key));
}

export function countAdvancedFilters(query) {
  let count = ['source', 'healthStatus', 'hasPrimaryPoster', 'hasActiveVersion', 'hasShowtime', 'genrePublicId', 'country']
    .filter(key => query[key]).length;
  if (query.releaseDateFrom || query.releaseDateTo) count += 1;
  if (query.tmdbUpdatedFrom || query.tmdbUpdatedTo) count += 1;
  return count;
}

export function clearAdvancedMovieFilters(query) {
  const cleared = { ...query, page: 0, sort: ADMIN_MOVIE_QUERY_DEFAULTS.sort };
  ADVANCED_FILTER_KEYS.forEach(key => { cleared[key] = ''; });
  return cleared;
}
