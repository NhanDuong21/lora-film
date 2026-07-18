import { useState, useCallback } from 'react';
import adminCinemaService from '../services/adminCinemaService';
import { getErrorMessage } from '@/utils/apiErrorHandler';

export default function useClosurePeriods(cinemaPublicId, triggerToast) {
  const [closurePeriods, setClosurePeriods] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchClosurePeriods = useCallback(async () => {
    if (!cinemaPublicId) return;
    setIsLoading(true);
    setError(null);
    try {
      const res = await adminCinemaService.getClosurePeriods(cinemaPublicId, { page: 0, size: 100 });
      if (res?.success && res.data?.data) {
        setClosurePeriods(res.data.data);
      } else {
        setClosurePeriods([]);
      }
    } catch (err) {
      const errMsg = getErrorMessage(err, 'Lỗi khi tải lịch đóng cửa');
      setError(errMsg);
      triggerToast?.(errMsg, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [cinemaPublicId, triggerToast]);

  const createClosurePeriod = async (data) => {
    try {
      const res = await adminCinemaService.createClosurePeriod(cinemaPublicId, data);
      if (res?.success) {
        triggerToast?.('Thêm lịch đóng cửa thành công!');
        fetchClosurePeriods();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Thêm lịch đóng cửa thất bại'), 'error');
      return false;
    }
  };

  const cancelClosurePeriod = async (closurePeriodId) => {
    try {
      const res = await adminCinemaService.cancelClosurePeriod(closurePeriodId);
      if (res?.success) {
        triggerToast?.('Hủy lịch đóng cửa thành công!');
        fetchClosurePeriods();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Hủy lịch đóng cửa thất bại'), 'error');
      return false;
    }
  };

  return {
    closurePeriods,
    isLoading,
    error,
    fetchClosurePeriods,
    createClosurePeriod,
    cancelClosurePeriod
  };
}
