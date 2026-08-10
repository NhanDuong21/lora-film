import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminAutoScheduleService from '@/features/scheduling/admin/services/adminAutoScheduleService';
import { parseApiError } from '@/utils/apiErrorHandler';
import {
  AUTO_SCHEDULE_HISTORY_DEFAULTS,
  getAutoScheduleHistoryRangeError,
  parseAutoScheduleHistoryQuery,
  resetAutoScheduleHistoryFilters,
  serializeAutoScheduleHistoryQuery,
  toAutoScheduleHistoryApiParams,
} from '@/features/scheduling/admin/utils/autoScheduleHistoryQuery';

const unwrapHistoryPage = envelope => {
  const page = envelope?.data;
  if (
    envelope?.success !== true
    || !page
    || !Array.isArray(page.data)
    || !Number.isInteger(page.pageNo)
    || !Number.isInteger(page.pageSize)
    || typeof page.totalElements !== 'number'
    || !Number.isInteger(page.totalPages)
    || typeof page.last !== 'boolean'
  ) {
    throw new Error('Phản hồi lịch sử xếp lịch không đúng định dạng.');
  }
  return page;
};

const unwrapCinemaPage = envelope => {
  const page = envelope?.data;
  if (
    envelope?.success !== true
    || !page
    || !Array.isArray(page.data)
    || !Number.isInteger(page.pageNo)
    || !Number.isInteger(page.totalPages)
  ) {
    throw new Error('Phản hồi danh sách cụm rạp không đúng định dạng.');
  }
  return page;
};

export default function useAutoScheduleHistory() {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryString = searchParams.toString();
  const query = useMemo(
    () => parseAutoScheduleHistoryQuery(new URLSearchParams(queryString)),
    [queryString],
  );
  const rangeError = useMemo(() => getAutoScheduleHistoryRangeError(query), [query]);

  const [previews, setPreviews] = useState([]);
  const [cinemas, setCinemas] = useState([]);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isCinemaLoading, setIsCinemaLoading] = useState(true);
  const [error, setError] = useState('');
  const [cinemaError, setCinemaError] = useState('');
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const requestSequence = useRef(0);
  const hasLoaded = useRef(false);

  const canonicalQueryString = useMemo(
    () => serializeAutoScheduleHistoryQuery(query).toString(),
    [query],
  );

  useEffect(() => {
    if (canonicalQueryString !== queryString) {
      setSearchParams(canonicalQueryString, { replace: true });
    }
  }, [canonicalQueryString, queryString, setSearchParams]);

  const commitQuery = useCallback((changes, options = {}) => {
    const { replace = false, resetPage = true } = options;
    const resolved = typeof changes === 'function' ? changes(query) : changes;
    const nextQuery = {
      ...query,
      ...resolved,
      page: resetPage ? 0 : (resolved.page ?? query.page),
    };
    setSearchParams(serializeAutoScheduleHistoryQuery(nextQuery), { replace });
  }, [query, setSearchParams]);

  const fetchHistory = useCallback(async () => {
    const requestId = ++requestSequence.current;
    if (rangeError) {
      setError('');
      setIsInitialLoading(false);
      setIsRefreshing(false);
      return;
    }

    if (hasLoaded.current) setIsRefreshing(true);
    else setIsInitialLoading(true);
    setError('');

    try {
      const envelope = await adminAutoScheduleService.getPreviewHistory(
        toAutoScheduleHistoryApiParams(query),
      );
      const page = unwrapHistoryPage(envelope);
      if (requestId !== requestSequence.current) return;

      if (page.totalPages > 0 && query.page >= page.totalPages) {
        commitQuery({ page: page.totalPages - 1 }, { replace: true, resetPage: false });
        return;
      }

      setPreviews(page.data);
      setTotalElements(page.totalElements);
      setTotalPages(page.totalPages);
      hasLoaded.current = true;
    } catch (err) {
      if (requestId !== requestSequence.current) return;
      setError(parseApiError(err));
    } finally {
      if (requestId === requestSequence.current) {
        setIsInitialLoading(false);
        setIsRefreshing(false);
      }
    }
  }, [commitQuery, query, rangeError]);

  const fetchCinemas = useCallback(async () => {
    setIsCinemaLoading(true);
    setCinemaError('');
    try {
      const allCinemas = [];
      let pageNumber = 0;
      let totalPageCount = 1;
      while (pageNumber < totalPageCount) {
        const envelope = await adminCinemaService.getCinemas({
          showDeleted: true,
          page: pageNumber,
          size: 100,
          sort: 'name,asc',
        });
        const page = unwrapCinemaPage(envelope);
        allCinemas.push(...page.data);
        totalPageCount = page.totalPages;
        pageNumber += 1;
      }
      setCinemas(allCinemas);
    } catch (err) {
      setCinemaError(parseApiError(err));
    } finally {
      setIsCinemaLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchHistory();
  }, [fetchHistory]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchCinemas();
  }, [fetchCinemas]);

  const resetFilters = useCallback(() => {
    setSearchParams(serializeAutoScheduleHistoryQuery(resetAutoScheduleHistoryFilters(query)));
  }, [query, setSearchParams]);

  return {
    previews,
    cinemas,
    query,
    rangeError,
    isInitialLoading,
    isRefreshing,
    isCinemaLoading,
    error,
    cinemaError,
    totalElements,
    totalPages,
    commitQuery,
    resetFilters,
    fetchHistory,
    fetchCinemas,
    defaults: AUTO_SCHEDULE_HISTORY_DEFAULTS,
  };
}

export { unwrapHistoryPage, unwrapCinemaPage };
