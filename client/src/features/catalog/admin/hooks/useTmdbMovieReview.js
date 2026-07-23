import { useCallback, useEffect, useRef, useState } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { normalizeApiError } from '@/utils/apiErrorHandler';

const HEALTH_STATUSES = new Set(['READY', 'WARNING', 'BLOCKED']);
const INVALID_REVIEW_CONTRACT = 'TMDB_REVIEW_INVALID_RESPONSE';

const REVIEW_ERROR_MESSAGES = {
  PROVIDER_UNAVAILABLE: 'Không thể kết nối tới dịch vụ dữ liệu TMDB.',
  INVALID_RESPONSE: 'Dữ liệu so sánh TMDB không hợp lệ.',
  UNKNOWN: 'Không thể tải dữ liệu so sánh TMDB.',
};

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

class TmdbReviewContractError extends Error {
  constructor() {
    super('Invalid TMDB review response contract');
    this.name = 'TmdbReviewContractError';
    this.code = INVALID_REVIEW_CONTRACT;
  }
}

const sanitizeErrorCode = value => (
  typeof value === 'string' ? value.replace(/[^A-Z0-9_.-]/gi, '').slice(0, 80) : ''
);

const normalizeReviewError = reviewError => {
  if (reviewError?.code === INVALID_REVIEW_CONTRACT) {
    return {
      type: 'INVALID_RESPONSE',
      message: REVIEW_ERROR_MESSAGES.INVALID_RESPONSE,
      technicalDetail: `Mã lỗi: ${INVALID_REVIEW_CONTRACT}`,
    };
  }

  const normalized = normalizeApiError(reviewError);
  const responseStatus = reviewError?.response?.status ?? reviewError?.data?.status;
  const errorCode = sanitizeErrorCode(reviewError?.code || normalized.code);
  const fingerprint = `${errorCode} ${normalized.message || ''}`.toLowerCase();
  const isProviderUnavailable = (
    reviewError?.code === 'ERR_NETWORK'
    || (Number.isInteger(responseStatus) && responseStatus >= 500)
    || /tmdb|provider|upstream|gateway|fetch/.test(fingerprint)
  );
  const technicalParts = [];

  if (errorCode && errorCode !== 'UNKNOWN_ERROR') technicalParts.push(`Mã lỗi: ${errorCode}`);
  if (Number.isInteger(responseStatus)) technicalParts.push(`HTTP ${responseStatus}`);

  return {
    type: isProviderUnavailable ? 'PROVIDER_UNAVAILABLE' : 'UNKNOWN',
    message: isProviderUnavailable
      ? REVIEW_ERROR_MESSAGES.PROVIDER_UNAVAILABLE
      : REVIEW_ERROR_MESSAGES.UNKNOWN,
    technicalDetail: technicalParts.join(' · '),
  };
};

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
    throw new TmdbReviewContractError();
  }
  return review;
};

export default function useTmdbMovieReview(movie) {
  const [reviewState, setReviewState] = useState({ moviePublicId: null, data: null });
  const [isLoading, setIsLoading] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [hasRequested, setHasRequested] = useState(false);
  const requestSequence = useRef(0);
  const hasLoaded = useRef(false);
  const loadedMovieId = useRef(null);

  const moviePublicId = movie?.source === 'TMDB' ? movie.publicId : null;

  const fetchReview = useCallback(async () => {
    if (!moviePublicId) {
      setReviewState({ moviePublicId: null, data: null });
      setError(null);
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
    setError(null);

    try {
      const envelope = await adminMovieService.getTmdbMovieReview(moviePublicId);
      const nextReview = unwrapTmdbReview(envelope);
      if (sequence === requestSequence.current) {
        setReviewState({ moviePublicId, data: nextReview });
        hasLoaded.current = true;
      }
    } catch (reviewError) {
      if (sequence === requestSequence.current) setError(normalizeReviewError(reviewError));
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
