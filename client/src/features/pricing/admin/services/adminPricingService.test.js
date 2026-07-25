import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import adminPricingService from './adminPricingService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('adminPricingService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiClient.get.mockResolvedValue({ data: { success: true } });
    apiClient.post.mockResolvedValue({ data: { success: true } });
    apiClient.put.mockResolvedValue({ data: { success: true } });
  });

  it('uses the policy search and lifecycle contracts', async () => {
    await adminPricingService.searchPolicies({ cinema: 'cinema-1', status: 'ACTIVE' });
    await adminPricingService.activatePolicy('policy-1', 3);
    await adminPricingService.deactivatePolicy('policy-1', 4, 'Replaced');

    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/pricing/policies', {
      params: { cinema: 'cinema-1', status: 'ACTIVE' },
    });
    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/pricing/policies/policy-1/activate', {
      expectedVersion: 3,
    });
    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/pricing/policies/policy-1/deactivate', {
      expectedVersion: 4,
      reason: 'Replaced',
    });
  });

  it('sends preview facts to the backend resolver', async () => {
    const request = {
      cinemaId: 'cinema-1',
      auditoriumId: 'auditorium-1',
      startTime: '2026-07-25T03:00:00Z',
    };

    await adminPricingService.previewResolution(request);

    expect(apiClient.post).toHaveBeenCalledWith('/api/admin/pricing/resolve-preview', request);
  });
});
