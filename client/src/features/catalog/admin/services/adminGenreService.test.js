import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import adminGenreService from './adminGenreService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const envelope = (content, pageNumber, totalPages, totalElements) => ({
  data: {
    success: true,
    data: {
      content,
      pageNumber,
      pageSize: 100,
      totalElements,
      totalPages,
      last: pageNumber === totalPages - 1,
    },
  },
});

describe('adminGenreService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads every genre page instead of silently stopping at 100 rows', async () => {
    apiClient.get
      .mockResolvedValueOnce(envelope([{ publicId: 'genre-1' }], 0, 2, 2))
      .mockResolvedValueOnce(envelope([{ publicId: 'genre-2' }], 1, 2, 2));

    const response = await adminGenreService.getAllGenres();

    expect(apiClient.get).toHaveBeenNthCalledWith(1, '/api/admin/genres', {
      params: { page: 1, size: 100 },
    });
    expect(apiClient.get).toHaveBeenNthCalledWith(2, '/api/admin/genres', {
      params: { page: 2, size: 100 },
    });
    expect(response.data.content).toEqual([
      { publicId: 'genre-1' },
      { publicId: 'genre-2' },
    ]);
    expect(response.data.totalElements).toBe(2);
  });
});
