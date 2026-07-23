import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import adminAutoScheduleService from './adminAutoScheduleService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('adminAutoScheduleService.getPreview', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiClient.get.mockResolvedValue({ data: { success: true } });
  });

  it('forwards pagination and AbortSignal without changing the endpoint contract', async () => {
    const controller = new AbortController();

    await adminAutoScheduleService.getPreview(
      'preview-1',
      { page: 0, size: 100 },
      { signal: controller.signal },
    );

    expect(apiClient.get).toHaveBeenCalledWith('/api/admin/showtime-schedules/preview-1', {
      params: { page: 0, size: 100 },
      signal: controller.signal,
    });
  });
});
