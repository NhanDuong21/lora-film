import { useCallback, useEffect, useRef, useState } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { parseApiError } from '@/utils/apiErrorHandler';

export default function useTmdbMovieReview(movie) {
  const [review, setReview] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');
  const requestSequence = useRef(0);
  const hasLoaded = useRef(false);
  const loadedMovieId = useRef(null);

  const moviePublicId = movie?.source === 'TMDB' ? movie.publicId : null;

  const fetchReview = useCallback(async () => {
    if (!moviePublicId) {
      setReview(null);
      setError('');
      hasLoaded.current = false;
      loadedMovieId.current = null;
      return;
    }

    if (loadedMovieId.current !== moviePublicId) {
      setReview(null);
      hasLoaded.current = false;
      loadedMovieId.current = moviePublicId;
    }

    const sequence = ++requestSequence.current;
    if (hasLoaded.current) setIsRefreshing(true);
    else setIsLoading(true);
    setError('');

    try {
      const envelope = await adminMovieService.getTmdbMovieReview(moviePublicId);
      if (envelope?.success !== true || !envelope?.data) {
        throw new Error('Invalid TMDB review response');
      }
      if (sequence === requestSequence.current) {
        setReview(envelope.data);
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

  return { review, isLoading, isRefreshing, error, reload: fetchReview };
}
