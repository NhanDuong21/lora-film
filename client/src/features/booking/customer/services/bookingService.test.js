import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  cancelBooking,
  createBooking,
  getBookingHistory
} from './bookingService';

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
    sessionStorage.clear();
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

  it('clears the completed creation attempt after Booking creation succeeds', async () => {
    sessionStorage.setItem('booking:create:showtime-1', '{"attempt":"pending"}');
    apiClient.post.mockResolvedValue({
      data: { data: { publicId: 'booking-1' } }
    });

    await createBooking({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-1'],
      idempotencyKey: 'request-key-1'
    });

    expect(sessionStorage.getItem('booking:create:showtime-1')).toBeNull();
  });

  it('clears all old creation attempts after cancellation succeeds', async () => {
    sessionStorage.setItem('booking:create:showtime-1', '{"attempt":"old"}');
    sessionStorage.setItem('booking:create:showtime-2', '{"attempt":"old"}');
    apiClient.delete.mockResolvedValue({
      data: { data: { publicId: 'booking-1', status: 'CANCELLED' } }
    });

    await cancelBooking('booking-1');

    expect(sessionStorage.getItem('booking:create:showtime-1')).toBeNull();
    expect(sessionStorage.getItem('booking:create:showtime-2')).toBeNull();
  });
});
