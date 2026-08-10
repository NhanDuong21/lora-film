import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import managerTicketControlService from './managerTicketControlService';

vi.mock('@/services/apiClient', () => ({
  default: { get: vi.fn() },
}));

describe('managerTicketControlService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('keeps the manager request inside the selected cinema scope', async () => {
    apiClient.get.mockResolvedValue({ data: { data: { totalScans: 3 } } });

    await managerTicketControlService.getSummary('cinema-1', '2026-08-10');

    expect(apiClient.get).toHaveBeenCalledWith('/api/manager/ticket-operations/summary', {
      params: { cinemaPublicId: 'cinema-1', date: '2026-08-10' },
    });
  });

  it('returns an empty list when no handoff is available', async () => {
    apiClient.get.mockResolvedValue({ data: { data: null } });

    await expect(managerTicketControlService.getHandoffs('cinema-1', '2026-08-10'))
      .resolves.toEqual([]);
  });
});
