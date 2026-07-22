import { useCallback, useEffect, useRef, useState } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { parseApiError } from '@/utils/apiErrorHandler';

const HEALTH_STATUSES = new Set(['READY', 'WARNING', 'BLOCKED']);

const isStringList = value => Array.isArray(value) && value.every(item => typeof item === 'string');
const isIssueList = value => Array.isArray(value) && value.every(item => (
  item && typeof item === 'object' && typeof item.code === 'string' && typeof item.message === 'string'
));
const isNullableString = value => value === null || typeof value === 'string';

const isScalarDiff = diff => (
  diff
  && typeof diff === 'object'
  && typeof diff.field === 'string'
  && typeof diff.label === 'string'
  && isNullableString(diff.currentValue)
  && isNullableString(diff.providerValue)
  && typeof diff.changed === 'boolean'
);

const isCollectionDiff = diff => (
  diff
  && typeof diff === 'object'
  && typeof diff.field === 'string'
  && typeof diff.label === 'string'
  && isStringList(diff.currentValues)
  && isStringList(diff.providerValues)
  && isStringList(diff.added)
  && isStringList(diff.removed)
  && typeof diff.changed === 'boolean'
);

const unwrapTmdbReview = envelope => {
  const review = envelope?.data;
  const readiness = review?.readiness;
  if (
    envelope?.success !== true
    || !review
    || review.source !== 'TMDB'
    || !Number.isInteger(review.tmdbId)
    || typeof review.reviewStatus !== 'string'
    || typeof review.canApprove !== 'boolean'
    || !isStringList(review.approvalBlockers)
    || !readiness
    || !HEALTH_STATUSES.has(readiness.healthStatus)
    || !isIssueList(readiness.blockers)
    || !isIssueList(readiness.warnings)
    || typeof review.hasProviderChanges !== 'boolean'
    || !Array.isArray(review.scalarDiffs)
    || !review.scalarDiffs.every(isScalarDiff)
    || !Array.isArray(review.collectionDiffs)
    || !review.collectionDiffs.every(isCollectionDiff)
  ) {
    throw new Error('Phản hồi TMDB review không đúng định dạng.');
  }
  return review;
};

export default function useTmdbMovieReview(movie) {
  const [reviewState, setReviewState] = useState({ moviePublicId: null, data: null });
  const [isLoading, setIsLoading] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [hasRequested, setHasRequested] = useState(false);
  const requestSequence = useRef(0);
  const hasLoaded = useRef(false);
  const loadedMovieId = useRef(null);

  const moviePublicId = movie?.source === 'TMDB' ? movie.publicId : null;

  const fetchReview = useCallback(async () => {
    if (!moviePublicId) {
      setReviewState({ moviePublicId: null, data: null });
      setError('');
      setHasRequested(false);
      hasLoaded.current = false;
      loadedMovieId.current = null;
      return;
    }

    if (loadedMovieId.current !== moviePublicId) {
      setReviewState({ moviePublicId, data: null });
      hasLoaded.current = false;
      loadedMovieId.current = moviePublicId;
    }

    const sequence = ++requestSequence.current;
    setHasRequested(true);
    if (hasLoaded.current) setIsRefreshing(true);
    else setIsLoading(true);
    setError('');

    try {
      const envelope = await adminMovieService.getTmdbMovieReview(moviePublicId);
      const nextReview = unwrapTmdbReview(envelope);
      if (sequence === requestSequence.current) {
        setReviewState({ moviePublicId, data: nextReview });
        hasLoaded.current = true;
      }
    } catch (reviewError) {
      if (sequence === requestSequence.current) setError(parseApiError(reviewError));
    } finally {
      if (sequence === requestSequence.current) {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    }
  }, [moviePublicId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchReview();
    return () => { requestSequence.current += 1; };
  }, [fetchReview]);

  const visibleReview = reviewState.moviePublicId === moviePublicId ? reviewState.data : null;
  return { review: visibleReview, isLoading, isRefreshing, hasRequested, error, reload: fetchReview };
}
