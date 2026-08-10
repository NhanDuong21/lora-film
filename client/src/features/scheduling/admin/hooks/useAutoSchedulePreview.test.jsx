import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminAutoScheduleService from '../services/adminAutoScheduleService';
import useAutoSchedulePreview from './useAutoSchedulePreview';

vi.mock('../services/adminAutoScheduleService', () => ({
  default: {
    getPreview: vi.fn(),
    updateSelections: vi.fn(),
    checkPricingReadiness: vi.fn(),
    applyPreview: vi.fn(),
  },
}));

const first = {
  itemPublicId: 'item-1',
  auditoriumPublicId: 'aud-1',
  startTime: '2026-07-24T10:00:00Z',
  endTime: '2026-07-24T11:00:00Z',
  occupancyEndTime: '2026-07-24T11:15:00Z',
  validationStatus: 'VALID',
  applyStatus: 'PENDING',
  selected: true,
};
const second = {
  ...first,
  itemPublicId: 'item-2',
  startTime: '2026-07-24T11:15:00Z',
  endTime: '2026-07-24T12:15:00Z',
  occupancyEndTime: '2026-07-24T12:30:00Z',
  selected: false,
};

const summary = (version = 3, overrides = {}) => ({
  previewPublicId: 'preview-1',
  version,
  status: 'PREVIEWED',
  timezoneSnapshot: 'UTC',
  expiresAt: '2099-07-24T12:00:00Z',
  applyMode: 'ALL_OR_NOTHING',
  ...overrides,
});

const previewResponse = ({
  items = [first, second],
  version = 3,
  page = 0,
  totalPages = 1,
  totalElements = items.length,
  previewOverrides = {},
} = {}) => ({
  success: true,
  data: {
    preview: summary(version, previewOverrides),
    items: {
      content: items,
      pageNumber: page,
      pageSize: 100,
      totalPages,
      totalElements,
    },
  },
});

const deferred = () => {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
};

const cancelledError = () => Object.assign(new Error('cancelled'), {
  name: 'CanceledError',
  code: 'ERR_CANCELED',
});

