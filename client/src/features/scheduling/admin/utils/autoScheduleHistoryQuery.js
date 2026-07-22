export const AUTO_SCHEDULE_HISTORY_DEFAULTS = Object.freeze({
  cinemaPublicId: '',
  status: '',
  strategyVersion: '',
  scheduleFrom: '',
  scheduleTo: '',
  createdFrom: '',
  createdTo: '',
  page: 0,
  size: 10,
  sort: 'createdAt,desc',
});

export const AUTO_SCHEDULE_HISTORY_STATUSES = Object.freeze([
  'GENERATING',
  'PREVIEWED',
  'APPLYING',
  'APPLIED',
  'EXPIRED',
  'FAILED',
  'CANCELLED',
]);

export const AUTO_SCHEDULE_HISTORY_STRATEGIES = Object.freeze([
  'BALANCED_V1',
  'BALANCED_V1_S2',
  'BALANCED_V1_S3',
]);

export const AUTO_SCHEDULE_HISTORY_PAGE_SIZES = Object.freeze([10, 20, 50]);

export const AUTO_SCHEDULE_HISTORY_SORTS = Object.freeze([
  'createdAt,desc',
  'createdAt,asc',
  'scheduleFrom,asc',
  'scheduleFrom,desc',
  'scheduleTo,asc',
  'scheduleTo,desc',
  'status,asc',
  'status,desc',
  'cinemaName,asc',
  'cinemaName,desc',
  'totalCandidateCount,asc',
  'totalCandidateCount,desc',
  'selectedCandidateCount,asc',
  'selectedCandidateCount,desc',
  'appliedAt,asc',
  'appliedAt,desc',
]);

const STATUS_SET = new Set(AUTO_SCHEDULE_HISTORY_STATUSES);
const STRATEGY_SET = new Set(AUTO_SCHEDULE_HISTORY_STRATEGIES);
const PAGE_SIZE_SET = new Set(AUTO_SCHEDULE_HISTORY_PAGE_SIZES);
const SORT_SET = new Set(AUTO_SCHEDULE_HISTORY_SORTS);
const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;
const OFFSET_PATTERN = /(Z|[+-]\d{2}:\d{2})$/;

const text = value => (typeof value === 'string' ? value.trim() : '');

export const normalizeHistoryDate = value => {
  const normalized = text(value);
  const match = DATE_PATTERN.exec(normalized);
  if (!match) return '';
  const [, yearText, monthText, dayText] = match;
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  const parsed = new Date(Date.UTC(year, month - 1, day));
  return parsed.getUTCFullYear() === year
    && parsed.getUTCMonth() === month - 1
    && parsed.getUTCDate() === day
    ? normalized
    : '';
};

export const normalizeHistoryInstant = value => {
  const normalized = text(value);
  if (!OFFSET_PATTERN.test(normalized)) return '';
  const parsed = new Date(normalized);
  return Number.isFinite(parsed.getTime()) ? parsed.toISOString() : '';
};

export function parseAutoScheduleHistoryQuery(searchParams) {
  const page = Number.parseInt(searchParams.get('page') || '', 10);
  const size = Number.parseInt(searchParams.get('size') || '', 10);
  const status = text(searchParams.get('status')).toUpperCase();
  const strategyVersion = text(searchParams.get('strategyVersion')).toUpperCase();
  const sort = text(searchParams.get('sort'));

  return {
    cinemaPublicId: text(searchParams.get('cinemaPublicId')),
    status: STATUS_SET.has(status) ? status : '',
    strategyVersion: STRATEGY_SET.has(strategyVersion) ? strategyVersion : '',
    scheduleFrom: normalizeHistoryDate(searchParams.get('scheduleFrom')),
    scheduleTo: normalizeHistoryDate(searchParams.get('scheduleTo')),
    createdFrom: normalizeHistoryInstant(searchParams.get('createdFrom')),
    createdTo: normalizeHistoryInstant(searchParams.get('createdTo')),
    page: Number.isInteger(page) && page >= 0 ? page : AUTO_SCHEDULE_HISTORY_DEFAULTS.page,
    size: PAGE_SIZE_SET.has(size) ? size : AUTO_SCHEDULE_HISTORY_DEFAULTS.size,
    sort: SORT_SET.has(sort) ? sort : AUTO_SCHEDULE_HISTORY_DEFAULTS.sort,
  };
}

export function serializeAutoScheduleHistoryQuery(query) {
  const normalized = { ...AUTO_SCHEDULE_HISTORY_DEFAULTS, ...query };
  const params = new URLSearchParams();

  ['cinemaPublicId', 'status', 'strategyVersion', 'scheduleFrom', 'scheduleTo', 'createdFrom', 'createdTo']
    .forEach(key => {
      if (normalized[key]) params.set(key, String(normalized[key]));
    });
  if (normalized.page > 0) params.set('page', String(normalized.page));
  if (normalized.size !== AUTO_SCHEDULE_HISTORY_DEFAULTS.size) params.set('size', String(normalized.size));
  if (normalized.sort !== AUTO_SCHEDULE_HISTORY_DEFAULTS.sort) params.set('sort', normalized.sort);
  return params;
}

export function toAutoScheduleHistoryApiParams(query) {
  const params = {
    page: query.page,
    size: query.size,
    sort: query.sort,
  };
  ['cinemaPublicId', 'status', 'strategyVersion', 'scheduleFrom', 'scheduleTo', 'createdFrom', 'createdTo']
    .forEach(key => {
      if (query[key]) params[key] = query[key];
    });
  return params;
}

export function getAutoScheduleHistoryRangeError(query) {
  if (query.scheduleFrom && query.scheduleTo && query.scheduleFrom > query.scheduleTo) {
    return 'Ngày bắt đầu lịch chiếu không được sau ngày kết thúc.';
  }
  if (query.createdFrom && query.createdTo && query.createdFrom >= query.createdTo) {
    return 'Thời điểm tạo từ phải trước thời điểm tạo đến.';
  }
  return '';
}

export function hasAutoScheduleHistoryFilters(query) {
  return Boolean(
    query.cinemaPublicId
    || query.status
    || query.strategyVersion
    || query.scheduleFrom
    || query.scheduleTo
    || query.createdFrom
    || query.createdTo
  );
}

export function resetAutoScheduleHistoryFilters(query) {
  return {
    ...query,
    ...AUTO_SCHEDULE_HISTORY_DEFAULTS,
    size: query.size,
    sort: query.sort,
  };
}

export function instantToDateTimeLocal(value) {
  const date = new Date(value || '');
  if (!Number.isFinite(date.getTime())) return '';
  const pad = part => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function dateTimeLocalToInstant(value) {
  const normalized = text(value);
  if (!normalized) return '';
  const date = new Date(normalized);
  return Number.isFinite(date.getTime()) ? date.toISOString() : '';
}
