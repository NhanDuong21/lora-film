import { describe, expect, it, vi } from 'vitest';
import {
  formatShowtimeCinemaDate,
  formatShowtimeCinemaDateTime,
  formatShowtimeCinemaTime,
  resolveShowtimeCinemaTimezone,
} from './showtimeCinemaDateTime';

describe('showtime cinema date-time formatting', () => {
  it('formats operational instants in the supplied cinema timezone', () => {
    const instant = '2026-07-24T18:30:00Z';
    expect(formatShowtimeCinemaDate(instant, 'Asia/Ho_Chi_Minh')).toBe('25/07/2026');
    expect(formatShowtimeCinemaTime(instant, 'Asia/Ho_Chi_Minh')).toBe('01:30');
    expect(formatShowtimeCinemaDateTime(instant, 'America/New_York')).toContain('14:30');
  });

  it('uses a consistent UTC fallback for invalid and missing timezones', () => {
    expect(resolveShowtimeCinemaTimezone('Not/A_Timezone')).toEqual({
      timezone: 'UTC',
      requestedTimezone: 'Not/A_Timezone',
      usedFallback: true,
    });
    expect(resolveShowtimeCinemaTimezone(null).usedFallback).toBe(true);
    expect(formatShowtimeCinemaTime('2026-07-24T18:30:00Z', null)).toBe('18:30');
  });

  it('reuses cached Intl formatters for repeated operational timestamps', () => {
    const NativeDateTimeFormat = Intl.DateTimeFormat;
    const constructor = vi.spyOn(Intl, 'DateTimeFormat').mockImplementation(
      function MockDateTimeFormat(...args) {
        return new NativeDateTimeFormat(...args);
      },
    );
    try {
      formatShowtimeCinemaDateTime('2026-07-24T18:30:00Z', 'Pacific/Chatham');
      const afterFirstFormat = constructor.mock.calls.length;
      formatShowtimeCinemaDateTime('2026-07-24T19:30:00Z', 'Pacific/Chatham');

      expect(afterFirstFormat).toBeGreaterThan(0);
      expect(constructor.mock.calls).toHaveLength(afterFirstFormat);
    } finally {
      constructor.mockRestore();
    }
  });
});
