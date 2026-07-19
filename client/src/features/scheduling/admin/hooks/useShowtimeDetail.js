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
        adminShowtimeService.getShowtimeDetail(showtimePublicId)
          .catch(err => ({ success: false, error: err, isDetailError: true })),
        adminShowtimeService.getStatusHistory(showtimePublicId)
          .catch(err => ({ success: false, data: [] })),
        adminShowtimeService.getPrices(showtimePublicId)
          .catch(err => ({ success: false, data: { prices: [] }, isPricingError: err.response?.status }))
      ]);
      
      if (detailRes?.isDetailError) {
          triggerToast?.('Không thể tải chi tiết suất chiếu chính', 'error');
          return;
      }

      if (detailRes?.success) setShowtime(detailRes.data);
      if (historyRes?.success) setHistory(historyRes.data);
      if (pricesRes?.success) {
          setPrices(pricesRes.data);
      } else if (pricesRes?.isPricingError) {
          // Pass the status code for conditional rendering
          setPrices({ errorStatus: pricesRes.isPricingError });
      }
    } catch (err) {
      triggerToast?.('Lỗi hệ thống khi tải dữ liệu', 'error');
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
