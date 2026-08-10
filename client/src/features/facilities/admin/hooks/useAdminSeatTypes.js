import { useState, useCallback } from 'react';
import adminSeatTypeService from '../services/adminSeatTypeService';
import { getErrorMessage } from '@/utils/apiErrorHandler';

export default function useAdminSeatTypes(triggerToast) {
  const [seatTypes, setSeatTypes] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchSeatTypes = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await adminSeatTypeService.getAllSeatTypes();
      if (res?.success && Array.isArray(res.data)) {
        setSeatTypes(res.data);
      } else {
        setSeatTypes([]);
      }
    } catch (err) {
      const errMsg = getErrorMessage(err, 'Lỗi khi tải danh sách loại ghế');
      setError(errMsg);
      triggerToast?.(errMsg, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [triggerToast]);

  const createSeatType = async (data) => {
    try {
      const res = await adminSeatTypeService.createSeatType(data);
      if (res?.success) {
        triggerToast?.('Thêm loại ghế thành công!');
        fetchSeatTypes();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Thêm loại ghế thất bại'), 'error');
      return false;
    }
  };

  const updateSeatType = async (publicId, data) => {
    try {
      const res = await adminSeatTypeService.updateSeatType(publicId, data);
      if (res?.success) {
        triggerToast?.('Cập nhật loại ghế thành công!');
        fetchSeatTypes();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Cập nhật loại ghế thất bại'), 'error');
      return false;
    }
  };

  return {
    seatTypes,
    isLoading,
    error,
    fetchSeatTypes,
    createSeatType,
    updateSeatType
  };
}
