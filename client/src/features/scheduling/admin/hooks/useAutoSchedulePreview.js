import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import adminAutoScheduleService from '@/features/scheduling/admin/services/adminAutoScheduleService';
import { derivePreviewCapabilities } from '@/features/scheduling/admin/utils/autoSchedulePreviewLifecycle';
import {
  SELECTION_BLOCK_TYPES,
  validateBulkSelection,
  validateSingleSelectionChange,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewSelection';

const PAGE_SIZE = 100;
const MAX_PAGE_CONCURRENCY = 3;

const createEmptySnapshot = () => ({
  sourcePreviewId: null,
  preview: null,
  items: [],
  selectedItemIds: new Set(),
  expectedVersion: 0,
});

const createIdleLoadState = () => ({
  active: false,
  loadedPages: 0,
  totalPages: 0,
  loadedItems: 0,
  totalItems: 0,
});

const isIntentionalCancellation = error => (
  error?.name === 'AbortError'
  || error?.name === 'CanceledError'
  || error?.code === 'ERR_CANCELED'
);

const createLoadError = (code, message, blocksMutations = false) => {
  const error = new Error(message);
  error.code = code;
  error.blocksMutations = blocksMutations;
  return error;
};

const getSelectionGuardMessage = type => {
  switch (type) {
    case SELECTION_BLOCK_TYPES.REJECTED:
      return 'Suất đề xuất không hợp lệ nên không thể được chọn.';
    case SELECTION_BLOCK_TYPES.ITEM_NOT_PENDING:
      return 'Chỉ suất đề xuất đang chờ kiểm tra mới có thể được chọn.';
    case SELECTION_BLOCK_TYPES.MALFORMED_ITEM:
    case SELECTION_BLOCK_TYPES.MALFORMED_SELECTED_ITEM:
      return 'Thiếu dữ liệu thời gian sử dụng phòng. Vui lòng cập nhật lại bản lịch.';
    case SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP:
      return 'Suất đề xuất bị trùng thời gian sử dụng phòng với một suất đã chọn.';
    default:
      return 'Không thể cập nhật lựa chọn này. Vui lòng cập nhật lại bản lịch.';
  }
};

const getSelectionBackendErrorMessage = (error, fallbackMessage) => {
  switch (error?.errorCode) {
    case 'AUTO_SCHEDULE_SELECTION_OVERLAP':
      return 'Không thể lưu lựa chọn vì có các suất chiếm cùng phòng bị trùng thời gian.';
    case 'AUTO_SCHEDULE_INVALID_ITEM_SELECTION':
      return 'Không thể lưu vì suất đề xuất này không còn hợp lệ. Hệ thống đang tải lại dữ liệu.';
    default:
      return fallbackMessage;
  }
};

const createUuid = () => (
  globalThis.crypto?.randomUUID?.()
  || `apply-${Date.now()}-${Math.random().toString(16).slice(2)}`
);

const readPage = response => {
  if (!response?.success || !response.data?.preview || !response.data?.items) {
    throw createLoadError('INVALID_RESPONSE', 'Dữ liệu bản lịch chưa đầy đủ.');
  }
  return response.data;
};

export default function useAutoSchedulePreview(
  previewPublicId,
  { triggerToast, onSuccess } = {},
) {
  const [snapshot, setSnapshot] = useState(createEmptySnapshot);
  const [loadState, setLoadState] = useState(createIdleLoadState);
  const [snapshotError, setSnapshotError] = useState(null);
  const [isApplying, setIsApplying] = useState(false);
  const [isUpdatingSelection, setIsUpdatingSelection] = useState(false);
  const [pricingPreflight, setPricingPreflight] = useState(null);
  const snapshotRef = useRef(snapshot);
  const activeLoadRef = useRef({ generation: 0, controller: null });
  const applyIdempotencyRef = useRef({ fingerprint: '', key: '' });

  const updateSnapshot = useCallback(updater => {
    setSnapshot(previous => {
      const next = typeof updater === 'function' ? updater(previous) : updater;
      snapshotRef.current = next;
      return next;
    });
  }, []);

  const abortActiveLoad = useCallback(() => {
    activeLoadRef.current.controller?.abort();
    activeLoadRef.current = {
      generation: activeLoadRef.current.generation + 1,
      controller: null,
    };
  }, []);

  const fetchPreview = useCallback(async () => {
    if (!previewPublicId) return false;

    activeLoadRef.current.controller?.abort();
    const generation = activeLoadRef.current.generation + 1;
    const controller = new AbortController();
    activeLoadRef.current = { generation, controller };

    const hasSafePreviousSnapshot = snapshotRef.current.sourcePreviewId === previewPublicId
      && Boolean(snapshotRef.current.preview);
    if (!hasSafePreviousSnapshot) {
      const emptySnapshot = createEmptySnapshot();
      snapshotRef.current = emptySnapshot;
      setSnapshot(emptySnapshot);
    }

    setSnapshotError(null);
    setLoadState({
      active: true,
      loadedPages: 0,
      totalPages: 0,
      loadedItems: 0,
      totalItems: 0,
    });

    const isCurrentLoad = () => (
      activeLoadRef.current.generation === generation
      && activeLoadRef.current.controller === controller
      && !controller.signal.aborted
    );

    try {
      const requestedPages = new Set([0]);
      const completedPages = new Set();
      const firstResponse = await adminAutoScheduleService.getPreview(
        previewPublicId,
        { page: 0, size: PAGE_SIZE },
        { signal: controller.signal },
      );
      if (!isCurrentLoad()) return false;

      const firstPage = readPage(firstResponse);
      const previewVersion = firstPage.preview.version;
      if (previewVersion === null || previewVersion === undefined) {
        throw createLoadError(
          'MISSING_VERSION',
          'Dữ liệu lịch tải về chưa đồng nhất. Hãy làm mới để kiểm tra lại.',
          true,
        );
      }
      const reportedTotalPages = Math.max(Number(firstPage.items.totalPages) || 0, 1);
      const totalElements = Math.max(
        Number(firstPage.items.totalElements) || firstPage.items.content?.length || 0,
        0,
      );
      const pages = Array.from({ length: reportedTotalPages });
      pages[0] = firstPage.items.content || [];
      completedPages.add(0);

      setLoadState({
        active: true,
        loadedPages: 1,
        totalPages: reportedTotalPages,
        loadedItems: pages[0].length,
        totalItems: totalElements,
      });

      let nextPage = 1;
      const worker = async () => {
        while (isCurrentLoad()) {
          const pageNumber = nextPage;
          nextPage += 1;
          if (pageNumber >= reportedTotalPages) return;
          if (requestedPages.has(pageNumber)) {
            throw createLoadError('DUPLICATE_PAGE', `Trang ${pageNumber} đã được yêu cầu nhiều lần.`, true);
          }
          requestedPages.add(pageNumber);

          const response = await adminAutoScheduleService.getPreview(
            previewPublicId,
            { page: pageNumber, size: PAGE_SIZE },
            { signal: controller.signal },
          );
          if (!isCurrentLoad()) return;

          const page = readPage(response);
          if (String(page.preview.version) !== String(previewVersion)) {
            throw createLoadError(
              'VERSION_MISMATCH',
              'Bản lịch đã thay đổi trong lúc tải. Hãy cập nhật lại để xem dữ liệu mới nhất.',
              true,
            );
          }

          pages[pageNumber] = page.items.content || [];
          completedPages.add(pageNumber);
          const loadedItems = pages.reduce((count, content) => count + (content?.length || 0), 0);
          setLoadState({
            active: true,
            loadedPages: completedPages.size,
            totalPages: reportedTotalPages,
            loadedItems,
            totalItems: totalElements,
          });
        }
      };

      const workerCount = Math.min(MAX_PAGE_CONCURRENCY, Math.max(reportedTotalPages - 1, 0));
      await Promise.all(Array.from({ length: workerCount }, () => worker()));
      if (!isCurrentLoad()) return false;

      const allItems = pages.flatMap(content => content || []);
      if (completedPages.size !== reportedTotalPages || allItems.length !== totalElements) {
        throw createLoadError(
          'INCOMPLETE_SNAPSHOT',
          'Không thể tải đầy đủ các suất đề xuất. Hãy cập nhật lại lịch.',
          true,
        );
      }

      const selectedItemIds = new Set(
        allItems.filter(item => item.selected).map(item => item.itemPublicId),
      );
      updateSnapshot({
        sourcePreviewId: previewPublicId,
        preview: firstPage.preview,
        items: allItems,
        selectedItemIds,
        expectedVersion: previewVersion,
      });
      setPricingPreflight(null);
      setLoadState({
        active: false,
        loadedPages: reportedTotalPages,
        totalPages: reportedTotalPages,
        loadedItems: allItems.length,
        totalItems: totalElements,
      });
      return true;
    } catch (error) {
      const isObsolete = activeLoadRef.current.generation !== generation
        || activeLoadRef.current.controller !== controller;
      if (isObsolete || controller.signal.aborted || isIntentionalCancellation(error)) return false;

      controller.abort();
      const normalizedError = {
        code: error?.code || 'LOAD_FAILED',
        message: error?.message || 'Không thể tải chi tiết bản lịch.',
        blocksMutations: Boolean(error?.blocksMutations),
      };
      setSnapshotError(normalizedError);
      setLoadState(previous => ({ ...previous, active: false }));
      if (!normalizedError.blocksMutations) {
        triggerToast?.('Không thể tải chi tiết bản lịch', 'error');
      }
      return false;
    }
  }, [previewPublicId, triggerToast, updateSnapshot]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- route identity starts the cancellable server snapshot load.
    fetchPreview();
    return () => abortActiveLoad();
  }, [abortActiveLoad, fetchPreview]);

  const isLoading = loadState.active && !snapshot.preview;
  const isRefreshing = loadState.active && Boolean(snapshot.preview);
  const isSnapshotUpdating = loadState.active;
  const capabilities = useMemo(() => derivePreviewCapabilities(snapshot.preview, {
    selectedCount: snapshot.selectedItemIds.size,
    isSnapshotUpdating,
    isApplying,
    isUpdatingSelection,
    hasUnsafeSnapshot: Boolean(snapshotError?.blocksMutations),
  }), [
    isApplying,
    isSnapshotUpdating,
    isUpdatingSelection,
    snapshot.preview,
    snapshot.selectedItemIds,
    snapshotError?.blocksMutations,
  ]);

  const handleToggleSelection = async (itemPublicId, currentSelectedState) => {
    if (!capabilities.canSelect) {
      triggerToast?.('Lịch này hiện không cho phép thay đổi các suất đã chọn.', 'error');
      return;
    }

    const newSelectedState = !currentSelectedState;
    const guard = validateSingleSelectionChange(
      snapshot.items,
      snapshot.selectedItemIds,
      itemPublicId,
      newSelectedState,
    );
    if (!guard.valid) {
      triggerToast?.(getSelectionGuardMessage(guard.type), 'error');
      return;
    }

    const previousSelectedIds = new Set(snapshot.selectedItemIds);
    setPricingPreflight(null);
    updateSnapshot(previous => {
      const nextSelectedIds = new Set(previous.selectedItemIds);
      if (newSelectedState) nextSelectedIds.add(itemPublicId);
      else nextSelectedIds.delete(itemPublicId);
      return { ...previous, selectedItemIds: nextSelectedIds };
    });

    setIsUpdatingSelection(true);
    try {
      const payload = {
        expectedVersion: snapshot.expectedVersion,
        items: [{ itemPublicId, selected: newSelectedState }],
      };
      const response = await adminAutoScheduleService.updateSelections(previewPublicId, payload);
      if (response?.success && response.data) {
        updateSnapshot(previous => ({
          ...previous,
          preview: response.data,
          expectedVersion: response.data.version,
        }));
      }
    } catch (error) {
      updateSnapshot(previous => ({ ...previous, selectedItemIds: previousSelectedIds }));
      triggerToast?.(getSelectionBackendErrorMessage(
        error,
        'Lỗi cập nhật trạng thái chọn. Đang tải lại dữ liệu.',
      ), 'error');
      fetchPreview();
    } finally {
      setIsUpdatingSelection(false);
    }
  };

  const handleBulkSelection = async selectedIdsArray => {
    if (!capabilities.canSelect) {
      triggerToast?.('Lịch này hiện không cho phép thay đổi các suất đã chọn.', 'error');
      return;
    }

    const guard = validateBulkSelection(snapshot.items, selectedIdsArray);
    if (!guard.valid) {
      triggerToast?.(getSelectionGuardMessage(guard.type), 'error');
      return;
    }

    const previousSelectedIds = new Set(snapshot.selectedItemIds);
    setPricingPreflight(null);
    updateSnapshot(previous => ({
      ...previous,
      selectedItemIds: new Set(selectedIdsArray),
    }));
    setIsUpdatingSelection(true);
    try {
      const selectedIds = new Set(selectedIdsArray);
      const payload = {
        expectedVersion: snapshot.expectedVersion,
        items: snapshot.items.map(item => ({
          itemPublicId: item.itemPublicId,
          selected: selectedIds.has(item.itemPublicId),
        })),
      };
      const response = await adminAutoScheduleService.updateSelections(previewPublicId, payload);
      if (response?.success && response.data) {
        updateSnapshot(previous => ({
          ...previous,
          preview: response.data,
          expectedVersion: response.data.version,
        }));
      }
    } catch (error) {
      updateSnapshot(previous => ({ ...previous, selectedItemIds: previousSelectedIds }));
      triggerToast?.(getSelectionBackendErrorMessage(
        error,
        'Không thể tự chọn lịch không xung đột. Đang tải lại dữ liệu.',
      ), 'error');
      fetchPreview();
    } finally {
      setIsUpdatingSelection(false);
    }
  };

  const handleApply = async () => {
    if (!capabilities.canApply) {
      triggerToast?.('Lịch này chưa sẵn sàng để tạo suất chiếu.', 'error');
      return null;
    }

    setIsApplying(true);
    try {
      const selectionFingerprint = Array.from(snapshot.selectedItemIds).sort().join(',');
      const fingerprint = `${previewPublicId}:${snapshot.expectedVersion}:${selectionFingerprint}`;
      if (applyIdempotencyRef.current.fingerprint !== fingerprint) {
        applyIdempotencyRef.current = { fingerprint, key: createUuid() };
      }
      const payload = {
        expectedVersion: snapshot.expectedVersion,
        idempotencyKey: applyIdempotencyRef.current.key,
      };
      const response = await adminAutoScheduleService.applyPreview(previewPublicId, payload);
      if (response?.success) {
        setPricingPreflight({
          complete: true,
          totalCandidateCount: snapshot.selectedItemIds.size,
          completeCandidateCount: snapshot.selectedItemIds.size,
          incompleteCandidateCount: 0,
          ambiguousCandidateCount: 0,
          reasonGroups: [],
          candidates: [],
        });
        triggerToast?.('Đã tạo các suất chiếu ở trạng thái đang soạn.', 'success');
        applyIdempotencyRef.current = { fingerprint: '', key: '' };
        onSuccess?.(response.data);
        return response.data;
      }
      return null;
    } catch (error) {
      if (
        ['PRICING_INCOMPLETE', 'PRICING_AMBIGUOUS'].includes(error?.errorCode)
        && error?.data
      ) {
        setPricingPreflight(error.data);
      }
      const message = error?.message
        || error?.response?.data?.message
        || 'Không thể tạo suất chiếu từ lịch này.';
      triggerToast?.(message, 'error');
      return null;
    } finally {
      setIsApplying(false);
    }
  };

  return {
    preview: snapshot.preview,
    items: snapshot.items,
    selectedItemIds: snapshot.selectedItemIds,
    expectedVersion: snapshot.expectedVersion,
    isLoading,
    isRefreshing,
    isSnapshotUpdating,
    loadingProgress: {
      loadedPages: loadState.loadedPages,
      totalPages: loadState.totalPages,
      loadedItems: loadState.loadedItems,
      totalItems: loadState.totalItems,
    },
    snapshotError,
    pricingPreflight,
    capabilities,
    isApplying,
    isUpdatingSelection,
    handleToggleSelection,
    handleBulkSelection,
    handleApply,
    fetchPreview,
  };
}
