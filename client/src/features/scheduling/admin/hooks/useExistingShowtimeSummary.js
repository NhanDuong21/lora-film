/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useMemo, useState } from 'react';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';

const PAGE_SIZE = 100;

const enumerateDates = (from, to) => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(from || '') || !/^\d{4}-\d{2}-\d{2}$/.test(to || '')) return [];
  const start = new Date(`${from}T00:00:00Z`);
  const end = new Date(`${to}T00:00:00Z`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start > end) return [];
  const dates = [];
  for (let cursor = start; cursor <= end && dates.length < 7; cursor = new Date(cursor.getTime() + 86400000)) {
    dates.push(cursor.toISOString().slice(0, 10));
  }
  return dates;
};

const pageRows = response => response?.data?.data || response?.data?.content || [];
const pageCount = response => Number(response?.data?.totalPages) || 0;

const isExistingOperationalShowtime = (showtime, excludeBatchId) => (
  showtime?.status !== 'CANCELLED'
  && (!excludeBatchId || showtime?.batchId !== excludeBatchId)
);

const fetchDateShowtimes = async (cinemaSlug, date) => {
  const first = await adminShowtimeService.getShowtimes({ cinemaSlug, date, page: 0, size: PAGE_SIZE });
  const totalPages = pageCount(first);
  if (totalPages <= 1) return pageRows(first);
  const remaining = await Promise.all(Array.from({ length: totalPages - 1 }, (_, index) => (
    adminShowtimeService.getShowtimes({ cinemaSlug, date, page: index + 1, size: PAGE_SIZE })
  )));
  return [first, ...remaining].flatMap(pageRows);
};

export default function useExistingShowtimeSummary({
  cinemaSlug,
  scheduleFrom,
  scheduleTo,
  excludeBatchId = '',
} = {}) {
  const dates = useMemo(
    () => enumerateDates(scheduleFrom, scheduleTo),
    [scheduleFrom, scheduleTo],
  );
  const [refreshKey, setRefreshKey] = useState(0);
  const [countsByDate, setCountsByDate] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    if (!cinemaSlug || dates.length === 0) {
      setCountsByDate({});
      setError(null);
      setIsLoading(false);
      return () => { active = false; };
    }

    setIsLoading(true);
    setError(null);
    Promise.all(dates.map(async date => {
      const showtimes = await fetchDateShowtimes(cinemaSlug, date);
      return [date, showtimes.filter(row => isExistingOperationalShowtime(row, excludeBatchId)).length];
    }))
      .then(entries => {
        if (active) setCountsByDate(Object.fromEntries(entries));
      })
      .catch(cause => {
        if (active) {
          setCountsByDate({});
          setError(cause);
        }
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => { active = false; };
  }, [cinemaSlug, dates, excludeBatchId, refreshKey]);

  return {
    countsByDate,
    totalExisting: Object.values(countsByDate).reduce((sum, count) => sum + count, 0),
    isLoading,
    error,
    retry: () => setRefreshKey(value => value + 1),
  };
}
