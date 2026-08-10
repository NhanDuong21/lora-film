import { useState, useEffect, useCallback, useRef } from 'react';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { getUserAccountId } from '@/utils/authStorage';
import {
  buildShowtimeQueryCacheKey,
  readShowtimeQueryCache,
  runShowtimeQueryOnce,
  writeShowtimeQueryCache,
} from '@/features/scheduling/admin/utils/showtimeQueryCache';

export default function useAdminShowtimes({ triggerToast, initialFilters } = {}) {
  const [showtimes, setShowtimes] = useState([]);
  const [cinemas, setCinemas] = useState([]);
  const [movies, setMovies] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isOptionsLoading, setIsOptionsLoading] = useState(false);
  
  // Filters
  const [cinemaSlug, setCinemaSlug] = useState(initialFilters?.cinemaSlug || '');
  const [movieSlug, setMovieSlug] = useState('');
  const [date, setDate] = useState(initialFilters?.date || '');
  const [status, setStatus] = useState(initialFilters?.status || '');
  const [format, setFormat] = useState('');
  const [audioLanguage, setAudioLanguage] = useState('');
  const [subtitleLanguage, setSubtitleLanguage] = useState('');
  const [batchId, setBatchId] = useState(initialFilters?.batchId || '');
  const [source, setSource] = useState(initialFilters?.source || '');
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(100);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const requestGenerationRef = useRef(0);

  // Fetch initial options
  useEffect(() => {
    const fetchOptions = async () => {
      setIsOptionsLoading(true);
      try {
        const [cinemaRes, movieRes] = await Promise.all([
          adminCinemaService.getCinemas({ size: 100, status: 'ACTIVE' }),
          adminMovieService.getMovies({ size: 100 })
        ]);
        if (cinemaRes?.success) setCinemas(cinemaRes.data?.data || []);
        if (movieRes?.success) setMovies(movieRes.data?.data || []);
      } catch {
        // ignore
      } finally {
        setIsOptionsLoading(false);
      }
    };
    fetchOptions();
  }, []);

  const fetchShowtimes = useCallback(async ({ force = false } = {}) => {
    const requestGeneration = ++requestGenerationRef.current;
    const params = {
      page: currentPage,
      size: pageSize
    };
    if (cinemaSlug?.trim()) params.cinemaSlug = cinemaSlug.trim();
    if (movieSlug?.trim()) params.movieSlug = movieSlug.trim();
    if (date?.trim()) params.date = date.trim();
    if (status?.trim() && status !== 'ALL') params.status = status.trim();
    if (format?.trim()) params.format = format.trim();
    if (audioLanguage?.trim()) params.audioLanguage = audioLanguage.trim();
    if (subtitleLanguage?.trim()) params.subtitleLanguage = subtitleLanguage.trim();
    if (batchId?.trim()) params.batchId = batchId.trim();
    if (source?.trim() && source !== 'ALL') params.source = source.trim();

    const accountScope = getUserAccountId() || 'current-session';
    const cacheKey = buildShowtimeQueryCacheKey(`admin-showtimes:${accountScope}`, params);
    const cached = readShowtimeQueryCache(cacheKey);
    const applyResponse = response => {
      setShowtimes(response?.data || []);
      setTotalPages(response?.totalPages || 0);
      setTotalElements(response?.totalElements || 0);
    };

    if (cached && !force && cached.isFresh) {
      applyResponse(cached.response);
      setIsLoading(false);
      setIsRefreshing(false);
      return cached.response;
    }

    if (cached) {
      applyResponse(cached.response);
      setIsLoading(false);
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
      setIsRefreshing(false);
    }

    try {
      const res = await runShowtimeQueryOnce(
        cacheKey,
        () => adminShowtimeService.getShowtimes(params),
      );
      if (res?.success && res?.data) writeShowtimeQueryCache(cacheKey, res.data);
      if (requestGeneration === requestGenerationRef.current && res?.success && res?.data) {
        applyResponse(res.data);
      }
      return res?.data || null;
    // eslint-disable-next-line no-unused-vars
    } catch (err) {
      if (requestGeneration === requestGenerationRef.current) {
        triggerToast?.('Không thể tải danh sách suất chiếu', 'error');
      }
    } finally {
      if (requestGeneration === requestGenerationRef.current) {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    }
  }, [
    currentPage, pageSize, 
    cinemaSlug, movieSlug, date, status,
    format, audioLanguage, subtitleLanguage,
    batchId, source,
    triggerToast
  ]);

  return {
    showtimes,
    cinemas,
    movies,
    isLoading,
    isRefreshing,
    isOptionsLoading,
    cinemaSlug, setCinemaSlug,
    movieSlug, setMovieSlug,
    date, setDate,
    status, setStatus,
    format, setFormat,
    audioLanguage, setAudioLanguage,
    subtitleLanguage, setSubtitleLanguage,
    batchId, setBatchId,
    source, setSource,
    currentPage, setCurrentPage,
    pageSize, setPageSize,
    totalPages,
    totalElements,
    fetchShowtimes
  };
}
