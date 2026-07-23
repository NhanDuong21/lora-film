import { describe, expect, it } from 'vitest';
import { cinemaLocalDateTimeToInstant } from './pricingDateTime';

describe('pricing cinema-local date/time conversion', () => {
  it('uses the configured cinema timezone instead of the browser timezone', () => {
    expect(cinemaLocalDateTimeToInstant(
      '2026-07-25T10:00', 'Asia/Ho_Chi_Minh',
    )).toBe('2026-07-25T03:00:00.000Z');
    expect(cinemaLocalDateTimeToInstant(
      '2026-07-25T10:00', 'America/New_York',
    )).toBe('2026-07-25T14:00:00.000Z');
  });

  it('rejects invalid or missing timezone data', () => {
    expect(() => cinemaLocalDateTimeToInstant('2026-07-25T10:00', 'Not/AZone'))
      .toThrow();
    expect(() => cinemaLocalDateTimeToInstant('2026-07-25T10:00', ''))
      .toThrow();
  });
});
