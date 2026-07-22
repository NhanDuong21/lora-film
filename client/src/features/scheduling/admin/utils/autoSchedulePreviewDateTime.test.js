import { execFileSync } from 'node:child_process';
import process from 'node:process';
import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import {
  FALLBACK_PREVIEW_TIMEZONE,
  formatCinemaTime,
  formatPreviewDateKey,
  getCinemaDateKey,
  getCinemaTimeParts,
  getTimelineRange,
  resolveCinemaTimezone,
} from './autoSchedulePreviewDateTime';

describe('auto schedule preview cinema time', () => {
  it('formats and groups by the requested cinema timezone', () => {
    const instant = '2026-07-24T18:30:00Z';

    expect(getCinemaDateKey(instant, 'Asia/Ho_Chi_Minh')).toBe('2026-07-25');
    expect(formatCinemaTime(instant, 'Asia/Ho_Chi_Minh')).toBe('01:30');
    expect(getCinemaDateKey(instant, 'UTC')).toBe('2026-07-24');
    expect(formatCinemaTime(instant, 'America/New_York')).toBe('14:30');
  });

  it('handles the New York DST spring-forward transition', () => {
    const before = getCinemaTimeParts('2026-03-08T06:30:00Z', 'America/New_York');
    const after = getCinemaTimeParts('2026-03-08T07:30:00Z', 'America/New_York');

    expect({ date: before.dateKey, hour: before.hour, minute: before.minute })
      .toEqual({ date: '2026-03-08', hour: 1, minute: 30 });
    expect({ date: after.dateKey, hour: after.hour, minute: after.minute })
      .toEqual({ date: '2026-03-08', hour: 3, minute: 30 });
  });

  it('uses a deterministic UTC fallback for invalid and missing timezones', () => {
    expect(resolveCinemaTimezone('Not/A_Timezone')).toEqual({
      timezone: FALLBACK_PREVIEW_TIMEZONE,
      requestedTimezone: 'Not/A_Timezone',
      usedFallback: true,
    });
    expect(resolveCinemaTimezone(null).usedFallback).toBe(true);
    expect(getCinemaDateKey('2026-07-24T23:30:00Z', 'Not/A_Timezone')).toBe('2026-07-24');
    expect(formatPreviewDateKey('2026-02-30')).toBe('—');
  });

  it('preserves the 08:00–24:00 timeline contract and clips safely', () => {
    expect(getTimelineRange('2026-07-24T10:00:00Z', '2026-07-24T12:00:00Z', 'UTC'))
      .toMatchObject({ isVisible: true, left: '12.5%', width: '12.5%' });

    expect(getTimelineRange('2026-07-24T07:00:00Z', '2026-07-24T09:00:00Z', 'UTC'))
      .toMatchObject({ isVisible: true, isClippedAtStart: true, left: '0%', width: '6.25%' });

    expect(getTimelineRange('2026-07-24T01:00:00Z', '2026-07-24T03:00:00Z', 'UTC'))
      .toMatchObject({ isVisible: false, isOutsideRange: true });

    expect(getTimelineRange('2026-07-24T23:00:00Z', '2026-07-25T01:00:00Z', 'UTC'))
      .toMatchObject({ isVisible: true, isClippedAtEnd: true, left: '93.75%', width: '6.25%' });
  });

  it('is independent of the process/browser timezone', () => {
    const moduleUrl = pathToFileURL(
      resolve('src/features/scheduling/admin/utils/autoSchedulePreviewDateTime.js'),
    ).href;
    const script = `
      import { getCinemaDateKey, formatCinemaTime, getTimelineRange } from ${JSON.stringify(moduleUrl)};
      console.log(JSON.stringify({
        date: getCinemaDateKey('2026-07-24T18:30:00Z', 'Asia/Ho_Chi_Minh'),
        time: formatCinemaTime('2026-07-24T18:30:00Z', 'Asia/Ho_Chi_Minh'),
        range: getTimelineRange('2026-07-24T10:00:00Z', '2026-07-24T12:00:00Z', 'UTC'),
      }));
    `;
    const runWithTimezone = (timezone) => execFileSync(
      process.execPath,
      ['--input-type=module', '--eval', script],
      { env: { ...process.env, TZ: timezone }, encoding: 'utf8' },
    ).trim();

    expect(runWithTimezone('UTC')).toBe(runWithTimezone('America/Los_Angeles'));
    expect(runWithTimezone('UTC')).toBe(runWithTimezone('Asia/Tokyo'));
  });
});
