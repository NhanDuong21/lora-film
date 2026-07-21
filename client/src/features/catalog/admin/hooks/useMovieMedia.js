import { useState, useCallback, useEffect } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { parseApiError } from '@/utils/apiErrorHandler';

export default function useMovieMedia(movieId) {
  const [mediaList, setMediaList] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchMedia = useCallback(async () => {
    if (!movieId) return;
    setIsLoading(true);
    try {
      const res = await adminMovieService.getMovieMedia(movieId);
      if (res?.success && res?.data) {
        setMediaList(res.data);
        setError('');
      } else {
        setError('Không thể tải danh sách media.');
      }
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setIsLoading(false);
    }
  }, [movieId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchMedia();
  }, [fetchMedia]);

  const addMedia = async (mediaData) => {
    setIsSubmitting(true);
    try {
      const res = await adminMovieService.createMovieMedia(movieId, mediaData);
      if (res?.success) {
        await fetchMedia();
        return { success: true };
      }
      return { success: false, error: 'Tạo thất bại.' };
    } catch (err) {
      return { success: false, error: parseApiError(err) };
    } finally {
      setIsSubmitting(false);
    }
  };

  const removeMedia = async (mediaId) => {
    setIsSubmitting(true);
    try {
      const res = await adminMovieService.deleteMovieMedia(mediaId);
      if (res?.success) {
        await fetchMedia();
        return { success: true };
      }
      return { success: false, error: 'Xóa thất bại.' };
    } catch (err) {
      return { success: false, error: parseApiError(err) };
    } finally {
      setIsSubmitting(false);
    }
  };

  return { mediaList, isLoading, error, isSubmitting, reload: fetchMedia, addMedia, removeMedia };
}
