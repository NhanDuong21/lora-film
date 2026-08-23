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
    await adminPromotionService.transitionCampaign('campaign-1', 'ACTIVATE', 'Ready', 4);
    await adminPromotionService.approveCampaign('campaign-1', 'Approved');
    await adminPromotionService.reviewCampaignLegal('campaign-1', 'PASSED', 'Legal checked');

    expect(apiClient.patch).toHaveBeenCalledWith(
      '/api/admin/promotion-campaigns/campaign-1/status', null,
      { params: { action: 'ACTIVATE', comment: 'Ready', expectedVersion: 4 } }
    );
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotion-campaigns/campaign-1/approve', { comment: 'Approved' }
    );
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotion-campaigns/campaign-1/legal-review',
      { status: 'PASSED', comment: 'Legal checked' }
    );
  });

  it('uses unified promotion lifecycle and wallet issue contracts', async () => {
    const payload = { promotionType: 'AUTO', actionsJson: { discountType: 'FULL_DISCOUNT' } };
    await adminPromotionService.createPromotion(payload);
    await adminPromotionService.activatePromotion('promotion-1');
    await adminPromotionService.issuePromotion('promotion-1', ['user-1', 'user-2']);

    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/promotions', payload);
    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/promotions/promotion-1/activate');
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotions/promotion-1/issue',
      { userPublicIds: ['user-1', 'user-2'] }
    );
  });

  it('loads a clone draft without calling a mutating endpoint', async () => {
    await adminPromotionService.getCloneDraft('promotion-1');

    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/admin/promotions/promotion-1/clone-draft'
    );
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  it('loads reservations through the unified admin route', async () => {
    await adminPromotionService.searchReservations({ status: 'ACTIVE' });
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/reservations', {
      params: { status: 'ACTIVE' },
    });
  });

  it('loads promotion and booking operations summaries', async () => {
    await adminPromotionService.getPromotionMonitoring();
    await adminPromotionService.getBookingMonitoring();

    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/admin/promotion-monitoring/summary',
      { params: { includeTestData: false } }
    );
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/monitoring/summary');
  });

  it('carries the UAT scope through automation, monitoring and anomaly contracts', async () => {
    await adminPromotionService.getPromotionPlaybooks(true);
    await adminPromotionService.getPromotionRuns(true);
    await adminPromotionService.getPromotionRun('run-1', true);
    await adminPromotionService.getPromotionAnomalyCases(true);
    await adminPromotionService.resolvePromotionAnomaly('case-1', 'TEST_DATA', 'UAT walkthrough complete');

    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/promotion-playbooks', {
      params: { includeTestData: true },
    });
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/promotion-runs', {
      params: { includeTestData: true },
    });
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/promotion-runs/run-1', {
      params: { includeTestData: true },
    });
    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/promotion-anomaly-cases', {
      params: { includeTestData: true },
    });
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotion-anomaly-cases/case-1/resolve',
      { resolution: 'TEST_DATA', resolutionNote: 'UAT walkthrough complete' }
    );
  });
});
