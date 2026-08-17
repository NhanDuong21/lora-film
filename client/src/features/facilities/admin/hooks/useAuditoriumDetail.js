import { useCallback, useState } from 'react';
import adminRoomService from '../services/adminRoomService';
import { getErrorMessage } from '@/utils/apiErrorHandler';

export default function useAuditoriumDetail(roomId, triggerToast) {
  const [auditorium, setAuditorium] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  const fetchAuditorium = useCallback(async (options = {}) => {
    if (!roomId) return;
    const silent = Boolean(options?.silent);
    if (!silent) setIsLoading(true);
    setError(null);
    try {
      const response = await adminRoomService.getAdminSeatLayout(roomId);
      if (!response?.success || !response.data) {
        throw new Error('Dữ liệu phòng chiếu không hợp lệ');
      }
      setAuditorium(response.data);
      setLastUpdatedAt(new Date());
    } catch (requestError) {
      const message = getErrorMessage(
        requestError,
        'Không thể tải thông tin phòng chiếu',
      );
      setError(message);
      if (!silent) triggerToast?.(message, 'error');
    } finally {
      if (!silent) setIsLoading(false);
    }
  }, [roomId, triggerToast]);

  const updateAuditoriumBasicInfo = useCallback(async (data, successMessage) => {
    try {
      const response = await adminRoomService.updateAuditorium(roomId, data);
      if (response?.success) {
        triggerToast?.(successMessage || 'Đã cập nhật phòng chiếu');
        await fetchAuditorium();
        return true;
      }
    } catch (requestError) {
      triggerToast?.(
        getErrorMessage(requestError, 'Không thể cập nhật phòng chiếu'),
        'error',
      );
    }
    return false;
  }, [fetchAuditorium, roomId, triggerToast]);

  const changeAuditoriumStatus = useCallback(async (targetStatus, successMessage) => {
    if (!auditorium) return false;
    return updateAuditoriumBasicInfo({
      name: auditorium.auditoriumName,
      screenType: auditorium.screenType,
      soundType: auditorium.soundType,
      capacity: auditorium.capacity,
      cleaningBufferMinutes: auditorium.cleaningBufferMinutes,
      status: targetStatus,
    }, successMessage);
  }, [auditorium, updateAuditoriumBasicInfo]);

  const updateSeatLayout = async (seatsList) => {
    try {
      const response = await adminRoomService.bulkCreateSeats(roomId, { seats: seatsList });
      if (response?.success) return true;
    } catch (requestError) {
      triggerToast?.(
        getErrorMessage(requestError, 'Không thể cập nhật sơ đồ ghế'),
        'error',
      );
    }
    return false;
  };

  return {
    auditorium,
    isLoading,
    error,
    lastUpdatedAt,
    fetchAuditorium,
    updateAuditoriumBasicInfo,
    changeAuditoriumStatus,
    updateSeatLayout,
  };
}
