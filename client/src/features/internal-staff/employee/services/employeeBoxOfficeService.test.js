import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import { getCinemas } from '@/features/catalog/customer/services/movieService';
import { getMyEmployeeCinemaContext } from './employeeBoxOfficeService';

vi.mock('@/services/apiClient', () => ({
  default: { get: vi.fn() },
}));
vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getCinemas: vi.fn(),
}));

describe('employeeBoxOfficeService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('ghép hồ sơ nhân viên với tên rạp dễ hiểu trên màn hình vận hành', async () => {
    apiClient.get.mockResolvedValue({
      data: { data: { accountId: 3, cinemaPublicId: 'cinema-landmark' } },
    });
    getCinemas.mockResolvedValue({
      data: [{ publicId: 'cinema-landmark', name: 'LoraFilm Landmark 81' }],
    });

    await expect(getMyEmployeeCinemaContext()).resolves.toEqual(expect.objectContaining({
      accountId: 3,
      cinemaName: 'LoraFilm Landmark 81',
      cinema: expect.objectContaining({ publicId: 'cinema-landmark' }),
    }));
  });
});
