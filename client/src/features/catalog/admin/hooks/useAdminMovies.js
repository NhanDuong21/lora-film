import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminGenreService from '@/features/catalog/admin/services/adminGenreService';
import { parseApiError } from '@/utils/apiErrorHandler';
import {
  ADMIN_MOVIE_QUERY_DEFAULTS,
  clearAdvancedMovieFilters,
  parseAdminMovieQuery,
  serializeAdminMovieQuery,
  toMovieApiParams,
} from '@/features/catalog/admin/utils/adminMovieQuery';

const unwrapMoviePage = envelope => {
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
    throw new Error('Phản hồi danh sách phim không đúng định dạng.');
  }
  return page;
};

const unwrapGenres = envelope => {
  const genres = envelope?.data?.content;
  if (envelope?.success !== true || !Array.isArray(genres)) {
    throw new Error('Phản hồi thể loại không đúng định dạng.');
  }
  return genres;
};

export default function useAdminMovies({ triggerConfirm, triggerToast, onMutation } = {}) {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryString = searchParams.toString();
  const query = useMemo(
    () => parseAdminMovieQuery(new URLSearchParams(queryString)),
    [queryString]
  );

  const [movies, setMovies] = useState([]);
  const [genresList, setGenresList] = useState([]);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchInput, setSearchInput] = useState(query.keyword);
  const [bulkApproval, setBulkApproval] = useState({
    isPending: false,
    result: null,
    error: '',
  });
  const [bulkArchive, setBulkArchive] = useState({
    isPending: false,
    result: null,
    error: '',
  });
  const requestSequence = useRef(0);
  const hasLoaded = useRef(false);

  const canonicalQueryString = useMemo(
    () => serializeAdminMovieQuery(query).toString(),
    [query]
  );

  useEffect(() => {
    if (canonicalQueryString !== queryString) {
      setSearchParams(canonicalQueryString, { replace: true });
    }
  }, [canonicalQueryString, queryString, setSearchParams]);

  const commitQuery = useCallback((changes, options = {}) => {
    const { replace = false, resetPage = true } = options;
    const resolvedChanges = typeof changes === 'function' ? changes(query) : changes;
    const nextQuery = {
      ...query,
      ...resolvedChanges,
      page: resetPage ? 0 : (resolvedChanges.page ?? query.page),
    };
    setSearchParams(serializeAdminMovieQuery(nextQuery), { replace });
  }, [query, setSearchParams]);

  useEffect(() => {
    // URL navigation is authoritative for the transient search box.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setSearchInput(query.keyword);
  }, [query.keyword]);

  useEffect(() => {
    const keyword = searchInput.trim();
    if (keyword === query.keyword) return undefined;
    const timer = window.setTimeout(() => {
      commitQuery({ keyword }, { replace: true, resetPage: true });
    }, 300);
    return () => window.clearTimeout(timer);
  }, [commitQuery, query.keyword, searchInput]);

  const fetchGenres = useCallback(async () => {
    try {
      const envelope = await adminGenreService.getAllGenres();
      setGenresList(unwrapGenres(envelope));
    } catch (err) {
      triggerToast?.(parseApiError(err), 'error');
    }
  }, [triggerToast]);

  const fetchMovies = useCallback(async () => {
    const requestId = ++requestSequence.current;
    if (hasLoaded.current) setIsRefreshing(true);
    else setIsInitialLoading(true);
    setError('');

    try {
      const envelope = await adminMovieService.getMovies(toMovieApiParams(query));
      const page = unwrapMoviePage(envelope);
      if (requestId !== requestSequence.current) return;

      if (page.totalPages > 0 && query.page >= page.totalPages) {
        commitQuery({ page: page.totalPages - 1 }, { replace: true, resetPage: false });
        return;
      }
      setMovies(page.data);
      setTotalElements(page.totalElements);
      setTotalPages(page.totalPages);
      hasLoaded.current = true;
    } catch (err) {
      if (requestId !== requestSequence.current) return;
      const message = parseApiError(err);
      setError(message);
      triggerToast?.(message, 'error');
    } finally {
      if (requestId === requestSequence.current) {
        setIsInitialLoading(false);
        setIsRefreshing(false);
      }
    }
  }, [commitQuery, query, triggerToast]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchGenres();
  }, [fetchGenres]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchMovies();
  }, [fetchMovies]);

  const refreshAll = useCallback(async () => {
    await Promise.all([fetchMovies(), onMutation?.()]);
  }, [fetchMovies, onMutation]);

  const bulkApproveTmdbMovies = useCallback(async (limit = 100) => {
    setBulkApproval({ isPending: true, result: null, error: '' });
    try {
      const envelope = await adminMovieService.bulkApproveTmdbMovies(toMovieApiParams(query), limit);
      if (envelope?.success !== true || !envelope.data || !Array.isArray(envelope.data.results)) {
        throw new Error('Phản hồi duyệt hàng loạt không đúng định dạng.');
      }
      setBulkApproval({ isPending: false, result: envelope.data, error: '' });
      await refreshAll();
      return envelope.data;
    } catch (err) {
      const message = parseApiError(err);
      setBulkApproval({ isPending: false, result: null, error: message });
      triggerToast?.(message, 'error');
      return null;
    }
  }, [query, refreshAll, triggerToast]);

  const bulkArchiveOldTmdbMovies = useCallback(async (limit = 100) => {
    setBulkArchive({ isPending: true, result: null, error: '' });
    try {
      const envelope = await adminMovieService.bulkArchiveOldTmdbMovies(toMovieApiParams(query), limit);
      if (envelope?.success !== true || !envelope.data || !Array.isArray(envelope.data.results)) {
        throw new Error('Phản hồi lưu trữ hàng loạt không đúng định dạng.');
      }
      setBulkArchive({ isPending: false, result: envelope.data, error: '' });
      await refreshAll();
      return envelope.data;
    } catch (err) {
      const message = parseApiError(err);
      setBulkArchive({ isPending: false, result: null, error: message });
      triggerToast?.(message, 'error');
      return null;
    }
  }, [query, refreshAll, triggerToast]);

  const handleDelete = async (publicId, title) => {
    const shouldDelete = triggerConfirm
      ? await triggerConfirm(`Bạn có chắc chắn muốn xóa phim "${title}"?`)
      : window.confirm(`Bạn có chắc chắn muốn xóa phim "${title}"?`);

    if (!shouldDelete) return;
    try {
      await adminMovieService.deleteMovie(publicId);
      triggerToast?.('Đã xóa phim thành công!');
      await refreshAll();
    } catch (err) {
      triggerToast?.(parseApiError(err), 'error');
    }
  };

  const clearAdvancedFilters = useCallback(() => {
    setSearchParams(serializeAdminMovieQuery(clearAdvancedMovieFilters(query)));
  }, [query, setSearchParams]);

  return {
    movies,
    genresList,
    setGenresList,
    query,
    searchInput,
    setSearchInput,
    isInitialLoading,
    isRefreshing,
    error,
    totalElements,
    totalPages,
    commitQuery,
    clearAdvancedFilters,
    fetchMovies,
    refreshAll,
    bulkApproval,
    bulkApproveTmdbMovies,
    bulkArchive,
    bulkArchiveOldTmdbMovies,
    handleDelete,
    defaults: ADMIN_MOVIE_QUERY_DEFAULTS,
  };
}
