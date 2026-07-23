import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminShowtimeService from '../services/adminShowtimeService';
import useAdminShowtimes from './useAdminShowtimes';

vi.mock('@/features/facilities/admin/services/adminCinemaService');
vi.mock('@/features/catalog/admin/services/adminMovieService');
vi.mock('../services/adminShowtimeService');

describe('useAdminShowtimes source and batch filters', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminCinemaService.getCinemas.mockResolvedValue({ success: true, data: { data: [] } });
    adminMovieService.getMovies.mockResolvedValue({ success: true, data: { data: [] } });
    adminShowtimeService.getShowtimes.mockResolvedValue({
      success: true,
      data: { data: [], totalPages: 0, totalElements: 0 },
    });
  });

  it('rebuilds and refetches params whenever source or batchId changes', async () => {
    const { result } = renderHook(() => useAdminShowtimes());

    await act(async () => result.current.fetchShowtimes());
    expect(adminShowtimeService.getShowtimes).toHaveBeenLastCalledWith({ page: 0, size: 10 });

    act(() => {
      result.current.setSource('AUTO');
      result.current.setBatchId('preview-1');
    });
    await waitFor(() => expect(result.current.source).toBe('AUTO'));
    await act(async () => result.current.fetchShowtimes());
    expect(adminShowtimeService.getShowtimes).toHaveBeenLastCalledWith({
      page: 0,
      size: 10,
      source: 'AUTO',
      batchId: 'preview-1',
    });

    act(() => result.current.setBatchId('preview-2'));
    await waitFor(() => expect(result.current.batchId).toBe('preview-2'));
    await act(async () => result.current.fetchShowtimes());
    expect(adminShowtimeService.getShowtimes).toHaveBeenLastCalledWith(expect.objectContaining({
      source: 'AUTO',
      batchId: 'preview-2',
    }));
  });
});
