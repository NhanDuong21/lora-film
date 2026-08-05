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
    expect(adminShowtimeService.getShowtimes).toHaveBeenLastCalledWith({ page: 0, size: 100 });

    act(() => {
      result.current.setSource('AUTO');
      result.current.setBatchId('preview-1');
    });
    await waitFor(() => expect(result.current.source).toBe('AUTO'));
    await act(async () => result.current.fetchShowtimes());
    expect(adminShowtimeService.getShowtimes).toHaveBeenLastCalledWith({
      page: 0,
      size: 100,
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

  it('uses URL-backed initial filters on the first request', async () => {
    const { result } = renderHook(() => useAdminShowtimes({
      initialFilters: {
        cinemaSlug: 'lora-cinema',
        date: '2026-08-04',
        source: 'AUTO',
        batchId: 'preview-initial',
        status: 'DRAFT',
      },
    }));

    await act(async () => result.current.fetchShowtimes());

    expect(adminShowtimeService.getShowtimes).toHaveBeenLastCalledWith({
      page: 0,
      size: 100,
      cinemaSlug: 'lora-cinema',
      date: '2026-08-04',
      source: 'AUTO',
      batchId: 'preview-initial',
      status: 'DRAFT',
    });
  });

  it('ignores a slower stale response after a newer filtered request completes', async () => {
    let resolveUnfiltered;
    let resolveFiltered;
    adminShowtimeService.getShowtimes
      .mockImplementationOnce(() => new Promise(resolve => { resolveUnfiltered = resolve; }))
      .mockImplementationOnce(() => new Promise(resolve => { resolveFiltered = resolve; }));
    const { result } = renderHook(() => useAdminShowtimes());

    let firstRequest;
    await act(async () => {
      firstRequest = result.current.fetchShowtimes();
    });
    act(() => {
      result.current.setSource('AUTO');
      result.current.setBatchId('preview-1');
    });
    await waitFor(() => expect(result.current.batchId).toBe('preview-1'));

    let secondRequest;
    await act(async () => {
      secondRequest = result.current.fetchShowtimes();
    });
    await act(async () => {
      resolveFiltered({
        success: true,
        data: { data: [{ publicId: 'filtered-showtime' }], totalPages: 1, totalElements: 1 },
      });
      await secondRequest;
    });
    expect(result.current.showtimes).toEqual([{ publicId: 'filtered-showtime' }]);

    await act(async () => {
      resolveUnfiltered({
        success: true,
        data: { data: [{ publicId: 'stale-unfiltered-showtime' }], totalPages: 1, totalElements: 1 },
      });
      await firstRequest;
    });
    expect(result.current.showtimes).toEqual([{ publicId: 'filtered-showtime' }]);
  });
});
