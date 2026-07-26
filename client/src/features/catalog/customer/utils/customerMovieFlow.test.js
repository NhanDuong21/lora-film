import { describe, expect, it } from 'vitest';
import {
  addCalendarDays,
  formatLocalClock,
  isFutureBookableShowtime,
  seatSelectionPath
} from './customerMovieFlow';

describe('customer movie flow helpers', () => {
  it('builds the canonical public Showtime handoff', () => {
    expect(seatSelectionPath('showtime/public')).toBe(
      '/seat-selection?showtimeId=showtime%2Fpublic'
    );
  });

  it('adds calendar days without browser timezone shifting', () => {
    expect(addCalendarDays('2026-07-31', 1)).toBe('2026-08-01');
  });

  it('formats backend local clock values without parsing them as browser dates', () => {
    expect(formatLocalClock('2026-07-25T00:30:00')).toBe('00:30');
  });

  it('keeps only open showtimes whose absolute start instant is in the future', () => {
    const now = Date.parse('2026-07-26T11:53:00Z');
    const showtime = startTime => ({ status: 'OPEN_FOR_BOOKING', startTime });

    expect(isFutureBookableShowtime(showtime('2026-07-26T02:00:00Z'), now)).toBe(false);
    expect(isFutureBookableShowtime(showtime('2026-07-26T05:30:00Z'), now)).toBe(false);
    expect(isFutureBookableShowtime(showtime('2026-07-26T11:53:00Z'), now)).toBe(false);
    expect(isFutureBookableShowtime(showtime('2026-07-26T12:30:00Z'), now)).toBe(true);
    expect(isFutureBookableShowtime({
      status: 'CLOSED',
      startTime: '2026-07-26T12:30:00Z'
    }, now)).toBe(false);
  });
});
