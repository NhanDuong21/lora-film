import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  getTicketCheckerSummary, getTicketScanHistory, saveTicketGateHandoff, scanTicket,
} from './employeeTicketCheckerService';

vi.mock('@/services/apiClient', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));

describe('employeeTicketCheckerService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('gửi mã vé và cửa soát tới API nghiệp vụ', async () => {
    apiClient.post.mockResolvedValue({ data: { data: { result: 'ADMITTED' } } });

    await expect(scanTicket({ code: 'QR-001', gateLabel: 'Cửa phòng 01' }))
      .resolves.toEqual({ result: 'ADMITTED' });
    expect(apiClient.post).toHaveBeenCalledWith('/api/employee/ticket-operations/scan', {
      code: 'QR-001', gateLabel: 'Cửa phòng 01',
    });
  });

  it('truyền đúng ngày và bộ lọc lịch sử', async () => {
    apiClient.get.mockResolvedValue({ data: { data: [] } });

    await getTicketCheckerSummary('2026-08-10');
    await getTicketScanHistory({ date: '2026-08-10', result: 'ALREADY_USED' });

    expect(apiClient.get).toHaveBeenNthCalledWith(1, '/api/employee/ticket-operations/summary', {
      params: { date: '2026-08-10' },
    });
    expect(apiClient.get).toHaveBeenNthCalledWith(2, '/api/employee/ticket-operations/history', {
      params: { date: '2026-08-10', result: 'ALREADY_USED' },
    });
  });

  it('lưu biên bản bàn giao theo ngày', async () => {
    apiClient.post.mockResolvedValue({ data: { data: { publicId: 'handoff-1' } } });
    const payload = { gateLabel: 'Cửa phòng 01', unresolvedIncidents: 0, note: 'Đã bàn giao đủ' };

    await saveTicketGateHandoff(payload, '2026-08-10');

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/employee/ticket-operations/handoffs', payload, { params: { date: '2026-08-10' } },
    );
  });
});
