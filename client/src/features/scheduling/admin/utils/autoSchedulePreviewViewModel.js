import {
  compareServiceDateKeys,
  formatCinemaDateTime,
  formatCinemaTime,
  getCandidateTimelineOffsets,
  getServiceDateKey,
  UNKNOWN_SERVICE_DATE_KEY,
} from './autoSchedulePreviewDateTime';
import { getCandidateApplyStateMeta } from './autoSchedulePreviewLifecycle';
import { getOperationalReasonPresentation } from './autoScheduleOperationalInsights';

const REASON_LABELS = Object.freeze({
  SHOWTIME_OUTSIDE_OPERATING_HOURS: 'Ngoài giờ hoạt động của cụm rạp',
  SHOWTIME_OVERLAPS_EXISTING: 'Trùng với suất chiếu hiện có',
  SHOWTIME_OVERLAP_CONFLICT: 'Trùng với suất chiếu hiện có',
  SHOWTIME_OUTSIDE_RELEASE_WINDOW: 'Ngoài thời gian phát hành của phim',
  CINEMA_OPERATING_HOURS_NOT_CONFIGURED: 'Chưa cấu hình giờ hoạt động cho ngày này',
  SHOWTIME_OVERLAPS_CINEMA_CLOSURE: 'Trùng với lịch đóng cửa của rạp',
  SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE: 'Trùng với lịch bảo trì phòng chiếu',
  MOVIE_NOT_ELIGIBLE: 'Phim chưa đủ điều kiện',
  MOVIE_STATUS_NOT_ELIGIBLE: 'Trạng thái phim không cho phép tạo suất chiếu',
  MOVIE_VERSION_NOT_ACTIVE: 'Định dạng phim không hoạt động',
  AUDITORIUM_UNAVAILABLE: 'Phòng chiếu không khả dụng',
  NOT_ENOUGH_CLEANING_TIME: 'Không đủ thời gian dọn dẹp',
});

const MOVIE_PALETTE = Object.freeze([
  { solid: '#f97316', border: '#fb923c', text: '#18181b', cleaning: 'rgba(249,115,22,0.42)' },
  { solid: '#38bdf8', border: '#7dd3fc', text: '#082f49', cleaning: 'rgba(56,189,248,0.42)' },
  { solid: '#a78bfa', border: '#c4b5fd', text: '#2e1065', cleaning: 'rgba(167,139,250,0.42)' },
  { solid: '#34d399', border: '#6ee7b7', text: '#022c22', cleaning: 'rgba(52,211,153,0.42)' },
  { solid: '#f472b6', border: '#f9a8d4', text: '#500724', cleaning: 'rgba(244,114,182,0.42)' },
  { solid: '#facc15', border: '#fde047', text: '#422006', cleaning: 'rgba(250,204,21,0.42)' },
  { solid: '#fb7185', border: '#fda4af', text: '#4c0519', cleaning: 'rgba(251,113,133,0.42)' },
  { solid: '#2dd4bf', border: '#5eead4', text: '#042f2e', cleaning: 'rgba(45,212,191,0.42)' },
]);

export const TIMELINE_ZOOM_MODES = Object.freeze({
  FIT: 'FIT',
  COMPACT: '30',
  COMFORTABLE: '60',
  DETAILED: '120',
});

const collator = new Intl.Collator('vi-VN', { sensitivity: 'base', numeric: true });

const hashStableKey = value => {
  const text = String(value || 'unknown-movie');
  let hash = 0;
  for (let index = 0; index < text.length; index += 1) {
    hash = ((hash * 31) + text.charCodeAt(index)) >>> 0;
  }
  return hash;
};

export const getMoviePalette = movieKey => {
  const index = hashStableKey(movieKey) % MOVIE_PALETTE.length;
  return { ...MOVIE_PALETTE[index], index };
};

const getConciseReason = item => {
  const code = item.rejectionCode || item.applyErrorCode;
  if (code && REASON_LABELS[code]) return REASON_LABELS[code];
  if (code) return getOperationalReasonPresentation(code).label;

  const rawReason = item.rejectionReason || item.applyErrorMessage || '';
  const normalized = rawReason.toUpperCase();
  const codeMatch = Object.entries(REASON_LABELS).find(([knownCode]) => (
    normalized.includes(knownCode)
  ));
  if (codeMatch) return codeMatch[1];
  if (!rawReason) return '';
  return rawReason.length > 120 ? `${rawReason.slice(0, 117)}…` : rawReason;
};

