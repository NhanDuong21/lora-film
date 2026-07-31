import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  acknowledgeAnalyticsAlert,
  getAnalyticsDashboard,
  getCinemaDirectory,
  updateAnalyticsRecommendation
} from './analyticsAdminService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn()
  }
}));

describe('analyticsAdminService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('uses the read-only dashboard endpoint with an explicit period', async () => {
    apiClient.get.mockResolvedValue({ data: { data: { summary: {} } } });

    await getAnalyticsDashboard({ startDate: '2026-07-01', endDate: '2026-07-29' });

    expect(apiClient.get).toHaveBeenCalledWith('/api/analytics/dashboard', {
      params: { startDate: '2026-07-01', endDate: '2026-07-29' }
    });
  });

  it('adds cinemaKey only when a cinema is selected', async () => {
    apiClient.get.mockResolvedValue({ data: { data: { summary: {} } } });

    await getAnalyticsDashboard({
      startDate: '2026-07-01',
      endDate: '2026-07-29',
      cinemaKey: 'cinema-1'
    });

    expect(apiClient.get).toHaveBeenCalledWith('/api/analytics/dashboard', {
      params: {
        startDate: '2026-07-01',
        endDate: '2026-07-29',
        cinemaKey: 'cinema-1'
      }
    });
  });

  it('resolves cinema identifiers through the cinema directory and detail API', async () => {
    apiClient.get.mockImplementation(url => {
      if (url === '/api/admin/cinemas') {
        return Promise.resolve({
          data: {
            data: {
              data: [{ publicId: 'cinema-1', name: 'LoraFilm Quận 1' }]
            }
          }
        });
      }
      return Promise.resolve({
        data: {
          data: { publicId: 'cinema-2', name: 'LoraFilm Thủ Đức' }
        }
      });
    });

    const cinemas = await getCinemaDirectory(['cinema-1', 'cinema-2']);

    expect(apiClient.get).toHaveBeenNthCalledWith(1, '/api/admin/cinemas', {
      params: { page: 0, size: 200, showDeleted: false }
    });
    expect(apiClient.get).toHaveBeenNthCalledWith(2, '/api/cinemas/cinema-2');
    expect(cinemas).toEqual([
      { publicId: 'cinema-1', name: 'LoraFilm Quận 1' },
      { publicId: 'cinema-2', name: 'LoraFilm Thủ Đức' }
    ]);
  });

  it('updates alert and recommendation lifecycle through explicit endpoints', async () => {
    apiClient.patch.mockResolvedValue({ data: { data: { status: 'ACCEPTED' } } });

    await acknowledgeAnalyticsAlert(12);
    await updateAnalyticsRecommendation(9, 'ACCEPTED');

    expect(apiClient.patch).toHaveBeenNthCalledWith(
      1,
      '/api/analytics/alerts/12/acknowledge'
    );
    expect(apiClient.patch).toHaveBeenNthCalledWith(
      2,
      '/api/analytics/recommendations/9/status',
      { status: 'ACCEPTED' }
    );
  });
});
