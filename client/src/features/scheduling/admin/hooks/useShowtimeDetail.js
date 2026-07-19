import { useState, useEffect, useCallback } from 'react';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';

export default function useShowtimeDetail(showtimePublicId, { triggerToast }) {
  const [showtime, setShowtime] = useState(null);
  const [history, setHistory] = useState([]);
  const [prices, setPrices] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  const fetchDetail = useCallback(async () => {
    if (!showtimePublicId) return;
    setIsLoading(true);
    try {
      const [detailRes, historyRes, pricesRes] = await Promise.all([
        adminShowtimeService.getShowtimeDetail(showtimePublicId).catch(() => null),
        adminShowtimeService.getStatusHistory(showtimePublicId).catch(() => null),
        adminShowtimeService.getPrices(showtimePublicId).catch(() => null)
      ]);
      
      if (detailRes?.success) setShowtime(detailRes.data);
      if (historyRes?.success) setHistory(historyRes.data);
      if (pricesRes?.success) setPrices(pricesRes.data);
    } catch (err) {
      triggerToast?.('Không thể tải chi tiết suất chiếu', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [showtimePublicId, triggerToast]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  const handleUpdateStatus = async (newStatus, reason) => {
    setIsUpdatingStatus(true);
    try {
      const res = await adminShowtimeService.transitionStatus(showtimePublicId, { status: newStatus, reason });
      if (res?.success) {
        triggerToast?.('Đã cập nhật trạng thái suất chiếu', 'success');
        fetchDetail(); // Reload everything (detail + history)
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Lỗi cập nhật trạng thái';
      triggerToast?.(msg, 'error');
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  return {
    showtime, history, prices, isLoading, isUpdatingStatus,
    handleUpdateStatus, fetchDetail
  };
}
