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
      let page = 0;
      let allItems = [];
      let previewData = null;
      let hasMore = true;

      while (hasMore) {
        const res = await adminAutoScheduleService.getPreview(previewPublicId, { page, size: 100 });
        if (res?.success && res.data) {
          if (!previewData) {
            previewData = res.data.preview;
            setExpectedVersion(res.data.preview.version);
          }
          const pageData = res.data.items?.content || [];
          allItems = allItems.concat(pageData);
          
          if (!res.data.items || page >= (res.data.items.totalPages - 1) || pageData.length === 0) {
            hasMore = false;
          } else {
            page++;
          }
        } else {
          hasMore = false;
        }
      }

      if (previewData) {
        setPreview(previewData);
        setItems(allItems);
        
        // Initialize selected items state
        const initialSelected = new Set();
        allItems.forEach(item => {
          if (item.selected) initialSelected.add(item.itemPublicId);
        });
        setSelectedItemIds(initialSelected);
      }
    } catch (err) {
      console.error(err);
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

  const handleBulkSelection = async (selectedIdsArray) => {
    // Optimistic UI
    setSelectedItemIds(new Set(selectedIdsArray));

    setIsUpdatingSelection(true);
    try {
      // Map all candidates to selected true/false
      const payloadItems = items.map(item => ({
        itemPublicId: item.itemPublicId,
        selected: selectedIdsArray.includes(item.itemPublicId)
      }));

      const payload = {
        expectedVersion,
        items: payloadItems
      };
      const res = await adminAutoScheduleService.updateSelections(previewPublicId, payload);
      if (res?.success && res.data) {
        setExpectedVersion(res.data.version);
        setPreview(res.data);
      }
    } catch (err) {
      triggerToast?.('Lỗi cập nhật đề xuất tối ưu. Đang tải lại dữ liệu.', 'error');
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
    selectedItemIds, handleToggleSelection, handleBulkSelection,
    handleApply, fetchPreview
  };
}
