// eslint-disable-next-line no-unused-vars
import { useState, useEffect, useCallback } from 'react';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';

export default function useAdminShowtimes({ triggerToast } = {}) {
  const [showtimes, setShowtimes] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  
  // Filters
  const [cinemaSlug, setCinemaSlug] = useState('');
  const [movieSlug, setMovieSlug] = useState('');
  const [date, setDate] = useState('');
  const [format, setFormat] = useState('');
  const [audioLanguage, setAudioLanguage] = useState('');
  const [subtitleLanguage, setSubtitleLanguage] = useState('');
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchShowtimes = useCallback(async () => {
    setIsLoading(true);
    try {
      const params = {
        page: currentPage,
        size: pageSize
      };
      if (cinemaSlug) params.cinemaSlug = cinemaSlug;
      if (movieSlug) params.movieSlug = movieSlug;
      if (date) params.date = date;
      if (format) params.format = format;
      if (audioLanguage) params.audioLanguage = audioLanguage;
      if (subtitleLanguage) params.subtitleLanguage = subtitleLanguage;

      const res = await adminShowtimeService.getShowtimes(params);
      if (res?.success && res?.data) {
        setShowtimes(res.data.data || []);
        setTotalPages(res.data.totalPages || 0);
        setTotalElements(res.data.totalElements || 0);
      }
    // eslint-disable-next-line no-unused-vars
    } catch (err) {
      triggerToast?.('Không thể tải danh sách suất chiếu', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [
    currentPage, pageSize, 
    cinemaSlug, movieSlug, date, 
    format, audioLanguage, subtitleLanguage, 
    triggerToast
  ]);

  return {
    showtimes,
    isLoading,
    cinemaSlug, setCinemaSlug,
    movieSlug, setMovieSlug,
    date, setDate,
    format, setFormat,
    audioLanguage, setAudioLanguage,
    subtitleLanguage, setSubtitleLanguage,
    currentPage, setCurrentPage,
    pageSize,
    totalPages,
    totalElements,
    fetchShowtimes
  };
}