export const buildCandidateViewModels = (items, { selectedItemIds, timezone }) => {
  const selectedIds = selectedItemIds instanceof Set
    ? selectedItemIds
    : new Set(selectedItemIds || []);

  return (items || []).map(item => {
    const id = item.itemPublicId;
    const serviceDate = getServiceDateKey(item.serviceDate);
    const offsets = getCandidateTimelineOffsets(item, serviceDate, timezone);
    const movieKey = item.moviePublicId || item.movieVersionPublicId || item.movieTitle || id;
    const auditoriumKey = item.auditoriumPublicId || item.auditoriumName || 'unknown-auditorium';
    const applyState = getCandidateApplyStateMeta(item.applyStatus);

    return {
      id,
      raw: item,
      serviceDate,
      startTimeDisplay: formatCinemaTime(item.startTime, timezone),
      endTimeDisplay: formatCinemaTime(item.endTime, timezone),
      occupancyEndTimeDisplay: formatCinemaTime(item.occupancyEndTime, timezone),
      startDateTimeDisplay: formatCinemaDateTime(item.startTime, timezone),
      endDateTimeDisplay: formatCinemaDateTime(item.endTime, timezone),
      occupancyEndDateTimeDisplay: formatCinemaDateTime(item.occupancyEndTime, timezone),
      startMinuteOffset: offsets.startMinute,
      endMinuteOffset: offsets.endMinute,
      occupancyEndMinuteOffset: offsets.occupancyEndMinute,
      moviePublicId: item.moviePublicId || null,
      movieKey,
      movieTitle: item.movieTitle || 'Phim không xác định',
      movieVersionPublicId: item.movieVersionPublicId || null,
      versionName: item.versionName || item.format || 'Không xác định',
      format: item.format || null,
      audioLanguage: item.audioLanguage || null,
      auditoriumPublicId: item.auditoriumPublicId || null,
      auditoriumName: item.auditoriumName || item.auditoriumPublicId || 'Phòng không xác định',
      auditoriumKey,
      selected: selectedIds.has(id),
      validationStatus: item.validationStatus || 'UNKNOWN',
      applyStatus: item.applyStatus || 'UNKNOWN',
      applyState,
      score: item.score ?? null,
      rank: item.rankingPosition ?? null,
      conciseReason: getConciseReason(item),
      palette: getMoviePalette(movieKey),
      createdShowtimePublicId: item.applyStatus === 'CREATED'
        ? item.createdShowtimePublicId || null
        : null,
      createdShowtimePath: item.applyStatus === 'CREATED' && item.createdShowtimePublicId
        ? `/admin/showtimes/${item.createdShowtimePublicId}`
        : null,
      timelineEligible: serviceDate !== UNKNOWN_SERVICE_DATE_KEY && offsets.valid,
      technicalDetails: {
        itemPublicId: id,
        moviePublicId: item.moviePublicId || null,
        movieVersionPublicId: item.movieVersionPublicId || null,
        auditoriumPublicId: item.auditoriumPublicId || null,
        startTime: item.startTime || null,
        endTime: item.endTime || null,
        occupancyEndTime: item.occupancyEndTime || null,
        rejectionCode: item.rejectionCode || null,
        rejectionReason: item.rejectionReason || null,
        applyErrorCode: item.applyErrorCode || null,
        applyErrorMessage: item.applyErrorMessage || null,
        scoreBreakdown: item.scoreBreakdown || null,
      },
    };
  });
};

export const sortCandidateViewModels = candidates => (
  [...(candidates || [])].sort((left, right) => {
    if (left.selected !== right.selected) return left.selected ? -1 : 1;
    const dateOrder = compareServiceDateKeys(left.serviceDate, right.serviceDate);
    if (dateOrder !== 0) return dateOrder;
    const auditoriumOrder = collator.compare(left.auditoriumName, right.auditoriumName);
    if (auditoriumOrder !== 0) return auditoriumOrder;
    const leftStart = Number.isFinite(left.startMinuteOffset) ? left.startMinuteOffset : Number.MAX_SAFE_INTEGER;
    const rightStart = Number.isFinite(right.startMinuteOffset) ? right.startMinuteOffset : Number.MAX_SAFE_INTEGER;
    if (leftStart !== rightStart) return leftStart - rightStart;
    const leftRank = Number.isFinite(Number(left.rank)) ? Number(left.rank) : Number.MAX_SAFE_INTEGER;
    const rightRank = Number.isFinite(Number(right.rank)) ? Number(right.rank) : Number.MAX_SAFE_INTEGER;
    if (leftRank !== rightRank) return leftRank - rightRank;
    return collator.compare(left.id, right.id);
  })
);

export const getDefaultActiveServiceDate = candidates => {
  const knownDates = (candidates || [])
    .filter(candidate => candidate.serviceDate !== UNKNOWN_SERVICE_DATE_KEY);
  const selectedDates = knownDates
    .filter(candidate => candidate.selected)
    .map(candidate => candidate.serviceDate)
    .sort(compareServiceDateKeys);
  if (selectedDates.length > 0) return selectedDates[0];

  return knownDates.map(candidate => candidate.serviceDate).sort(compareServiceDateKeys)[0] || null;
};

export const getRelevantAuditoriums = (candidates, activeServiceDate) => {
  const auditoriums = new Map();
  (candidates || []).forEach(candidate => {
    if (candidate.serviceDate !== activeServiceDate) return;
    if (!auditoriums.has(candidate.auditoriumKey)) {
      auditoriums.set(candidate.auditoriumKey, {
        key: candidate.auditoriumKey,
        publicId: candidate.auditoriumPublicId,
        name: candidate.auditoriumName,
      });
    }
  });
  return Array.from(auditoriums.values()).sort((left, right) => collator.compare(left.name, right.name));
};

export const getPrimaryTimelineCandidates = (
  candidates,
  activeServiceDate,
  diagnosticCandidate = null,
) => {
  const primary = (candidates || []).filter(candidate => (
    candidate.selected
    && candidate.serviceDate === activeServiceDate
    && candidate.timelineEligible
  ));
  if (
    diagnosticCandidate?.timelineEligible
    && diagnosticCandidate.serviceDate === activeServiceDate
    && !primary.some(candidate => candidate.id === diagnosticCandidate.id)
  ) {
    return [...primary, { ...diagnosticCandidate, diagnostic: true }];
  }
  return primary;
};
