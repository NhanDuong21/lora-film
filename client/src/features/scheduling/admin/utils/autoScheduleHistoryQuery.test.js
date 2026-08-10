import { describe, expect, it } from 'vitest';
import {
  AUTO_SCHEDULE_HISTORY_DEFAULTS,
  AUTO_SCHEDULE_HISTORY_STRATEGIES,
  dateTimeLocalToInstant,
  getAutoScheduleHistoryRangeError,
  hasAutoScheduleHistoryFilters,
  instantToDateTimeLocal,
  parseAutoScheduleHistoryQuery,
  resetAutoScheduleHistoryFilters,
  serializeAutoScheduleHistoryQuery,
  toAutoScheduleHistoryApiParams,
} from './autoScheduleHistoryQuery';

describe('autoScheduleHistoryQuery', () => {
  it('canonicalizes malformed and unsupported URL values to defaults', () => {
    const parsed = parseAutoScheduleHistoryQuery(new URLSearchParams(
      'status=UNKNOWN&strategyVersion=BALANCED_V9&scheduleFrom=2026-02-30&createdFrom=2026-07-22T10:00:00&page=-1&size=100&sort=id,desc&unexpected=1',
    ));

    expect(parsed).toEqual(AUTO_SCHEDULE_HISTORY_DEFAULTS);
    expect(serializeAutoScheduleHistoryQuery(parsed).toString()).toBe('');
  });

  it('keeps cinema-local schedule dates unchanged and normalizes offset instants', () => {
    const parsed = parseAutoScheduleHistoryQuery(new URLSearchParams(
      'cinemaPublicId=cinema-1&status=PREVIEWED&strategyVersion=BALANCED_V1_S3&scheduleFrom=2026-07-22&scheduleTo=2026-07-23&createdFrom=2026-07-22T17%3A00%3A00%2B07%3A00&page=2&size=20&sort=cinemaName%2Casc',
    ));

    expect(toAutoScheduleHistoryApiParams(parsed)).toEqual({
      cinemaPublicId: 'cinema-1',
      status: 'PREVIEWED',
      strategyVersion: 'BALANCED_V1_S3',
      scheduleFrom: '2026-07-22',
      scheduleTo: '2026-07-23',
      createdFrom: '2026-07-22T10:00:00.000Z',
      page: 2,
      size: 20,
      sort: 'cinemaName,asc',
    });
  });

  it('accepts S4 history links without making assumptions about score breakdowns', () => {
    const parsed = parseAutoScheduleHistoryQuery(new URLSearchParams(
      'strategyVersion=BALANCED_V1_S4',
    ));

    expect(AUTO_SCHEDULE_HISTORY_STRATEGIES).toContain('BALANCED_V1_S4');
    expect(parsed.strategyVersion).toBe('BALANCED_V1_S4');
    expect(serializeAutoScheduleHistoryQuery(parsed).toString())
      .toBe('strategyVersion=BALANCED_V1_S4');
  });

  it('accepts the current S5 distribution strategy in history links', () => {
    const parsed = parseAutoScheduleHistoryQuery(new URLSearchParams(
      'strategyVersion=BALANCED_V1_S5',
    ));

    expect(AUTO_SCHEDULE_HISTORY_STRATEGIES).toContain('BALANCED_V1_S5');
    expect(parsed.strategyVersion).toBe('BALANCED_V1_S5');
    expect(serializeAutoScheduleHistoryQuery(parsed).toString())
      .toBe('strategyVersion=BALANCED_V1_S5');
  });

  it('validates reversed ranges and resets only filters and page', () => {
    const query = {
      ...AUTO_SCHEDULE_HISTORY_DEFAULTS,
      status: 'FAILED',
      scheduleFrom: '2026-07-23',
      scheduleTo: '2026-07-22',
      page: 4,
      size: 50,
      sort: 'status,asc',
    };
    expect(getAutoScheduleHistoryRangeError(query)).toMatch(/không được sau/);
    expect(hasAutoScheduleHistoryFilters(query)).toBe(true);
    expect(resetAutoScheduleHistoryFilters(query)).toEqual({
      ...AUTO_SCHEDULE_HISTORY_DEFAULTS,
      size: 50,
      sort: 'status,asc',
    });
  });

  it('converts datetime-local values through the administrator device timezone', () => {
    const localValue = '2026-07-22T17:30';
    const instant = dateTimeLocalToInstant(localValue);
    expect(instant).toBe(new Date(localValue).toISOString());
    expect(instantToDateTimeLocal(instant)).toBe(localValue);
    expect(dateTimeLocalToInstant('')).toBe('');
  });
});
