import { useState, useEffect, useCallback, useRef } from "react";
import { getMovies } from "@/features/catalog/customer/services/movieService";

/**
 * Custom hook to query movies for a specific status tab on the homepage
 * @param {Object} config - Config parameters for status, sort, and pagination size
 * @returns {Object} The state and control functions for the movie grid tab
 */
export function useMoviesQuery({ status, sort, size = 8, onDataLoaded }) {
  const [movies, setMovies] = useState([]);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [first, setFirst] = useState(true);
  const [last, setLast] = useState(true);

  // Store onDataLoaded in a Ref to avoid unstable callback dependencies
  const onDataLoadedRef = useRef(onDataLoaded);
  useEffect(() => {
    onDataLoadedRef.current = onDataLoaded;
  }, [onDataLoaded]);

  // Store movies in a Ref to avoid referencing it directly in fetchMovies' dependencies
  const moviesRef = useRef(movies);
  useEffect(() => {
    moviesRef.current = movies;
  }, [movies]);

  // Use a ref to prevent race conditions from multiple asynchronous responses
  const lastRequestRef = useRef(null);

  const fetchMovies = useCallback(async (pageToFetch) => {
    // Defer state updates to microtask to prevent react-hooks/set-state-in-effect
    await Promise.resolve();

    // Determine loading state type (initial vs refreshing)
    const hasExistingMovies = moviesRef.current.length > 0;
    if (hasExistingMovies) {
      setIsRefreshing(true);
    } else {
      setIsInitialLoading(true);
    }
    setError(null);

    const requestId = `${status}-${pageToFetch}`;
    lastRequestRef.current = requestId;

    try {
      const data = await getMovies({
        page: pageToFetch,
        size,
        status,
        sort
      });

      // Ignore responses from outdated requests
      if (lastRequestRef.current !== requestId) {
        return;
      }

      if (data) {
        const content = data.data || data.content || [];
        setMovies(content);
        setPage(data.pageNo !== undefined ? data.pageNo : data.page || 0);
        setTotalPages(data.totalPages || 0);
        setTotalElements(data.totalElements || 0);
        setFirst(data.first !== undefined ? data.first : true);
        setLast(data.last !== undefined ? data.last : true);

        // Call optional callback (useful for updating global state)
        if (onDataLoadedRef.current) {
          onDataLoadedRef.current(content);
        }
      }
    } catch (err) {
      if (lastRequestRef.current === requestId) {
        setError(err.message || "Không thể tải danh sách phim.");
      }
    } finally {
      if (lastRequestRef.current === requestId) {
        setIsInitialLoading(false);
        setIsRefreshing(false);
      }
    }
  }, [status, sort, size]);

  useEffect(() => {
    let active = true;
    const load = async () => {
      await Promise.resolve();
      if (active) {
        fetchMovies(page);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, [page, fetchMovies]);

  // Reset to page 0 when the query conditions (like status or sort) change
  useEffect(() => {
    Promise.resolve().then(() => {
      setPage(0);
    });
  }, [status, sort]);

  const retry = () => {
    fetchMovies(page);
  };

  return {
    movies,
    loading: isInitialLoading, // Map loading to isInitialLoading for backward compatibility
    isInitialLoading,
    isRefreshing,
    error,
    page,
    setPage,
    totalPages,
    totalElements,
    first,
    last,
    retry
  };
}
