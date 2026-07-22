import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminAutoScheduleService from '../services/adminAutoScheduleService';
import useAutoSchedulePreview from './useAutoSchedulePreview';

vi.mock('../services/adminAutoScheduleService', () => ({
  default: {
    getPreview: vi.fn(),
    updateSelections: vi.fn(),
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

const previewResponse = (version = 3) => ({
  success: true,
  data: {
    preview: { version, status: 'PREVIEWED', timezoneSnapshot: 'UTC' },
    items: { content: [first, second], totalPages: 1 },
  },
});

const previewResponseWithItems = (items, version = 3) => ({
  success: true,
  data: {
    preview: { version, status: 'PREVIEWED', timezoneSnapshot: 'UTC' },
    items: { content: items, totalPages: 1 },
  },
});

describe('useAutoSchedulePreview selection compatibility', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminAutoScheduleService.getPreview.mockResolvedValue(previewResponse());
    adminAutoScheduleService.updateSelections.mockResolvedValue({
      success: true,
      data: { version: 4, status: 'PREVIEWED', timezoneSnapshot: 'UTC' },
    });
    adminAutoScheduleService.applyPreview.mockResolvedValue({ success: true });
  });

  it('retains backend defaults and sends unchanged single and bulk payload shapes', async () => {
    const triggerToast = vi.fn();
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(Array.from(result.current.selectedItemIds)).toEqual(['item-1']);

    await act(async () => {
      await result.current.handleToggleSelection('item-2', false);
    });
    expect(adminAutoScheduleService.updateSelections).toHaveBeenNthCalledWith(1, 'preview-1', {
      expectedVersion: 3,
      items: [{ itemPublicId: 'item-2', selected: true }],
    });

    await act(async () => {
      await result.current.handleBulkSelection(['item-2']);
    });
    expect(adminAutoScheduleService.updateSelections).toHaveBeenNthCalledWith(2, 'preview-1', {
      expectedVersion: 4,
      items: [
        { itemPublicId: 'item-1', selected: false },
        { itemPublicId: 'item-2', selected: true },
      ],
    });
  });

  it('uses the canonical guard before optimistic selection submission', async () => {
    const triggerToast = vi.fn();
    const overlapping = {
      ...second,
      startTime: '2026-07-24T11:05:00Z',
    };
    adminAutoScheduleService.getPreview.mockResolvedValue({
      ...previewResponse(),
      data: { ...previewResponse().data, items: { content: [first, overlapping], totalPages: 1 } },
    });
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.handleToggleSelection('item-2', false);
    });

    expect(adminAutoScheduleService.updateSelections).not.toHaveBeenCalled();
    expect(triggerToast).toHaveBeenCalledWith(
      'Suất chiếu xung đột khoảng chiếm phòng với một suất đã chọn.',
      'error',
    );
  });

  it('rejects an overlapping bulk final set before optimistic mutation or API submission', async () => {
    const triggerToast = vi.fn();
    const unselectedFirst = { ...first, selected: false };
    const cleaningConflict = {
      ...second,
      startTime: '2026-07-24T11:05:00Z',
      selected: false,
    };
    adminAutoScheduleService.getPreview.mockResolvedValue(
      previewResponseWithItems([unselectedFirst, cleaningConflict]),
    );
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.selectedItemIds.size).toBe(0);

    await act(async () => {
      await result.current.handleBulkSelection(['item-1', 'item-2']);
    });

    expect(result.current.selectedItemIds.size).toBe(0);
    expect(adminAutoScheduleService.updateSelections).not.toHaveBeenCalled();
    expect(triggerToast).toHaveBeenCalledWith(
      'Suất chiếu xung đột khoảng chiếm phòng với một suất đã chọn.',
      'error',
    );
  });

  it('accepts an exactly adjacent bulk final set', async () => {
    const triggerToast = vi.fn();
    adminAutoScheduleService.getPreview.mockResolvedValue(
      previewResponseWithItems([{ ...first, selected: false }, second]),
    );
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.handleBulkSelection(['item-1', 'item-2']);
    });

    expect(adminAutoScheduleService.updateSelections).toHaveBeenCalledWith('preview-1', {
      expectedVersion: 3,
      items: [
        { itemPublicId: 'item-1', selected: true },
        { itemPublicId: 'item-2', selected: true },
      ],
    });
    expect(Array.from(result.current.selectedItemIds)).toEqual(['item-1', 'item-2']);
  });

  it('allows bulk deselection of malformed selected data and blocks adding beside it', async () => {
    const triggerToast = vi.fn();
    const malformedSelected = { ...first, occupancyEndTime: null, selected: true };
    const candidate = { ...second, selected: false };
    adminAutoScheduleService.getPreview.mockResolvedValue(
      previewResponseWithItems([malformedSelected, candidate]),
    );
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.handleBulkSelection(['item-1', 'item-2']);
    });
    expect(adminAutoScheduleService.updateSelections).not.toHaveBeenCalled();
    expect(Array.from(result.current.selectedItemIds)).toEqual(['item-1']);

    await act(async () => {
      await result.current.handleBulkSelection([]);
    });
    expect(adminAutoScheduleService.updateSelections).toHaveBeenCalledWith('preview-1', {
      expectedVersion: 3,
      items: [
        { itemPublicId: 'item-1', selected: false },
        { itemPublicId: 'item-2', selected: false },
      ],
    });
    expect(result.current.selectedItemIds.size).toBe(0);
  });

  it('surfaces selection failures and refreshes authoritative data', async () => {
    const triggerToast = vi.fn();
    adminAutoScheduleService.updateSelections.mockRejectedValue(new Error('version conflict'));
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.handleToggleSelection('item-2', false);
    });

    expect(triggerToast).toHaveBeenCalledWith(
      'Lỗi cập nhật trạng thái chọn. Đang tải lại dữ liệu.',
      'error',
    );
    await waitFor(() => expect(adminAutoScheduleService.getPreview).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(Array.from(result.current.selectedItemIds)).toEqual(['item-1']));
  });

  it('maps the authoritative backend overlap code and restores selected state', async () => {
    const triggerToast = vi.fn();
    adminAutoScheduleService.updateSelections.mockRejectedValue({
      errorCode: 'AUTO_SCHEDULE_SELECTION_OVERLAP',
    });
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.handleToggleSelection('item-2', false);
    });

    expect(triggerToast).toHaveBeenCalledWith(
      'Không thể lưu lựa chọn vì có các suất chiếm cùng phòng bị trùng thời gian.',
      'error',
    );
    await waitFor(() => expect(Array.from(result.current.selectedItemIds)).toEqual(['item-1']));
  });

  it('maps invalid-item selection failures and refreshes the preview', async () => {
    const triggerToast = vi.fn();
    adminAutoScheduleService.updateSelections.mockRejectedValue({
      errorCode: 'AUTO_SCHEDULE_INVALID_ITEM_SELECTION',
    });
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.handleToggleSelection('item-2', false);
    });

    expect(triggerToast).toHaveBeenCalledWith(
      'Không thể lưu lựa chọn vì ứng viên không còn hợp lệ. Đang tải lại dữ liệu.',
      'error',
    );
    await waitFor(() => expect(adminAutoScheduleService.getPreview).toHaveBeenCalledTimes(2));
  });

  it('keeps the apply payload shape and expected version', async () => {
    const randomUuid = vi.spyOn(globalThis.crypto, 'randomUUID').mockReturnValue('idempotency-1');
    const triggerToast = vi.fn();
    const { result } = renderHook(() => useAutoSchedulePreview('preview-1', { triggerToast }));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.handleApply();
    });

    expect(adminAutoScheduleService.applyPreview).toHaveBeenCalledWith('preview-1', {
      expectedVersion: 3,
      idempotencyKey: 'idempotency-1',
    });
    randomUuid.mockRestore();
  });
});
