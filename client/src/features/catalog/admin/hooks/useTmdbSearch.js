import { useState, useEffect } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';

export default function useTmdbSearch() {
  const [tmdbSearch, setTmdbSearch] = useState('');
  const [tmdbSuggestions, setTmdbSuggestions] = useState([]);
  const [isTmdbSearching, setIsTmdbSearching] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);

  const [tmdbLatestMovies, setTmdbLatestMovies] = useState([]);
  const [latestMoviesPageIndex, setLatestMoviesPageIndex] = useState(0);
  const [isLatestMoviesLoading, setIsLatestMoviesLoading] = useState(false);
  const [slideOffset, setSlideOffset] = useState(-14.2857);
  const [isTransitioning, setIsTransitioning] = useState(true);
  const [slideLock, setSlideLock] = useState(false);

  useEffect(() => {
    if (!tmdbSearch.trim()) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setTmdbSuggestions([]);
      return;
    }

    const abortController = new AbortController();

    const t = setTimeout(async () => {
      setIsTmdbSearching(true);
      try {
        const res = await adminMovieService.searchTmdbSuggestions(tmdbSearch, abortController.signal);
        setTmdbSuggestions(Array.isArray(res?.data) ? res.data : []);
      } catch (err) {
        if (err?.name !== 'CanceledError' && err?.message !== 'canceled') {
          setTmdbSuggestions([]);
        }
      } finally {
        setIsTmdbSearching(false);
      }
    }, 400);

    return () => {
      clearTimeout(t);
      abortController.abort();
    };
  }, [tmdbSearch]);

  useEffect(() => {
    const fetchLatestMovies = async () => {
      setIsLatestMoviesLoading(true);
      try {
        const res = await adminMovieService.getLatestTop20(20);
        if (res?.success && Array.isArray(res.data)) {
          setTmdbLatestMovies(res.data);
        }
      } catch (err) {
        console.error("Failed to fetch TMDB latest movies:", err);
      } finally {
        setIsLatestMoviesLoading(false);
      }
    };
    fetchLatestMovies();
  }, []);

  const slideRight = () => {
    if (slideLock || tmdbLatestMovies.length === 0) return;
    setSlideLock(true);
    setSlideOffset(-28.5714);
    setTimeout(() => {
      setLatestMoviesPageIndex(prev => (prev + 1) % tmdbLatestMovies.length);
      setIsTransitioning(false);
      setSlideOffset(-14.2857);
      setTimeout(() => {
        setIsTransitioning(true);
        setSlideLock(false);
      }, 30);
    }, 300);
  };

  const slideLeft = () => {
    if (slideLock || tmdbLatestMovies.length === 0) return;
    setSlideLock(true);
    setSlideOffset(0);
    setTimeout(() => {
      setLatestMoviesPageIndex(prev => (prev - 1 + tmdbLatestMovies.length) % tmdbLatestMovies.length);
      setIsTransitioning(false);
      setSlideOffset(-14.2857);
      setTimeout(() => {
        setIsTransitioning(true);
        setSlideLock(false);
      }, 30);
    }, 300);
  };

  return {
    tmdbSearch,
    setTmdbSearch,
    tmdbSuggestions,
    setTmdbSuggestions,
    isTmdbSearching,
    showSuggestions,
    setShowSuggestions,
    tmdbLatestMovies,
    latestMoviesPageIndex,
    setLatestMoviesPageIndex,
    isLatestMoviesLoading,
    slideOffset,
    isTransitioning,
    slideLock,
    slideLeft,
    slideRight,
  };
}
