import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import adminPromotionService from './adminPromotionService';

vi.mock('@/services/apiClient', () => ({
  default: {
    delete: vi.fn(),
    get: vi.fn(),
    patch: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('adminPromotionService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    const response = { data: { data: { publicId: 'promotion-1' } } };
    Object.values(apiClient).forEach(mock => mock.mockResolvedValue(response));
  });

  it('searches campaigns with only populated query parameters', async () => {
    await adminPromotionService.searchCampaigns({ keyword: 'summer', status: '', page: 0, size: 20 });

    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/promotion-campaigns', {
      params: { keyword: 'summer', page: 0, size: 20 },
    });
  });

  it('uses campaign workflow, approval, and legal review contracts', async () => {
    await adminPromotionService.transitionCampaign('campaign-1', 'ACTIVATE', 'Ready');
    await adminPromotionService.approveCampaign('campaign-1', 'Approved');
    await adminPromotionService.reviewCampaignLegal('campaign-1', 'PASSED', 'Legal checked');

    expect(apiClient.patch).toHaveBeenCalledWith(
      '/api/admin/promotion-campaigns/campaign-1/status', null,
      { params: { action: 'ACTIVATE', comment: 'Ready' } }
    );
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotion-campaigns/campaign-1/approve', { comment: 'Approved' }
    );
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotion-campaigns/campaign-1/legal-review',
      { status: 'PASSED', comment: 'Legal checked', legalNotificationRef: null }
    );
  });

  it('uses unified promotion lifecycle and wallet issue contracts', async () => {
    const payload = { promotionType: 'AUTO', actionsJson: { discountType: 'FULL_DISCOUNT' } };
    await adminPromotionService.createPromotion(payload);
    await adminPromotionService.activatePromotion('promotion-1');
    await adminPromotionService.clonePromotion('promotion-1');
    await adminPromotionService.issuePromotion('promotion-1', ['user-1', 'user-2']);

    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/promotions', payload);
    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/promotions/promotion-1/activate');
    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/promotions/promotion-1/clone');
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotions/promotion-1/issue',
      { userPublicIds: ['user-1', 'user-2'] }
    );
  });

  it('loads reservations through the unified admin route', async () => {
    await adminPromotionService.searchReservations({ status: 'ACTIVE' });
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/reservations', {
      params: { status: 'ACTIVE' },
    });
  });
});
