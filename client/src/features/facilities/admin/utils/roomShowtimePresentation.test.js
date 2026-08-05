import { describe, expect, it } from 'vitest';
import {
  getNextShowtimeForAuditorium,
  getShowtimeDateKeys,
  getShowtimeState,
} from './roomShowtimePresentation';

const now = new Date('2026-08-05T10:00:00Z');

const showtime = (overrides = {}) => ({
  showtimePublicId: 'showtime-1',
  auditorium: { publicId: 'room-1' },
  movie: { title: 'Phim A' },
  startTime: '2026-08-05T11:00:00Z',
  endTime: '2026-08-05T13:00:00Z',
  status: 'OPEN_FOR_BOOKING',
  ...overrides,
});

describe('room showtime presentation', () => {
  it('returns the nearest active showtime for a room', () => {
    const result = getNextShowtimeForAuditorium([
      showtime({ showtimePublicId: 'later', startTime: '2026-08-05T15:00:00Z' }),
      showtime({ showtimePublicId: 'nearest', startTime: '2026-08-05T11:00:00Z' }),
      showtime({ showtimePublicId: 'cancelled', status: 'CANCELLED', startTime: '2026-08-05T10:30:00Z' }),
    ], 'room-1', now);

    expect(result.showtimePublicId).toBe('nearest');
  });

  it('keeps a currently running showtime as the next operational item', () => {
    const result = getNextShowtimeForAuditorium([
      showtime({ startTime: '2026-08-05T09:30:00Z' }),
      showtime({ showtimePublicId: 'later', startTime: '2026-08-05T11:00:00Z' }),
    ], 'room-1', now);

    expect(result.showtimePublicId).toBe('showtime-1');
    expect(getShowtimeState(result, now)).toBe('SHOWING');
  });

  it('builds the next seven service dates in the cinema timezone', () => {
    expect(getShowtimeDateKeys(now, 'Asia/Ho_Chi_Minh', 3)).toEqual([
      '2026-08-05',
      '2026-08-06',
      '2026-08-07',
    ]);
  });
});
