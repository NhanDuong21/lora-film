import { act, renderHook, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminAutoScheduleService from '@/features/scheduling/admin/services/adminAutoScheduleService';
import useAutoScheduleHistory from './useAutoScheduleHistory';

vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: { getCinemas: vi.fn() },
}));

vi.mock('@/features/scheduling/admin/services/adminAutoScheduleService', () => ({
  default: { getPreviewHistory: vi.fn() },
}));

const page = (data, overrides = {}) => ({
  success: true,
  data: {
    data,
    pageNo: 0,
    pageSize: 10,
    totalElements: data.length,
    totalPages: data.length ? 1 : 0,
    last: true,
    ...overrides,
  },
});

const wrapper = ({ children }) => <MemoryRouter>{children}</MemoryRouter>;

describe('useAutoScheduleHistory', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminCinemaService.getCinemas.mockResolvedValue(page([], { totalPages: 0 }));
    adminAutoScheduleService.getPreviewHistory.mockResolvedValue(page([]));
  });

  it('strictly loads the admin page envelope and exact default API parameters', async () => {
    const { result } = renderHook(() => useAutoScheduleHistory(), { wrapper });

    await waitFor(() => expect(result.current.isInitialLoading).toBe(false));
    expect(adminAutoScheduleService.getPreviewHistory).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sort: 'createdAt,desc',
    });
    expect(result.current.previews).toEqual([]);

    adminAutoScheduleService.getPreviewHistory.mockResolvedValueOnce({
      success: true,
      data: { content: [] },
    });
    await act(async () => result.current.fetchHistory());
    expect(result.current.error).toMatch(/không đúng định dạng/);
  });

  it('maps URL filters, resets page on changes, and suppresses reversed ranges', async () => {
    const { result } = renderHook(() => useAutoScheduleHistory(), { wrapper });
    await waitFor(() => expect(result.current.isInitialLoading).toBe(false));

    act(() => result.current.commitQuery({ status: 'APPLIED', page: 8 }));
    await waitFor(() => expect(adminAutoScheduleService.getPreviewHistory).toHaveBeenLastCalledWith({
      status: 'APPLIED',
      page: 0,
      size: 10,
      sort: 'createdAt,desc',
    }));

    const callCount = adminAutoScheduleService.getPreviewHistory.mock.calls.length;
    act(() => result.current.commitQuery({
      scheduleFrom: '2026-07-23',
      scheduleTo: '2026-07-22',
    }));
    await waitFor(() => expect(result.current.rangeError).toBeTruthy());
    expect(adminAutoScheduleService.getPreviewHistory).toHaveBeenCalledTimes(callCount);
  });

  it('prevents an older response from replacing a newer filter result', async () => {
    let resolveOld;
    let resolveNew;
    adminAutoScheduleService.getPreviewHistory
      .mockImplementationOnce(() => new Promise(resolve => { resolveOld = resolve; }))
      .mockImplementationOnce(() => new Promise(resolve => { resolveNew = resolve; }));

    const { result } = renderHook(() => useAutoScheduleHistory(), { wrapper });
    await waitFor(() => expect(adminAutoScheduleService.getPreviewHistory).toHaveBeenCalledTimes(1));

    act(() => result.current.commitQuery({ status: 'FAILED' }));
    await waitFor(() => expect(adminAutoScheduleService.getPreviewHistory).toHaveBeenCalledTimes(2));

    await act(async () => resolveNew(page([{ previewPublicId: 'new-result' }])));
    await waitFor(() => expect(result.current.previews[0]?.previewPublicId).toBe('new-result'));

    await act(async () => resolveOld(page([{ previewPublicId: 'old-result' }])));
    expect(result.current.previews[0]?.previewPublicId).toBe('new-result');
  });

  it('loads all cinema pages independently from history errors', async () => {
    adminAutoScheduleService.getPreviewHistory.mockRejectedValue(new Error('history unavailable'));
    adminCinemaService.getCinemas
      .mockResolvedValueOnce(page([{ publicId: 'cinema-1', name: 'Rạp 1' }], { totalPages: 2 }))
      .mockResolvedValueOnce(page([{ publicId: 'cinema-2', name: 'Rạp 2' }], { pageNo: 1, totalPages: 2 }));

    const { result } = renderHook(() => useAutoScheduleHistory(), { wrapper });
    await waitFor(() => expect(result.current.isCinemaLoading).toBe(false));

    expect(result.current.cinemas).toHaveLength(2);
    expect(result.current.cinemaError).toBe('');
    expect(adminCinemaService.getCinemas).toHaveBeenNthCalledWith(1, {
      showDeleted: true,
      page: 0,
      size: 100,
      sort: 'name,asc',
    });
    expect(adminCinemaService.getCinemas).toHaveBeenNthCalledWith(2, {
      showDeleted: true,
      page: 1,
      size: 100,
      sort: 'name,asc',
    });
  });
});