describe('useAutoSchedulePreview bounded loading', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminAutoScheduleService.updateSelections.mockResolvedValue({
      success: true,
      data: summary(4),
    });
    adminAutoScheduleService.checkPricingReadiness.mockResolvedValue({
      success: true,
      data: {
        complete: true,
        totalCandidateCount: 1,
        completeCandidateCount: 1,
        incompleteCandidateCount: 0,
        ambiguousCandidateCount: 0,
        reasonGroups: [],
        candidates: [],
      },
    });
    adminAutoScheduleService.applyPreview.mockResolvedValue({ success: true });
  });

  it('loads page 0 first, requests each page once, and uses at most three workers', async () => {
    let activeRequests = 0;
    let maximumConcurrency = 0;
    adminAutoScheduleService.getPreview.mockImplementation(async (_id, params) => {
      if (params.page === 0) {
        return previewResponse({
          items: [{ ...first, itemPublicId: 'item-0' }],
          totalPages: 7,
          totalElements: 7,
        });
      }

      activeRequests += 1;
      maximumConcurrency = Math.max(maximumConcurrency, activeRequests);
      await new Promise(resolve => setTimeout(resolve, 5));
      activeRequests -= 1;
      return previewResponse({
        items: [{ ...first, itemPublicId: `item-${params.page}`, selected: false }],
        page: params.page,
        totalPages: 7,
        totalElements: 7,
      });
    });

    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', {}));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    const requestedPages = adminAutoScheduleService.getPreview.mock.calls
      .map(([, params]) => params.page);
    expect(requestedPages[0]).toBe(0);
    expect(requestedPages).toHaveLength(7);
    expect(new Set(requestedPages).size).toBe(7);
    expect(maximumConcurrency).toBeLessThanOrEqual(3);
    expect(result.current.items).toHaveLength(7);
    expect(result.current.loadingProgress).toEqual({
      loadedPages: 7,
      totalPages: 7,
      loadedItems: 7,
      totalItems: 7,
    });
    expect(adminAutoScheduleService.getPreview.mock.calls[0][2].signal)
      .toBeInstanceOf(AbortSignal);
  });

  it('publishes the selected schedule before loading a large candidate set in the background', async () => {
    const backgroundGate = deferred();
    adminAutoScheduleService.getPreview.mockImplementation(async (_id, params) => {
      if (params.selected) {
        return previewResponse({
          items: [first],
          totalPages: 1,
          totalElements: 1,
          previewOverrides: { totalCandidateCount: 20 },
        });
      }
      if (params.page === 0) {
        return previewResponse({
          items: [first],
          totalPages: 20,
          totalElements: 20,
          previewOverrides: { totalCandidateCount: 20 },
        });
      }

      await backgroundGate.promise;
      return previewResponse({
        items: [{ ...second, itemPublicId: `candidate-${params.page}` }],
        page: params.page,
        totalPages: 20,
        totalElements: 20,
        previewOverrides: { totalCandidateCount: 20 },
      });
    });

    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', {}));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.items.map(item => item.itemPublicId)).toEqual(['item-1']);
    expect(result.current.isLoadingCandidates).toBe(true);
    expect(result.current.capabilities.canSelect).toBe(true);
    expect(adminAutoScheduleService.getPreview).toHaveBeenCalledWith(
      'preview-1',
      { page: 0, size: 100, selected: true, sort: 'startTime,asc' },
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    );

    await act(async () => {
      backgroundGate.resolve();
      await backgroundGate.promise;
    });
    await waitFor(() => expect(result.current.isLoadingCandidates).toBe(false));

    expect(result.current.items).toHaveLength(20);
    expect(result.current.candidateLoadingProgress.loadedPages).toBe(20);
  });

  it('aborts obsolete refreshes, preview ID changes, and unmount without user-facing errors', async () => {
    const triggerToast = vi.fn();
    adminAutoScheduleService.getPreview.mockResolvedValueOnce(previewResponse());
    const pendingSignals = [];
    adminAutoScheduleService.getPreview.mockImplementation((_id, _params, config) => {
      pendingSignals.push(config.signal);
      return new Promise((_resolve, reject) => {
        config.signal.addEventListener('abort', () => reject(cancelledError()), { once: true });
      });
    });

    const { result, rerender, unmount } = renderHook(
      ({ id }) => useAutoSchedulePreview(id, { triggerToast }),
      { initialProps: { id: 'preview-1' } },
    );
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => { void result.current.fetchPreview(); });
    await waitFor(() => expect(pendingSignals).toHaveLength(1));
    act(() => { void result.current.fetchPreview(); });
    await waitFor(() => expect(pendingSignals).toHaveLength(2));
    expect(pendingSignals[0].aborted).toBe(true);

    rerender({ id: 'preview-2' });
    await waitFor(() => expect(pendingSignals).toHaveLength(3));
    expect(pendingSignals[1].aborted).toBe(true);

    unmount();
    expect(pendingSignals[2].aborted).toBe(true);
    await Promise.resolve();
    expect(triggerToast).not.toHaveBeenCalled();
  });

  it('prevents an obsolete response from replacing a newer snapshot', async () => {
    const oldLoad = deferred();
    adminAutoScheduleService.getPreview
      .mockImplementationOnce(() => oldLoad.promise)
      .mockResolvedValueOnce(previewResponse({
        version: 5,
        items: [{ ...first, itemPublicId: 'new-item' }],
        totalElements: 1,
      }));

    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', {}));
    await waitFor(() => expect(adminAutoScheduleService.getPreview).toHaveBeenCalledTimes(1));
    act(() => { void result.current.fetchPreview(); });
    await waitFor(() => expect(result.current.expectedVersion).toBe(5));

    await act(async () => {
      oldLoad.resolve(previewResponse({
        version: 3,
        items: [{ ...first, itemPublicId: 'old-item' }],
        totalElements: 1,
      }));
      await oldLoad.promise;
    });

    expect(result.current.expectedVersion).toBe(5);
    expect(result.current.items[0].itemPublicId).toBe('new-item');
  });

  it('keeps the previous complete snapshot visible and locks controls during refresh', async () => {
    adminAutoScheduleService.getPreview.mockResolvedValueOnce(previewResponse());
    const refresh = deferred();
    adminAutoScheduleService.getPreview.mockImplementationOnce(() => refresh.promise);
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', {}));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => { void result.current.fetchPreview(); });
    await waitFor(() => expect(result.current.isRefreshing).toBe(true));
    expect(result.current.items).toHaveLength(2);
    expect(result.current.capabilities.canSelect).toBe(false);
    expect(result.current.capabilities.canApply).toBe(false);

    await act(async () => {
      refresh.resolve(previewResponse({ version: 4 }));
      await refresh.promise;
    });
    await waitFor(() => expect(result.current.isRefreshing).toBe(false));
    expect(result.current.expectedVersion).toBe(4);
  });

  it('rejects mixed page versions without publishing partial data', async () => {
    adminAutoScheduleService.getPreview
      .mockResolvedValueOnce(previewResponse({ items: [first], totalElements: 1 }))
      .mockImplementationOnce((_id, params) => previewResponse({
        version: params.page === 0 ? 4 : 5,
        items: [{ ...first, itemPublicId: `refresh-${params.page}` }],
        page: params.page,
        totalPages: 2,
        totalElements: 2,
      }))
      .mockImplementationOnce((_id, params) => previewResponse({
        version: params.page === 0 ? 4 : 5,
        items: [{ ...first, itemPublicId: `refresh-${params.page}` }],
        page: params.page,
        totalPages: 2,
        totalElements: 2,
      }));

    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', {}));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.expectedVersion).toBe(3);

    await act(async () => {
      await result.current.fetchPreview();
    });

    expect(result.current.expectedVersion).toBe(3);
    expect(result.current.items.map(item => item.itemPublicId)).toEqual(['item-1']);
    expect(result.current.snapshotError.code).toBe('VERSION_MISMATCH');
    expect(result.current.capabilities.canSelect).toBe(false);
  });
});

