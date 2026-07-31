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
    apiClient.delete.mockResolvedValue(response);
    apiClient.get.mockResolvedValue(response);
    apiClient.patch.mockResolvedValue(response);
    apiClient.post.mockResolvedValue(response);
    apiClient.put.mockResolvedValue(response);
  });

  it('searches campaigns with only populated query parameters', async () => {
    const result = await adminPromotionService.searchCampaigns({
      keyword: 'summer',
      status: '',
      page: 0,
      size: 20,
      sort: undefined,
    });

    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/promotion-campaigns', {
      params: { keyword: 'summer', page: 0, size: 20 },
    });
    expect(result).toEqual({ publicId: 'promotion-1' });
  });

  it('uses the campaign workflow and approval contracts', async () => {
    await adminPromotionService.transitionCampaign('campaign-1', 'ACTIVATE', 'Ready');
    await adminPromotionService.approveCampaign('campaign-1', 'Approved');
    await adminPromotionService.reviewCampaignLegal('campaign-1', {
      approved: true,
      comment: 'Legal checked',
    });

    expect(apiClient.patch).toHaveBeenCalledWith(
      '/api/admin/promotion-campaigns/campaign-1/status',
      null,
      { params: { action: 'ACTIVATE', comment: 'Ready' } }
    );
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotion-campaigns/campaign-1/approve',
      { comment: 'Approved' }
    );
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotion-campaigns/campaign-1/legal-review',
      { approved: true, comment: 'Legal checked' }
    );
  });

  it('uses the rule clone and preview contracts', async () => {
    const cloneRequest = { name: 'Weekend copy', campaignId: 'campaign-2' };
    const previewRequest = { conditions: {}, actions: [] };

    await adminPromotionService.cloneRule('rule-1', cloneRequest);
    await adminPromotionService.previewRule(previewRequest);

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotion-rules/rule-1/clone',
      cloneRequest
    );
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/promotion-rules/preview',
      previewRequest
    );
  });

  it('exports coupons as a blob and preserves supported filters', async () => {
    const csv = new Blob(['code,status'], { type: 'text/csv' });
    apiClient.get.mockResolvedValueOnce({ data: csv });

    const result = await adminPromotionService.exportCoupons({
      status: 'ACTIVE',
      campaignId: '',
    });

    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/coupons/export', {
      params: { status: 'ACTIVE' },
      responseType: 'blob',
    });
    expect(result).toBe(csv);
  });

  it('wraps batch vouchers and sends voucher lifecycle operations', async () => {
    const vouchers = [{ userId: 'user-1', value: 50000 }];

    await adminPromotionService.batchIssueVouchers(vouchers);
    await adminPromotionService.revokeVoucher('voucher-1', 'Customer request');
    await adminPromotionService.extendVoucher('voucher-1', { validTo: '2026-12-31T00:00:00Z' });

    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/vouchers/batch', { vouchers });
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/vouchers/voucher-1/revoke',
      null,
      { params: { reason: 'Customer request' } }
    );
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/admin/vouchers/voucher-1/extend',
      { validTo: '2026-12-31T00:00:00Z' }
    );
  });

  it('updates compensation vouchers through the documented resource', async () => {
    const payload = { status: 'CANCELLED', reason: 'Duplicate issue' };

    await adminPromotionService.updateCompensation('compensation-1', payload);

    expect(apiClient.put).toHaveBeenCalledWith(
      '/api/admin/compensation-vouchers/compensation-1',
      payload
    );
  });
});
