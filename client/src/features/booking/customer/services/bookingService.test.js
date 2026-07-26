import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import { getBookingHistory } from './bookingService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}));

describe('bookingService customer history normalization', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps immutable presentation and food data for history cards', async () => {
    apiClient.get.mockResolvedValue({
      data: {
        data: {
          content: [
            {
              publicId: 'booking-1',
              presentation: {
                movieTitle: 'Mưa đỏ',
                moviePosterUrl: 'https://cdn.lorafilm.test/mua-do.jpg',
                cinemaName: 'LoraFilm Cần Thơ',
                auditoriumName: 'Phòng 3',
                showtimeStart: '2026-07-27T12:30:00Z',
                seats: [{ label: 'A1' }, { label: 'A2' }]
              },
              food: {
                items: [{ name: 'Bắp rang', quantity: 2 }]
              }
            }
          ],
          totalPages: 1
        }
      }
    });

    const result = await getBookingHistory({
      fromDate: '2026-07-26',
      toDate: '2026-07-27',
      sort: 'totalAmount,desc'
    });

    expect(result.content[0]).toMatchObject({
      movieTitle: 'Mưa đỏ',
      posterUrl: 'https://cdn.lorafilm.test/mua-do.jpg',
      cinemaName: 'LoraFilm Cần Thơ',
      auditoriumName: 'Phòng 3',
      seatNames: 'A1, A2',
      foodNames: 'Bắp rang x2'
    });
    expect(apiClient.get).toHaveBeenCalledWith('/api/bookings', {
      params: {
        page: 0,
        size: 10,
        sort: 'totalAmount,desc',
        fromDate: new Date('2026-07-26T00:00:00').toISOString(),
        toDate: new Date('2026-07-27T23:59:59.999').toISOString()
      }
    });
  });
});
