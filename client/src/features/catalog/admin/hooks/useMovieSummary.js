import { useCallback, useEffect, useRef, useState } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { parseApiError } from '@/utils/apiErrorHandler';

const SUMMARY_FIELDS = [
  'total', 'draft', 'upcoming', 'nowShowing', 'ended', 'inactive',
  'ready', 'warning', 'blocked', 'missingPrimaryPoster',
  'missingActiveVersion', 'withoutShowtime'
];

const unwrapSummary = envelope => {
  const summary = envelope?.data;
  if (envelope?.success !== true || !summary || SUMMARY_FIELDS.some(field => typeof summary[field] !== 'number')) {
    throw new Error('Phản hồi thống kê phim không đúng định dạng.');
  }
  return summary;
};

export default function useMovieSummary() {
  const [summary, setSummary] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');
  const requestSequence = useRef(0);
  const hasLoaded = useRef(false);

  const fetchSummary = useCallback(async () => {
    const requestId = ++requestSequence.current;
    if (hasLoaded.current) setIsRefreshing(true);
    else setIsLoading(true);
    setError('');

    try {
      const envelope = await adminMovieService.getMovieSummary();
      const nextSummary = unwrapSummary(envelope);
      if (requestId !== requestSequence.current) return;
      setSummary(nextSummary);
      hasLoaded.current = true;
    } catch (err) {
      if (requestId !== requestSequence.current) return;
      setError(parseApiError(err));
    } finally {
      if (requestId === requestSequence.current) {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchSummary();
  }, [fetchSummary]);

  return { summary, isLoading, isRefreshing, error, fetchSummary };
}
