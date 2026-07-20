import { useState, useEffect, useCallback } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminGenreService from '@/features/catalog/admin/services/adminGenreService';
import { parseApiError } from '@/utils/apiErrorHandler';
import { normalizePagination } from '@/utils/pagination';

export default function useAdminMovies({ triggerConfirm, triggerToast } = {}) {
  const [movies, setMovies] = useState([]);
  const [genresList, setGenresList] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState('DRAFT');
  const [searchTerm, setSearchTerm] = useState('');
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchGenres = useCallback(async () => {
    try {
      const data = await adminGenreService.getAllGenres();
      let list = data?.data?.content || data?.data || data?.content || data || [];
      if (!Array.isArray(list)) list = [];
      setGenresList(list);
    } catch { /* ignore */ }
  }, []);

  const fetchMovies = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await adminMovieService.getMovies({
        page: currentPage,
        size: pageSize,
        search: searchTerm || undefined,
        status: statusFilter === 'ALL' ? undefined : (statusFilter || 'DRAFT'),
      });
      const normalized = normalizePagination(data?.data || data, pageSize);
      setMovies(normalized.items);
      setTotalElements(normalized.totalElements);
      setTotalPages(normalized.totalPages);
    } catch {
      triggerToast?.('Lỗi khi tải danh sách phim', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, pageSize, searchTerm, statusFilter, triggerToast]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchGenres();
  }, [fetchGenres]);

  useEffect(() => {
    const t = setTimeout(fetchMovies, 300);
    return () => clearTimeout(t);
  }, [fetchMovies]);

  const handleDelete = async (publicId, title) => {
    const shouldDelete = triggerConfirm 
      ? await triggerConfirm(`Bạn có chắc chắn muốn xóa phim "${title}"?`)
      : window.confirm(`Bạn có chắc chắn muốn xóa phim "${title}"?`);
      
    if (!shouldDelete) return;
    try {
      await adminMovieService.deleteMovie(publicId);
      triggerToast?.('Đã xóa phim thành công!');
      fetchMovies();
    } catch (err) {
      triggerToast?.(parseApiError(err), 'error');
    }
  };

  return {
    movies,
    genresList,
    setGenresList,
    isLoading,
    setIsLoading,
    currentPage,
    setCurrentPage,
    pageSize,
    setPageSize,
    statusFilter,
    setStatusFilter,
    searchTerm,
    setSearchTerm,
    totalElements,
    totalPages,
    fetchMovies,
    handleDelete,
  };
}
