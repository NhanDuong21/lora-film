import { describe, expect, it } from 'vitest';
import { getAutoScheduleError } from './autoScheduleErrors';

describe('getAutoScheduleError', () => {
  it('normalizes plain and Axios-shaped backend envelopes', () => {
    expect(getAutoScheduleError({
      errorCode: 'AUTO_SCHEDULE_DATE_RANGE_TOO_LARGE', message: 'backend message',
    }).message).toContain('7 ngày');
    expect(getAutoScheduleError({ response: { data: {
      errorCode: 'AUTO_SCHEDULE_CANDIDATE_LIMIT_EXCEEDED', message: 'backend message',
    } } }).message).toContain('quá nhiều suất đề xuất');
    expect(getAutoScheduleError({ response: { data: {
      errorCode: 'AUTO_SCHEDULE_TOO_MANY_CANDIDATES', message: 'backend message',
    } } }).message).toContain('quá nhiều suất đề xuất');
    expect(getAutoScheduleError({
      errorCode: 'AUTO_SCHEDULE_INVALID_DATE_RANGE', message: 'Cannot schedule in the past',
    }).message).toContain('giờ địa phương của rạp');
    expect(getAutoScheduleError({
      errorCode: 'IDEMPOTENCY_KEY_REUSED', message: 'backend message',
    }).message).toContain('không khớp cấu hình');
  });

  it('preserves an actionable backend message for unknown business codes', () => {
    expect(getAutoScheduleError({ errorCode: 'NEW_CODE', message: 'Hãy chọn lại phòng chiếu.' }).message)
      .toBe('Hãy chọn lại phòng chiếu.');
  });
});
