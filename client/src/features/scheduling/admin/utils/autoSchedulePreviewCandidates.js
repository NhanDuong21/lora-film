export const CANDIDATE_VIEWS = Object.freeze({
  RECOMMENDED: 'RECOMMENDED',
  UNSELECTED_VALID: 'UNSELECTED_VALID',
  ISSUES: 'ISSUES',
  ALL: 'ALL',
  CREATED: 'CREATED',
});

export const CANDIDATE_PAGE_SIZES = Object.freeze([25, 50, 100]);
export const DEFAULT_CANDIDATE_PAGE_SIZE = 50;
export const MAX_RENDERED_CANDIDATE_ROWS = 100;

export const getDefaultCandidateView = capabilities => {
  if (capabilities?.effectiveStatus === 'APPLIED') return CANDIDATE_VIEWS.CREATED;
  if (capabilities?.isEditable) return CANDIDATE_VIEWS.RECOMMENDED;
  return CANDIDATE_VIEWS.ALL;
};

export const getCandidateViewCounts = (items, selectedItemIds) => {
  const metrics = getCandidateMetrics(items, selectedItemIds);

  return {
    [CANDIDATE_VIEWS.RECOMMENDED]: metrics.selectedRecommendations,
    [CANDIDATE_VIEWS.UNSELECTED_VALID]: metrics.validUnselected,
    [CANDIDATE_VIEWS.ISSUES]: metrics.issueCandidates,
    [CANDIDATE_VIEWS.ALL]: metrics.totalGenerated,
    [CANDIDATE_VIEWS.CREATED]: metrics.createdShowtimes,
  };
};

export const getCandidateMetrics = (items, selectedItemIds) => {
  const selectedIds = selectedItemIds instanceof Set
    ? selectedItemIds
    : new Set(selectedItemIds || []);
  const source = items || [];
  const isSelected = item => selectedIds.has(item.itemPublicId);
  const isApplyIssue = item => item.applyStatus === 'CONFLICT' || item.applyStatus === 'FAILED';

  return {
    totalGenerated: source.length,
    selectedRecommendations: source.filter(isSelected).length,
    validUnselected: source.filter(item => item.validationStatus === 'VALID' && !isSelected(item)).length,
    rejectedCandidates: source.filter(item => item.validationStatus === 'REJECTED').length,
    applyConflictsFailures: source.filter(isApplyIssue).length,
    issueCandidates: source.filter(item => item.validationStatus !== 'VALID' || isApplyIssue(item)).length,
    createdShowtimes: source.filter(item => item.applyStatus === 'CREATED').length,
    skippedCandidates: source.filter(item => item.applyStatus === 'SKIPPED').length,
  };
};

export const getMovieDistribution = candidates => {
  const groups = new Map();

  (candidates || []).forEach(candidate => {
    const movieKey = candidate.movieKey
      || candidate.moviePublicId
      || candidate.movieVersionPublicId
      || candidate.movieTitle
      || 'unknown-movie';
    if (!groups.has(movieKey)) {
      groups.set(movieKey, {
        movieKey,
        movieTitle: candidate.movieTitle || 'Phim không xác định',
        palette: candidate.palette || null,
        generatedCount: 0,
        validCount: 0,
        selectedCount: 0,
        createdCount: 0,
      });
    }
    const group = groups.get(movieKey);
    group.generatedCount += 1;
    if (candidate.validationStatus === 'VALID') group.validCount += 1;
    if (candidate.selected) group.selectedCount += 1;
    if (candidate.applyStatus === 'CREATED') group.createdCount += 1;
  });

  const rows = Array.from(groups.values()).map(group => ({
    ...group,
    scheduledCount: group.createdCount > 0 ? group.createdCount : group.selectedCount,
  }));
  const totalScheduled = rows.reduce((sum, row) => sum + row.scheduledCount, 0);

  return rows
    .map(row => ({
      ...row,
      sharePercent: totalScheduled > 0
        ? Math.round((row.scheduledCount / totalScheduled) * 1000) / 10
        : 0,
      hasCoverageGap: row.validCount > 0 && row.scheduledCount === 0,
    }))
    .sort((left, right) => (
      right.scheduledCount - left.scheduledCount
      || right.validCount - left.validCount
      || left.movieTitle.localeCompare(right.movieTitle, 'vi')
    ));
};

export const getMovieDistributionSummary = distribution => {
  const rows = distribution || [];
  const eligibleMovieCount = rows.filter(row => row.validCount > 0).length;
  const representedMovieCount = rows.filter(row => row.scheduledCount > 0).length;
  const dominant = rows.find(row => row.scheduledCount > 0) || null;
  const uncoveredMovies = rows.filter(row => row.hasCoverageGap);

  return {
    eligibleMovieCount,
    representedMovieCount,
    uncoveredMovies,
    dominantMovieTitle: dominant?.movieTitle || null,
    dominantSharePercent: dominant?.sharePercent || 0,
    hasCoverageGap: uncoveredMovies.length > 0,
    isHighlyConcentrated: eligibleMovieCount > 1 && (dominant?.sharePercent || 0) > 60,
  };
};

export const filterCandidatesByView = (items, view, selectedItemIds) => {
  const source = items || [];
  const selectedIds = selectedItemIds instanceof Set
    ? selectedItemIds
    : new Set(selectedItemIds || []);

  switch (view) {
    case CANDIDATE_VIEWS.RECOMMENDED:
      return source.filter(item => selectedIds.has(item.itemPublicId));
    case CANDIDATE_VIEWS.UNSELECTED_VALID:
      return source.filter(item => (
        item.validationStatus === 'VALID'
        && !selectedIds.has(item.itemPublicId)
      ));
    case CANDIDATE_VIEWS.ISSUES:
      return source.filter(item => (
        item.validationStatus !== 'VALID'
        || item.applyStatus === 'CONFLICT'
        || item.applyStatus === 'FAILED'
      ));
    case CANDIDATE_VIEWS.CREATED:
      return source.filter(item => item.applyStatus === 'CREATED');
    case CANDIDATE_VIEWS.ALL:
    default:
      return source;
  }
};

export const normalizeCandidatePageSize = value => {
  const parsed = Number(value);
  return CANDIDATE_PAGE_SIZES.includes(parsed) ? parsed : DEFAULT_CANDIDATE_PAGE_SIZE;
};

export const paginateCandidates = (items, requestedPage, requestedPageSize) => {
  const source = items || [];
  const pageSize = Math.min(
    normalizeCandidatePageSize(requestedPageSize),
    MAX_RENDERED_CANDIDATE_ROWS,
  );
  const totalPages = Math.max(1, Math.ceil(source.length / pageSize));
  const page = Math.min(Math.max(Number(requestedPage) || 1, 1), totalPages);
  const start = (page - 1) * pageSize;

  return {
    page,
    pageSize,
    totalPages,
    totalItems: source.length,
    items: source.slice(start, start + pageSize),
  };
};
