import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  getCounterSessionHistory,
  getCurrentCounterSession,
  searchCounterCustomers,
} from './employeeOperationsService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('employeeOperationsService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('giữ nguyên dữ liệu null khi nhân viên chưa mở ca thu ngân', async () => {
    apiClient.get.mockResolvedValue({ data: { success: true, data: null } });

    await expect(getCurrentCounterSession()).resolves.toBeNull();
    await expect(getCounterSessionHistory()).resolves.toEqual([]);
  });

  it('tra cứu thành viên bằng dữ liệu tối thiểu dành cho quầy vé', async () => {
    apiClient.get.mockResolvedValue({
      data: { data: { content: [{ customerCode: 'CUS-0006', fullName: 'Nguyễn Quốc Bảo' }] } },
    });

    await expect(searchCounterCustomers('Nguyễn Quốc Bảo')).resolves.toEqual({
      content: [{ customerCode: 'CUS-0006', fullName: 'Nguyễn Quốc Bảo' }],
    });
    expect(apiClient.get).toHaveBeenCalledWith('/api/users/customers/counter-search', {
      params: { keyword: 'Nguyễn Quốc Bảo', page: 0, size: 8, sort: 'joinedAt,desc' },
    });
  });
});
