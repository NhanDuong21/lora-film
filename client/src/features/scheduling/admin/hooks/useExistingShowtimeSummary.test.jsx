import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminShowtimeService from '../services/adminShowtimeService';
import useExistingShowtimeSummary from './useExistingShowtimeSummary';

vi.mock('../services/adminShowtimeService');

describe('useExistingShowtimeSummary', () => {
  beforeEach(() => vi.clearAllMocks());

  it('counts operational existing shows per date and excludes the current preview batch', async () => {
    adminShowtimeService.getShowtimes.mockImplementation(({ date }) => Promise.resolve({
      success: true,
      data: {
        data: date === '2026-08-04'
          ? [
            { status: 'DRAFT', batchId: 'old-preview' },
            { status: 'CANCELLED', batchId: 'old-preview' },
            { status: 'DRAFT', batchId: 'current-preview' },
          ]
          : [{ status: 'OPEN_FOR_BOOKING', batchId: null }],
        totalPages: 1,
      },
    }));

    const { result } = renderHook(() => useExistingShowtimeSummary({
      cinemaSlug: 'lora-cinema',
      scheduleFrom: '2026-08-04',
      scheduleTo: '2026-08-05',
      excludeBatchId: 'current-preview',
    }));

    await waitFor(() => expect(result.current.countsByDate).toEqual({
      '2026-08-04': 1,
      '2026-08-05': 1,
    }));
    expect(result.current.countsByDate).toEqual({
      '2026-08-04': 1,
      '2026-08-05': 1,
    });
    expect(result.current.totalExisting).toBe(2);
    expect(adminShowtimeService.getShowtimes).toHaveBeenCalledTimes(2);
  });

  it('does not query until cinema and date range are known', () => {
    const { result } = renderHook(() => useExistingShowtimeSummary({}));
    expect(result.current.countsByDate).toEqual({});
    expect(result.current.isLoading).toBe(false);
    expect(adminShowtimeService.getShowtimes).not.toHaveBeenCalled();
  });
});
