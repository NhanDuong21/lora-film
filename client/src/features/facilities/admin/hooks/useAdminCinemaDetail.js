import { useState, useCallback } from 'react';
import adminCinemaService from '../services/adminCinemaService';
import { getErrorMessage } from '@/utils/apiErrorHandler';

export default function useAdminCinemaDetail(cinemaPublicId, triggerToast) {
  const [cinema, setCinema] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchCinema = useCallback(async () => {
    if (!cinemaPublicId) return;
    setIsLoading(true);
    setError(null);
    try {
      const res = await adminCinemaService.getAdminCinemaDetail(cinemaPublicId);
      if (res?.success && res.data) {
        setCinema(res.data);
      } else {
        throw new Error('Dữ liệu rạp chiếu không hợp lệ');
      }
    } catch (err) {
      const errMsg = getErrorMessage(err, 'Lỗi khi tải chi tiết rạp chiếu');
      setError(errMsg);
      triggerToast?.(errMsg, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [cinemaPublicId, triggerToast]);

  const updateCinemaBasicInfo = async (data) => {
    try {
      const res = await adminCinemaService.updateCinema(cinemaPublicId, data);
      if (res?.success) {
        // Also update status if provided and different
        if (data.status && data.status !== cinema?.status) {
          await adminCinemaService.updateCinemaStatus(cinemaPublicId, data.status);
        }
        triggerToast?.('Cập nhật thông tin rạp chiếu thành công!');
        fetchCinema();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Cập nhật rạp chiếu thất bại'), 'error');
      return false;
    }
  };

  const changeCinemaStatus = async (status) => {
    try {
      const res = await adminCinemaService.updateCinemaStatus(cinemaPublicId, status);
      if (res?.success) {
        triggerToast?.('Đã cập nhật trạng thái vận hành của cụm rạp.');
        await fetchCinema();
        return true;
      }
      return false;
    } catch (err) {
      triggerToast?.(
        getErrorMessage(err, 'Không thể cập nhật trạng thái vận hành của cụm rạp'),
        'error',
      );
      return false;
    }
  };

  const updateOperatingHours = async (hoursList) => {
    try {
      const res = await adminCinemaService.updateOperatingHours(cinemaPublicId, hoursList);
      if (res?.success) {
        triggerToast?.('Cập nhật giờ hoạt động thành công!');
        fetchCinema();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Cập nhật giờ hoạt động thất bại'), 'error');
      return false;
    }
  };

  const addMedia = async (mediaData) => {
    try {
      const res = await adminCinemaService.createCinemaMedia(cinemaPublicId, mediaData);
      if (res?.success) {
        triggerToast?.('Thêm phương tiện thành công!');
        fetchCinema();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Thêm phương tiện thất bại'), 'error');
      return false;
    }
  };

  const updateMedia = async (mediaPublicId, mediaData) => {
    try {
      const res = await adminCinemaService.updateCinemaMedia(mediaPublicId, mediaData);
      if (res?.success) {
        triggerToast?.('Cập nhật phương tiện thành công!');
        fetchCinema();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Cập nhật phương tiện thất bại'), 'error');
      return false;
    }
  };

  const archiveMedia = async (media) => {
    try {
      const res = await adminCinemaService.updateCinemaMedia(media.publicId, {
        url: media.url,
        title: media.title || '',
        displayOrder: media.displayOrder || 0,
        isPrimary: false,
        status: 'INACTIVE',
      });
      if (res?.success) {
        triggerToast?.('Đã lưu trữ hình ảnh. Dữ liệu lịch sử vẫn được giữ lại.');
        await fetchCinema();
        return true;
      }
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Không thể lưu trữ hình ảnh'), 'error');
      return false;
    }
  };

  const reorderMedia = async (orderedMedia) => {
    try {
      for (let index = 0; index < orderedMedia.length; index += 1) {
        const media = orderedMedia[index];
        await adminCinemaService.updateCinemaMedia(media.publicId, {
          url: media.url,
          title: media.title || '',
          displayOrder: index + 1,
          isPrimary: Boolean(media.isPrimary),
          status: media.status || 'ACTIVE',
        });
      }
      triggerToast?.('Đã lưu thứ tự hiển thị hình ảnh.');
      await fetchCinema();
      return true;
    } catch (err) {
      triggerToast?.(getErrorMessage(err, 'Không thể lưu thứ tự hình ảnh'), 'error');
      return false;
    }
  };

  return {
    cinema,
    isLoading,
    error,
    fetchCinema,
    updateCinemaBasicInfo,
    changeCinemaStatus,
    updateOperatingHours,
    addMedia,
    updateMedia,
    archiveMedia,
    reorderMedia
  };
}
