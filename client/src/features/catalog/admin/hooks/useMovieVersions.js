import { useState, useCallback, useEffect } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { parseApiError } from '@/utils/apiErrorHandler';

export default function useMovieVersions(movieId) {
  const [versions, setVersions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchVersions = useCallback(async () => {
    if (!movieId) return;
    setIsLoading(true);
    try {
      const res = await adminMovieService.getMovieVersions(movieId);
      if (res?.success && res?.data) {
        setVersions(res.data);
        setError('');
      } else {
        setError('Không thể tải danh sách phiên bản.');
      }
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setIsLoading(false);
    }
  }, [movieId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchVersions();
  }, [fetchVersions]);

  const addVersion = async (versionData) => {
    setIsSubmitting(true);
    try {
      const res = await adminMovieService.createMovieVersion(movieId, versionData);
      if (res?.success) {
        await fetchVersions();
        return { success: true };
      }
      return { success: false, error: 'Tạo thất bại.' };
    } catch (err) {
      return { success: false, error: parseApiError(err) };
    } finally {
      setIsSubmitting(false);
    }
  };

  const removeVersion = async (versionId) => {
    setIsSubmitting(true);
    try {
      const res = await adminMovieService.deleteMovieVersion(versionId);
      if (res?.success) {
        await fetchVersions();
        return { success: true };
      }
      return { success: false, error: 'Xóa thất bại.' };
    } catch (err) {
      return { success: false, error: parseApiError(err) };
    } finally {
      setIsSubmitting(false);
    }
  };

  return { versions, isLoading, error, isSubmitting, reload: fetchVersions, addVersion, removeVersion };
}
