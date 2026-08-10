import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import scoreAdminService, { normalizeScoreDashboardStats } from './scoreAdminService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('scoreAdminService dashboard adapter', () => {
  beforeEach(() => vi.clearAllMocks());

  it('maps the current backend reconciliation field names to the dashboard view model', async () => {
    apiClient.get.mockResolvedValue({
      data: {
        data: {
          totalMembers: 18,
          totalPointsEarned: 12500,
          pendingReconciliations: 3,
          lastReconciliationDate: '2026-08-03T07:00:00Z',
        },
      },
    });

    await expect(scoreAdminService.getDashboardStats()).resolves.toMatchObject({
      totalMembers: 18,
      totalPointsEarned: 12500,
      totalPointsHeld: 0,
      pendingReconciliationMismatches: 3,
      lastReconciliationTime: '2026-08-03T07:00:00Z',
    });
  });

  it('returns finite numeric defaults for absent or malformed values', () => {
    expect(normalizeScoreDashboardStats({ totalMembers: 'not-a-number' })).toEqual({
      totalMembers: 0,
      totalPointsEarned: 0,
      totalPointsRedeemed: 0,
      totalPointsHeld: 0,
      totalPointsExpired: 0,
      silverMembers: 0,
      goldMembers: 0,
      diamondMembers: 0,
      pendingReconciliationMismatches: 0,
      lastReconciliationBatch: 'N/A',
      lastReconciliationTime: null,
    });
  });
});
