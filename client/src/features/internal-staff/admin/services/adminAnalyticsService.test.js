import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import { getTopMoviesByRevenue } from './adminAnalyticsService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn()
  }
}));

describe('adminAnalyticsService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads real top-movie revenue data through the Gateway contract', async () => {
    apiClient.get.mockResolvedValue({
      data: {
        data: {
          currency: 'VND',
          movies: [{ movieId: 91 }]
        }
      }
    });

    const response = await getTopMoviesByRevenue({
      limit: 10,
      startDate: '2026-07-01',
      endDate: '2026-07-27'
    });

    expect(response.movies).toEqual([{ movieId: 91 }]);
    expect(apiClient.get).toHaveBeenCalledWith('/api/analytics/movies/top', {
      params: {
        metric: 'REVENUE',
        direction: 'desc',
        limit: 10,
        startDate: '2026-07-01',
        endDate: '2026-07-27'
      }
    });
  });
});
