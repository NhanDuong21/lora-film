import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import managerPromotionService from './managerPromotionService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('managerPromotionService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('uses only manager-scoped promotion endpoints with the selected cinema', async () => {
    apiClient.get.mockResolvedValue({ data: { data: [] } });

    await managerPromotionService.getCampaigns('cinema-a');
    await managerPromotionService.getAutomations('cinema-a');
    await managerPromotionService.getDistributionOptions('cinema-a');
    await managerPromotionService.getIncidents('cinema-a');

    expect(apiClient.get.mock.calls.map(call => call[0])).toEqual([
      '/api/manager/promotions/campaigns',
      '/api/manager/promotions/automations',
      '/api/manager/promotions/distribution-options',
      '/api/manager/promotions/incidents',
    ]);
    expect(apiClient.get).not.toHaveBeenCalledWith(
      expect.stringContaining('/api/admin/'), expect.anything(),
    );
    apiClient.get.mock.calls.forEach(([, config]) => {
      expect(config.params).toEqual({ cinemaPublicId: 'cinema-a' });
    });
  });

  it('records the cinema scope in a local benefit issuance request', async () => {
    apiClient.post.mockResolvedValue({ data: { data: { issuedCount: 1 } } });

    await managerPromotionService.issueBenefit('cinema-a', 'promotion-1', ['customer-1']);

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/manager/promotions/distribution-options/promotion-1/issue',
      { userPublicIds: ['customer-1'] },
      { params: { cinemaPublicId: 'cinema-a' } },
    );
  });
});
