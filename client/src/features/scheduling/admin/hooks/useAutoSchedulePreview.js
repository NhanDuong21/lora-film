import { useState, useEffect, useCallback, useMemo } from 'react';
import adminAutoScheduleService from '@/features/scheduling/admin/services/adminAutoScheduleService';

export default function useAutoSchedulePreview(previewPublicId, { triggerToast, onSuccess }) {
  const [preview, setPreview] = useState(null);
  const [items, setItems] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isApplying, setIsApplying] = useState(false);
  const [isUpdatingSelection, setIsUpdatingSelection] = useState(false);
  const [selectedItemIds, setSelectedItemIds] = useState(new Set());
  const [expectedVersion, setExpectedVersion] = useState(0);

  const fetchPreview = useCallback(async () => {
    if (!previewPublicId) return;
    setIsLoading(true);
    try {
      const res = await adminAutoScheduleService.getPreview(previewPublicId, { size: 500 }); // Try to get all items
      if (res?.success && res.data) {
        setPreview(res.data.preview);
        setExpectedVersion(res.data.preview.version);
        setItems(res.data.items?.data || []);
        
        // Initialize selected items state
        const initialSelected = new Set();
        (res.data.items?.data || []).forEach(item => {
          if (item.selected) initialSelected.add(item.itemPublicId);
        });
        setSelectedItemIds(initialSelected);
      }
    } catch (err) {
      triggerToast?.('Không thể tải chi tiết bản xem trước', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [previewPublicId, triggerToast]);

  useEffect(() => {
    fetchPreview();
  }, [fetchPreview]);

  // Group items by Date -> Auditorium
  const groupedItems = useMemo(() => {
    const groups = {};
    items.forEach(item => {
      // Group by Date string (YYYY-MM-DD based on startTime)
      // Note: startTime is UTC, we should display it in Cinema local time ideally.
      // But for grouping, a simple localized date string is fine.
      const d = new Date(item.startTime);
      const dateKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      
      if (!groups[dateKey]) groups[dateKey] = {};
      
      const audKey = item.auditoriumName || item.auditoriumPublicId;
      if (!groups[dateKey][audKey]) groups[dateKey][audKey] = [];
      
      groups[dateKey][audKey].push(item);
    });

    // Sort items inside each auditorium by startTime
    Object.keys(groups).forEach(dateKey => {
      Object.keys(groups[dateKey]).forEach(audKey => {
        groups[dateKey][audKey].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
      });
    });

    return groups;
  }, [items]);

  const handleToggleSelection = async (itemPublicId, currentSelectedState) => {
    // Optimistic UI update
    const newSelectedState = !currentSelectedState;
    setSelectedItemIds(prev => {
      const next = new Set(prev);
      if (newSelectedState) next.add(itemPublicId);
      else next.delete(itemPublicId);
      return next;
    });

    // Real API call
    setIsUpdatingSelection(true);
    try {
      const payload = {
        expectedVersion,
        items: [{ itemPublicId, selected: newSelectedState }]
      };
      const res = await adminAutoScheduleService.updateSelections(previewPublicId, payload);
      if (res?.success && res.data) {
        // Update to new version
        setExpectedVersion(res.data.version);
        setPreview(res.data);
      }
    } catch (err) {
      triggerToast?.('Lỗi cập nhật trạng thái chọn. Đang tải lại dữ liệu.', 'error');
      // Revert optimism
      fetchPreview();
    } finally {
      setIsUpdatingSelection(false);
    }
  };

  const handleApply = async () => {
    if (selectedItemIds.size === 0) {
      triggerToast?.('Không có suất chiếu nào được chọn', 'error');
      return;
    }
    
    setIsApplying(true);
    try {
      const payload = {
        expectedVersion,
        idempotencyKey: crypto.randomUUID()
      };
      const res = await adminAutoScheduleService.applyPreview(previewPublicId, payload);
      if (res?.success) {
        triggerToast?.('Đã áp dụng lịch chiếu thành công', 'success');
        if (onSuccess) onSuccess();
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Lỗi áp dụng lịch chiếu';
      triggerToast?.(msg, 'error');
    } finally {
      setIsApplying(false);
    }
  };

  return {
    preview, items, groupedItems,
    isLoading, isApplying, isUpdatingSelection,
    selectedItemIds, handleToggleSelection,
    handleApply, fetchPreview
  };
}
