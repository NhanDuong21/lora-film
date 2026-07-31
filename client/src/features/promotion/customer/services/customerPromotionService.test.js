import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import customerPromotionService from './customerPromotionService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('customerPromotionService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads the authenticated customer voucher wallet', async () => {
    apiClient.get.mockResolvedValue({
      data: { data: { content: [{ publicId: 'voucher-1' }], totalElements: 1 } },
    });

    const result = await customerPromotionService.getMyVouchers({
      page: 0,
      size: 50,
      sort: 'validTo,asc',
    });

    expect(apiClient.get).toHaveBeenCalledWith('/api/customers/me/vouchers', {
      params: { page: 0, size: 50, sort: 'validTo,asc' },
    });
    expect(result.content).toHaveLength(1);
  });
});
