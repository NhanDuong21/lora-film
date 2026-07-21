import { useState, useCallback } from 'react';
import adminRoomService from '../services/adminRoomService';
import { getErrorMessage } from '@/utils/apiErrorHandler';

export default function useAuditoriumDetail(roomId, triggerToast) {
  const [auditorium, setAuditorium] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchAuditorium = useCallback(async () => {
    if (!roomId) return;
    setIsLoading(true);
    setError(null);
    try {
      const res = await adminRoomService.getAdminSeatLayout(roomId);
      if (res?.success && res.data) {
        setAuditorium(res.data);
      } else {
        throw new Error('Dữ liệu phòng chiếu không hợp lệ');
      }
    } catch (err) {
      const errMsg = getErrorMessage(err, 'Lỗi khi tải chi tiết phòng chiếu');
      setError(errMsg);
      triggerToast?.(errMsg, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [roomId, triggerToast]);

  const updateAuditoriumBasicInfo = async (data) => {
    try {
      const res = await adminRoomService.updateAuditorium(roomId, data);
      if (res?.success) {
        triggerToast?.('Cập nhật thông tin phòng chiếu thành công!');
        fetchAuditorium();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Cập nhật phòng chiếu thất bại'), 'error');
      return false;
    }
  };

  const updateSeatLayout = async (seatsList) => {
    try {
      const res = await adminRoomService.bulkCreateSeats(roomId, { seats: seatsList });
      if (res?.success) {
        // triggerToast?.('Cập nhật sơ đồ ghế thành công!'); // Handled by caller to avoid double toast
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Cập nhật sơ đồ ghế thất bại'), 'error');
      return false;
    }
  };

  return {
    auditorium,
    isLoading,
    error,
    fetchAuditorium,
    updateAuditoriumBasicInfo,
    updateSeatLayout
  };
}
