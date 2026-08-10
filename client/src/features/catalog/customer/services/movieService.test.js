import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  getCinemaBySlug,
  getCinemas,
  getMovies,
  getSeatLayout,
  getShowtimes
} from './movieService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn()
  }
}));

describe('customer movie service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('requests all public movies by default and normalizes public DTO fields', async () => {
    apiClient.get.mockResolvedValue({
      data: {
        data: {
          content: [{
            publicId: 'movie-public-1',
            title: 'Nhà Có Năm Nàng Tiên',
            primaryPoster: 'https://cdn.lorafilm.test/poster.jpg',
            synopsis: 'Nội dung phim'
          }],
          totalPages: 1
        }
      }
    });

    const result = await getMovies({ page: 0, size: 8 });

    expect(apiClient.get).toHaveBeenCalledWith('/api/customer/movies', {
      params: {
        page: 0,
        size: 8,
        status: 'all'
      },
      signal: undefined
    });
    expect(result.content[0]).toMatchObject({
      id: 'movie-public-1',
      posterUrl: 'https://cdn.lorafilm.test/poster.jpg',
      description: 'Nội dung phim'
    });
  });

  it('maps status, keyword and public genre identity to the API contract', async () => {
    apiClient.get.mockResolvedValue({
      data: { data: { content: [] } }
    });

    await getMovies({
      status: 'NOW_SHOWING',
      search: 'Nàng tiên',
      genreId: 'genre-public-1'
    });

    expect(apiClient.get).toHaveBeenCalledWith('/api/customer/movies', {
      params: expect.objectContaining({
        status: 'now-showing',
        keyword: 'Nàng tiên',
        genrePublicId: 'genre-public-1'
      }),
      signal: undefined
    });
  });

  it('uses public customer APIs for cinemas, showtimes and real seat prices', async () => {
    apiClient.get
      .mockResolvedValueOnce({
        data: { data: { data: [{ publicId: 'cinema-1', slug: 'lorafilm-01' }] } }
      })
      .mockResolvedValueOnce({
        data: { data: { publicId: 'cinema-1', slug: 'lorafilm-01' } }
      })
      .mockResolvedValueOnce({
        data: { data: { data: [{ showtimePublicId: 'showtime-1' }] } }
      })
      .mockResolvedValueOnce({
        data: { data: { seats: [{ seatType: 'VIP', price: 86000 }] } }
      });

    await getCinemas({ page: 0, size: 100 });
    await getCinemaBySlug('lorafilm-01');
    await getShowtimes({ cinemaSlug: 'lorafilm-01', date: '2026-07-30' });
    await getSeatLayout('showtime-1');

    expect(apiClient.get).toHaveBeenNthCalledWith(1, '/api/cinemas', {
      params: { page: 0, size: 100 },
      signal: undefined
    });
    expect(apiClient.get).toHaveBeenNthCalledWith(2, '/api/cinemas/lorafilm-01');
    expect(apiClient.get).toHaveBeenNthCalledWith(3, '/api/showtimes', {
      params: {
        page: 0,
        size: 100,
        cinemaSlug: 'lorafilm-01',
        date: '2026-07-30'
      },
      signal: undefined
    });
    expect(apiClient.get).toHaveBeenNthCalledWith(
      4,
      '/api/customer/showtimes/showtime-1/seat-layout',
      { signal: undefined }
    );
  });
});
