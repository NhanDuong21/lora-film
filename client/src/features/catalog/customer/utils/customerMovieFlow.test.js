import { describe, expect, it } from 'vitest';
import {
  addCalendarDays,
  formatLocalClock,
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
});
