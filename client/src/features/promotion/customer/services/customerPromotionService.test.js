import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import customerPromotionService from './customerPromotionService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('customerPromotionService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads and flattens the authenticated customer promotion wallet', async () => {
    apiClient.get.mockResolvedValue({
      data: { data: { content: [{ publicId: 'wallet-1', status: 'AVAILABLE', promotion: { publicId: 'promotion-1', name: 'Summer' } }], totalElements: 1 } },
    });

    const result = await customerPromotionService.getMyPromotions({
      page: 0,
      size: 50,
      sort: 'validTo,asc',
    });

    expect(apiClient.get).toHaveBeenCalledWith('/api/customers/me/promotions', {
      params: { page: 0, size: 50, sort: 'validTo,asc' },
    });
    expect(result.content).toHaveLength(1);
    expect(result.content[0]).toMatchObject({
      publicId: 'wallet-1',
      promotionPublicId: 'promotion-1',
      name: 'Summer',
    });
  });

  it('claims public vouchers and redeems private coupons', async () => {
    apiClient.post.mockResolvedValue({
      data: { data: { publicId: 'wallet-1', promotion: { publicId: 'promotion-1' } } },
    });

    await customerPromotionService.claimVoucher('promotion-1');
    await customerPromotionService.redeemCoupon('VIP2027');

    expect(apiClient.post).toHaveBeenCalledWith('/api/promotions/promotion-1/claim');
    expect(apiClient.post).toHaveBeenCalledWith('/api/promotions/coupons/redeem', { code: 'VIP2027' });
  });
});
