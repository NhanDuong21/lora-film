import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import { notificationCustomerService } from './notificationCustomerService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn()
  }
}));

describe('notificationCustomerService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads the authenticated customer notification page', async () => {
    apiClient.get.mockResolvedValue({
      data: { data: { content: [{ publicId: 'notification-1' }], totalPages: 1 } }
    });

    const page = await notificationCustomerService.list({ page: 2, size: 10 });

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/notifications', {
      params: { page: 2, size: 10 }
    });
    expect(page.content).toHaveLength(1);
  });

  it('marks one notification and all notifications as read', async () => {
    apiClient.patch
      .mockResolvedValueOnce({ data: { data: { publicId: 'notification-1', readAt: 'now' } } })
      .mockResolvedValueOnce({ data: { data: { count: 4 } } });

    await notificationCustomerService.markRead('notification-1');
    const count = await notificationCustomerService.markAllRead();

    expect(apiClient.patch).toHaveBeenNthCalledWith(
      1, '/api/v1/notifications/notification-1/read'
    );
    expect(apiClient.patch).toHaveBeenNthCalledWith(
      2, '/api/v1/notifications/read-all'
    );
    expect(count).toBe(4);
  });
});
