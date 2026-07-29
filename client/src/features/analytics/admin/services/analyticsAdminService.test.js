import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  acknowledgeAnalyticsAlert,
  getAnalyticsDashboard,
  getMovieKpis,
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

  it('returns an empty list when a performance response has no data', async () => {
    apiClient.get.mockResolvedValue({ data: { success: true } });
    await expect(getMovieKpis()).resolves.toEqual([]);
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
