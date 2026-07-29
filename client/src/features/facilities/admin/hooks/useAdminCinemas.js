// eslint-disable-next-line no-unused-vars
import { useState, useEffect, useCallback, useMemo } from 'react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';

export default function useAdminCinemas({ triggerConfirm, triggerToast } = {}) {
  const [cinemas, setCinemas] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [cityFilter, setCityFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [error, setError] = useState(null);
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Available cities list for filters (extracted dynamically from loaded cinemas)
  const citiesList = useMemo(() => {
    const cities = cinemas.map(c => c.city).filter(Boolean);
    return [...new Set(cities)];
  }, [cinemas]);

  // Fetch Cinemas List
  const fetchCinemas = useCallback(async () => {
    setIsLoading(true);
    try {
      const params = {
        page: currentPage,
        size: pageSize,
        sort: 'createdAt,desc'
      };
      if (searchTerm.trim()) params.keyword = searchTerm.trim();
      if (cityFilter) params.city = cityFilter;
      if (statusFilter) params.status = statusFilter;

      const res = await adminCinemaService.getCinemas(params);
      if (res?.success && res?.data) {
        setCinemas(res.data.data || []);
        setTotalPages(res.data.totalPages || 0);
        setTotalElements(res.data.totalElements || 0);
      }
    // eslint-disable-next-line no-unused-vars
    } catch (err) {
      triggerToast?.('Không thể tải danh sách cụm rạp', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, pageSize, searchTerm, cityFilter, statusFilter, triggerToast]);

  // Handle delete
  const handleDeleteCinema = async (id, name) => {
    const shouldDelete = await triggerConfirm?.({
      title: `Ngừng sử dụng cụm rạp “${name}”?`,
      message: 'Nếu cụm rạp đã có lịch sử suất chiếu, hệ thống sẽ từ chối xóa và bạn nên chuyển rạp sang trạng thái đóng cửa.',
      confirmLabel: 'Tiếp tục',
      tone: 'danger',
    });

    if (shouldDelete) {
      setIsLoading(true);
      setError(null);
      try {
        await adminCinemaService.deleteCinema(id);
        setCinemas(cinemas.filter(c => c.publicId !== id));
        triggerToast?.('Xóa cụm rạp thành công', 'success');
        return true;
      } catch (err) {
        const errorMsg = err.response?.data?.message || err.message || 'Không thể xóa cụm rạp';
        let displayMsg = errorMsg;
        if (errorMsg.includes('showtime history') || errorMsg.includes('CINEMA_CANNOT_BE_DELETED_HAS_SHOWTIME_HISTORY')) {
            displayMsg = 'Cụm rạp này đã có dữ liệu giao dịch, không thể xóa. Vui lòng chuyển trạng thái sang ĐÓNG CỬA (CLOSED).';
        }
        setError(displayMsg);
        triggerToast?.(displayMsg, 'error');
        return false;
      } finally {
        setIsLoading(false);
      }
    }
    return false;
  };

  // Handle status update
  const handleStatusChange = useCallback(async (publicId, newStatus) => {
    try {
      await adminCinemaService.updateCinemaStatus(publicId, newStatus);
      triggerToast?.('Cập nhật trạng thái cụm rạp thành công!');
      fetchCinemas();
    // eslint-disable-next-line no-unused-vars
    } catch (err) {
      triggerToast?.('Không thể cập nhật trạng thái', 'error');
    }
  }, [fetchCinemas, triggerToast]);

  return {
    cinemas,
    isLoading,
    searchTerm,
    setSearchTerm,
    cityFilter,
    setCityFilter,
    statusFilter,
    setStatusFilter,
    currentPage,
    setCurrentPage,
    pageSize,
    totalPages,
    totalElements,
    citiesList,
    fetchCinemas,
    handleDeleteCinema,
    handleStatusChange,
    error
  };
}
