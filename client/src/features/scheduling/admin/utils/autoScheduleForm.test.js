import { describe, expect, it } from 'vitest';
import {
  addCalendarDays,
  buildAutoScheduleRequestFingerprint,
  getInclusiveDayCount,
  validateAutoScheduleDateRange,
} from './autoScheduleForm';

describe('autoScheduleForm date contract', () => {
  const now = new Date('2026-07-23T17:30:00Z');

  it('accepts one through seven inclusive service dates', () => {
    expect(getInclusiveDayCount('2026-08-22', '2026-08-22')).toBe(1);
    expect(validateAutoScheduleDateRange({
      scheduleFrom: '2026-08-22', scheduleTo: '2026-08-28', cinemaTimezone: 'Asia/Ho_Chi_Minh', now,
    })).toMatchObject({ dayCount: 7, isTooLong: false, errors: {} });
  });

  it('accepts a batch beginning at least 30 days ahead without a future ceiling', () => {
    const result = validateAutoScheduleDateRange({
      scheduleFrom: '2026-08-23', scheduleTo: '2026-08-29', cinemaTimezone: 'Asia/Ho_Chi_Minh', now,
    });
    expect(result.errors).toEqual({});
    expect(result.dayCount).toBe(7);
  });

  it('rejects eight days, preserves the range, and suggests the first seven-day range', () => {
    const result = validateAutoScheduleDateRange({
      scheduleFrom: '2026-08-22', scheduleTo: '2026-08-29', cinemaTimezone: 'Asia/Ho_Chi_Minh', now,
    });
    expect(result.isTooLong).toBe(true);
    expect(result.suggestedScheduleFrom).toBe('2026-08-22');
    expect(result.suggestedScheduleTo).toBe('2026-08-28');
    expect(result.errors.scheduleTo).toContain('tối đa 7 ngày');
  });

  it('rejects only dates before the cinema-local current date', () => {
    const sameDay = validateAutoScheduleDateRange({
      scheduleFrom: '2026-07-24', scheduleTo: '2026-07-24', cinemaTimezone: 'Asia/Ho_Chi_Minh', now,
    });
    const previousDay = validateAutoScheduleDateRange({
      scheduleFrom: '2026-07-23', scheduleTo: '2026-07-23', cinemaTimezone: 'Asia/Ho_Chi_Minh', now,
    });
    expect(sameDay.errors).toEqual({});
    expect(previousDay.errors.scheduleFrom).toContain('không được trước');
  });

  it('handles month boundaries and fingerprints set-like request arrays canonically', () => {
    expect(addCalendarDays('2026-08-28', 6)).toBe('2026-09-03');
    const base = {
      cinemaPublicId: 'cinema', scheduleFrom: '2026-08-22', scheduleTo: '2026-08-28',
      auditoriumPublicIds: ['b', 'a'], movieVersionPublicIds: ['v2', 'v1'],
      slotGranularityMinutes: '15', previewTtlMinutes: '60',
    };
    expect(buildAutoScheduleRequestFingerprint(base)).toBe(buildAutoScheduleRequestFingerprint({
      ...base, auditoriumPublicIds: ['a', 'b'], movieVersionPublicIds: ['v1', 'v2'],
    }));
  });
});