describe('useAutoSchedulePreview selection compatibility', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminAutoScheduleService.getPreview.mockResolvedValue(previewResponse());
    adminAutoScheduleService.updateSelections.mockResolvedValue({
      success: true,
      data: summary(4),
    });
    adminAutoScheduleService.applyPreview.mockResolvedValue({ success: true });
  });

  it('retains backend defaults and sends unchanged single and full-array bulk payloads', async () => {
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', {}));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(Array.from(result.current.selectedItemIds)).toEqual(['item-1']);

    await act(async () => result.current.handleToggleSelection('item-2', false));
    expect(adminAutoScheduleService.updateSelections).toHaveBeenNthCalledWith(1, 'preview-1', {
      expectedVersion: 3,
      items: [{ itemPublicId: 'item-2', selected: true }],
    });

    await act(async () => result.current.handleBulkSelection(['item-2']));
    expect(adminAutoScheduleService.updateSelections).toHaveBeenNthCalledWith(2, 'preview-1', {
      expectedVersion: 4,
      items: [
        { itemPublicId: 'item-1', selected: false },
        { itemPublicId: 'item-2', selected: true },
      ],
    });
  });

  it('replaces one selected showtime atomically without updating unrelated candidates', async () => {
    const triggerToast = vi.fn();
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    let replaced;
    await act(async () => {
      replaced = await result.current.handleReplaceSelection('item-1', 'item-2');
    });

    expect(replaced).toBe(true);
    expect(adminAutoScheduleService.updateSelections).toHaveBeenCalledWith('preview-1', {
      expectedVersion: 3,
      items: [
        { itemPublicId: 'item-1', selected: false },
        { itemPublicId: 'item-2', selected: true },
      ],
    });
    expect(Array.from(result.current.selectedItemIds)).toEqual(['item-2']);
    expect(triggerToast).toHaveBeenCalledWith(
      'Đã thay suất và kiểm tra lại xung đột thành công.',
      'success',
    );
  });

  it('uses the complete snapshot for canonical occupancy guards', async () => {
    const triggerToast = vi.fn();
    const overlapping = { ...second, startTime: '2026-07-24T11:05:00Z' };
    adminAutoScheduleService.getPreview.mockResolvedValue(previewResponse({ items: [first, overlapping] }));
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => result.current.handleToggleSelection('item-2', false));

    expect(adminAutoScheduleService.updateSelections).not.toHaveBeenCalled();
    expect(triggerToast).toHaveBeenCalledWith(
      'Suất đề xuất bị trùng thời gian sử dụng phòng với một suất đã chọn.',
      'error',
    );
  });

  it('allows bulk deselection of malformed selected data and blocks additions beside it', async () => {
    const malformedSelected = { ...first, occupancyEndTime: null, selected: true };
    adminAutoScheduleService.getPreview.mockResolvedValue(previewResponse({
      items: [malformedSelected, second],
    }));
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', {}));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => result.current.handleBulkSelection(['item-1', 'item-2']));
    expect(adminAutoScheduleService.updateSelections).not.toHaveBeenCalled();

    await act(async () => result.current.handleBulkSelection([]));
    expect(adminAutoScheduleService.updateSelections).toHaveBeenCalledWith('preview-1', {
      expectedVersion: 3,
      items: [
        { itemPublicId: 'item-1', selected: false },
        { itemPublicId: 'item-2', selected: false },
      ],
    });
  });

  it('restores authoritative selection state after a backend rejection and refreshes', async () => {
    const triggerToast = vi.fn();
    adminAutoScheduleService.updateSelections.mockRejectedValue({
      errorCode: 'AUTO_SCHEDULE_SELECTION_OVERLAP',
    });
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => result.current.handleToggleSelection('item-2', false));

    expect(triggerToast).toHaveBeenCalledWith(
      'Không thể lưu lựa chọn vì có các suất chiếm cùng phòng bị trùng thời gian.',
      'error',
    );
    await waitFor(() => expect(adminAutoScheduleService.getPreview).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(Array.from(result.current.selectedItemIds)).toEqual(['item-1']));
  });

  it('uses the auto-selection action wording when a bulk update fails', async () => {
    const triggerToast = vi.fn();
    adminAutoScheduleService.updateSelections.mockRejectedValue(new Error('network'));
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => result.current.handleBulkSelection(['item-2']));

    expect(triggerToast).toHaveBeenCalledWith(
      'Không thể tự chọn lịch không xung đột. Đang tải lại dữ liệu.',
      'error',
    );
    await waitFor(() => expect(adminAutoScheduleService.getPreview).toHaveBeenCalledTimes(2));
  });

  it('keeps the apply payload and expected-version contract unchanged', async () => {
    const randomUuid = vi.spyOn(globalThis.crypto, 'randomUUID').mockReturnValue('idempotency-1');
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', {}));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    await waitFor(() => expect(result.current.pricingPreflight?.complete).toBe(true));

    await act(async () => result.current.handleApply());

    expect(adminAutoScheduleService.applyPreview).toHaveBeenCalledWith('preview-1', {
      expectedVersion: 3,
      idempotencyKey: 'idempotency-1',
    });
    randomUuid.mockRestore();
  });

  it('exposes grouped pricing preflight diagnostics from an atomic apply rejection', async () => {
    adminAutoScheduleService.applyPreview.mockRejectedValue({
      errorCode: 'PRICING_INCOMPLETE',
      message: 'Không thể áp dụng lịch vì một hoặc nhiều ứng viên chưa có giá đầy đủ.',
      data: {
        complete: false,
        totalCandidateCount: 1,
        completeCandidateCount: 0,
        incompleteCandidateCount: 1,
        ambiguousCandidateCount: 0,
        reasonGroups: [{
          reasonCode: 'PRICING_INCOMPLETE',
          count: 1,
          affectedDates: ['2026-09-30'],
          auditoriums: [{ publicId: 'room-1', name: 'Phòng 1' }],
          seatTypes: [{ publicId: 'vip', code: 'VIP', name: 'Ghế VIP' }],
        }],
      },
    });
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', {}));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    await waitFor(() => expect(result.current.pricingPreflight?.complete).toBe(true));

    await act(async () => result.current.handleApply());

    expect(result.current.pricingPreflight).toMatchObject({
      complete: false,
      incompleteCandidateCount: 1,
    });
    expect(result.current.pricingPreflight.reasonGroups[0].seatTypes[0].name).toBe('Ghế VIP');
  });

  it('reuses one apply idempotency key after transport failure and rotates it after success', async () => {
    const randomUuid = vi.spyOn(globalThis.crypto, 'randomUUID')
      .mockReturnValueOnce('idempotency-1')
      .mockReturnValueOnce('idempotency-2');
    adminAutoScheduleService.applyPreview
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce({
        success: true,
        data: { createdShowtimeCount: 1, skippedItemCount: 1 },
      })
      .mockResolvedValueOnce({
        success: true,
        data: { createdShowtimeCount: 1, skippedItemCount: 1 },
      });
    const onSuccess = vi.fn();
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { onSuccess }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    await waitFor(() => expect(result.current.pricingPreflight?.complete).toBe(true));

    await act(async () => result.current.handleApply());
    await act(async () => result.current.handleApply());
    await act(async () => result.current.handleApply());

    expect(adminAutoScheduleService.applyPreview.mock.calls[0][1].idempotencyKey).toBe('idempotency-1');
    expect(adminAutoScheduleService.applyPreview.mock.calls[1][1].idempotencyKey).toBe('idempotency-1');
    expect(adminAutoScheduleService.applyPreview.mock.calls[2][1].idempotencyKey).toBe('idempotency-2');
    expect(onSuccess).toHaveBeenCalledWith({
      createdShowtimeCount: 1,
      skippedItemCount: 1,
    });
    randomUuid.mockRestore();
  });
});
