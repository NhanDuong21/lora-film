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
