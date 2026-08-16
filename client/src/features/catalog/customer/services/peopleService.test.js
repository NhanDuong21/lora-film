import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import { getPeople, getPerson, getPersonMovies } from './peopleService';

vi.mock('@/services/apiClient', () => ({
  default: { get: vi.fn() },
}));

describe('peopleService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('queries the internal public people API with catalog filters', async () => {
    apiClient.get.mockResolvedValue({ data: { data: { content: [] } } });

    await getPeople({
      role: 'ACTOR',
      query: 'Tom',
      availability: 'NOW_SHOWING',
      sort: 'POPULAR',
      page: 0,
      size: 20,
    });

    expect(apiClient.get).toHaveBeenCalledWith('/api/public/people', {
      params: {
        role: 'ACTOR',
        query: 'Tom',
        availability: 'NOW_SHOWING',
        sort: 'POPULAR',
        page: 0,
        size: 20,
      },
      signal: undefined,
    });
  });

  it('loads profile and filtered credits from internal endpoints', async () => {
    apiClient.get
      .mockResolvedValueOnce({ data: { data: { name: 'Tom Hanks' } } })
      .mockResolvedValueOnce({ data: { data: [{ title: 'Forrest Gump' }] } });

    await expect(getPerson('tom-hanks-id')).resolves.toEqual({ name: 'Tom Hanks' });
    await expect(getPersonMovies('tom-hanks-id', { availability: 'NOW_SHOWING' }))
      .resolves.toEqual([{ title: 'Forrest Gump' }]);

    expect(apiClient.get).toHaveBeenNthCalledWith(
      1,
      '/api/public/people/tom-hanks-id',
      { signal: undefined },
    );
    expect(apiClient.get).toHaveBeenNthCalledWith(
      2,
      '/api/public/people/tom-hanks-id/movies',
      { params: { availability: 'NOW_SHOWING' }, signal: undefined },
    );
  });
});
