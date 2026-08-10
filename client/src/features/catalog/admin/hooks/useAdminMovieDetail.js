import { useState, useCallback, useEffect } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { parseApiError } from '@/utils/apiErrorHandler';

export default function useAdminMovieDetail(moviePublicId) {
  const [movie, setMovie] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchMovie = useCallback(async () => {
    if (!moviePublicId) return;
    setIsLoading(true);
    setError('');
    try {
      const res = await adminMovieService.getMovieById(moviePublicId);
      if (res?.success && res?.data) {
        setMovie(res.data);
      } else {
        setError('Không thể tải chi tiết phim.');
      }
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setIsLoading(false);
    }
  }, [moviePublicId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchMovie();
  }, [fetchMovie]);

  return { movie, isLoading, error, reload: fetchMovie };
}
