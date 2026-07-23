const KNOWN_HEALTH_STATUSES = new Set(['READY', 'WARNING', 'BLOCKED']);

const BLOCKER_CODES = Object.freeze({
  GENRE: 'NO_GENRE',
  ACTIVE_VERSION: 'NO_ACTIVE_VERSION',
  PRIMARY_POSTER: 'NO_ACTIVE_PRIMARY_POSTER'
});

const normalizeIssues = (issues) => (
  Array.isArray(issues)
    ? issues.filter(issue => issue && typeof issue === 'object')
    : []
);

/**
 * Normalizes the backend readiness contract for rendering only.
 * It intentionally does not infer readiness from movie fields or legacy classification.
 */
export function getMovieReadinessView(movie) {
  const readiness = movie?.readiness;
  const healthStatus = KNOWN_HEALTH_STATUSES.has(readiness?.healthStatus)
    ? readiness.healthStatus
    : 'UNKNOWN';
  const blockers = normalizeIssues(readiness?.blockers);
  const warnings = normalizeIssues(readiness?.warnings);

  return {
    healthStatus,
    classification: readiness?.classification,
    blockers,
    warnings,
    issues: [
      ...blockers.map(issue => ({ ...issue, severity: 'BLOCKER' })),
      ...warnings.map(issue => ({ ...issue, severity: 'WARNING' }))
    ]
  };
}

/**
 * Adapts canonical backend blocker codes to the existing checklist presentation.
 * Missing readiness remains UNKNOWN instead of re-running business rules in React.
 */
export function getPublishChecklist(readinessView) {
  if (readinessView.healthStatus === 'UNKNOWN') {
    return {
      hasGenre: 'UNKNOWN',
      hasActiveVersion: 'UNKNOWN',
      hasPrimaryPoster: 'UNKNOWN',
      isReady: false
    };
  }

  const blockerCodes = new Set(readinessView.blockers.map(issue => issue.code));

  return {
    hasGenre: blockerCodes.has(BLOCKER_CODES.GENRE) ? 'MISSING' : 'PASS',
    hasActiveVersion: blockerCodes.has(BLOCKER_CODES.ACTIVE_VERSION) ? 'MISSING' : 'PASS',
    hasPrimaryPoster: blockerCodes.has(BLOCKER_CODES.PRIMARY_POSTER) ? 'MISSING' : 'PASS',
    isReady: readinessView.healthStatus !== 'BLOCKED'
  };
}
