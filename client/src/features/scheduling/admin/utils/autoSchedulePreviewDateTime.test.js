import { execFileSync } from 'node:child_process';
import process from 'node:process';
import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import {
  addServiceDateDays,
  buildOperationalDateRange,
  buildDynamicTimelineWindow,
  compareServiceDateKeys,
  FALLBACK_PREVIEW_TIMEZONE,
  formatCinemaTime,
  formatPreviewDateKey,
  formatPreviewDateRange,
  formatServiceDateKey,
  formatTimelineMinute,
  getCandidateTimelineOffsets,
  getCinemaDateKey,
  getCinemaMinuteOffset,
  getCinemaTimeParts,
  getOperationalTodayDateKey,
  getServiceDateKey,
  resolveCinemaTimezone,
  UNKNOWN_SERVICE_DATE_KEY,
} from './autoSchedulePreviewDateTime';

describe('auto schedule preview cinema time', () => {
  it('formats and groups by the requested cinema timezone', () => {
    const instant = '2026-07-24T18:30:00Z';
    expect(getCinemaDateKey(instant, 'Asia/Ho_Chi_Minh')).toBe('2026-07-25');
    expect(formatCinemaTime(instant, 'Asia/Ho_Chi_Minh')).toBe('01:30');
    expect(getCinemaDateKey(instant, 'UTC')).toBe('2026-07-24');
    expect(formatCinemaTime(instant, 'America/New_York')).toBe('14:30');
  });

  it('derives the operational today from the cinema timezone instead of the browser timezone', () => {
    const nearMidnight = new Date('2026-08-10T18:00:00Z');
    expect(getOperationalTodayDateKey('Asia/Ho_Chi_Minh', nearMidnight)).toBe('2026-08-11');
    expect(getOperationalTodayDateKey('UTC', nearMidnight)).toBe('2026-08-10');
  });

  it('builds plain-calendar quick date ranges across month boundaries', () => {
    expect(addServiceDateDays('2026-08-31', 1)).toBe('2026-09-01');
    expect(buildOperationalDateRange('2026-08-30', 4)).toEqual([
      '2026-08-30',
      '2026-08-31',
      '2026-09-01',
      '2026-09-02',
    ]);
    expect(buildOperationalDateRange('invalid', 7)).toEqual([]);
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

  it('builds a dynamic whole-hour range from earliest start to latest occupancy end', () => {
    const candidates = [
      { timelineEligible: true, startMinuteOffset: 75, occupancyEndMinuteOffset: 195 },
      { timelineEligible: true, startMinuteOffset: 620, occupancyEndMinuteOffset: 755 },
    ];

    expect(buildDynamicTimelineWindow(candidates)).toEqual({
      startMinute: 60,
      endMinute: 780,
      totalMinutes: 720,
      ticks: Array.from({ length: 13 }, (_, index) => 60 + (index * 60)),
    });
  });

  it('supports overnight offsets beyond 24:00 without clipping', () => {
    const candidate = {
      startTime: '2026-07-24T23:30:00Z',
      endTime: '2026-07-25T01:00:00Z',
      occupancyEndTime: '2026-07-25T01:20:00Z',
    };
    expect(getCandidateTimelineOffsets(candidate, '2026-07-24', 'UTC')).toEqual({
      valid: true,
      startMinute: 1410,
      endMinute: 1500,
      occupancyEndMinute: 1520,
    });
    expect(getCinemaMinuteOffset(candidate.occupancyEndTime, '2026-07-24', 'UTC')).toBe(1520);
    expect(formatTimelineMinute(1500)).toBe('25:00');
    expect(buildDynamicTimelineWindow([{
      timelineEligible: true,
      startMinuteOffset: 1410,
      occupancyEndMinuteOffset: 1520,
    }])).toMatchObject({ startMinute: 1380, endMinute: 1560 });
  });

  it('ignores malformed intervals without corrupting valid candidates', () => {
    expect(getCandidateTimelineOffsets({
      startTime: 'invalid', endTime: null, occupancyEndTime: null,
    }, '2026-07-24', 'UTC').valid).toBe(false);
    expect(buildDynamicTimelineWindow([
      { timelineEligible: false, startMinuteOffset: null, occupancyEndMinuteOffset: null },
      { timelineEligible: true, startMinuteOffset: 60, occupancyEndMinuteOffset: 180 },
    ])).toMatchObject({ startMinute: 60, endMinute: 180 });
  });

  it('is independent of the process/browser timezone', () => {
    const moduleUrl = pathToFileURL(
      resolve('src/features/scheduling/admin/utils/autoSchedulePreviewDateTime.js'),
    ).href;
    const script = `
      import { getCinemaDateKey, formatCinemaTime, getCandidateTimelineOffsets } from ${JSON.stringify(moduleUrl)};
      console.log(JSON.stringify({
        date: getCinemaDateKey('2026-07-24T18:30:00Z', 'Asia/Ho_Chi_Minh'),
        time: formatCinemaTime('2026-07-24T18:30:00Z', 'Asia/Ho_Chi_Minh'),
        range: getCandidateTimelineOffsets({
          startTime: '2026-07-24T23:30:00Z',
          endTime: '2026-07-25T01:00:00Z',
          occupancyEndTime: '2026-07-25T01:20:00Z',
        }, '2026-07-24', 'UTC'),
      }));
    `;
    const runWithTimezone = timezone => execFileSync(
      process.execPath,
      ['--input-type=module', '--eval', script],
      { env: { ...process.env, TZ: timezone }, encoding: 'utf8' },
    ).trim();

    expect(runWithTimezone('UTC')).toBe(runWithTimezone('America/Los_Angeles'));
    expect(runWithTimezone('UTC')).toBe(runWithTimezone('Asia/Tokyo'));
  }, 15000);

  it('treats authoritative service dates as plain calendar strings', () => {
    expect(getServiceDateKey('2026-07-24')).toBe('2026-07-24');
    expect(getServiceDateKey('2026-02-29')).toBe(UNKNOWN_SERVICE_DATE_KEY);
    expect(getServiceDateKey(null)).toBe(UNKNOWN_SERVICE_DATE_KEY);
    expect(formatServiceDateKey('2026-07-24')).toBe('24/07/2026');
    expect(formatServiceDateKey('2026-07-24', { weekday: true })).toBe('Thứ sáu, 24/07/2026');
    expect(formatServiceDateKey(UNKNOWN_SERVICE_DATE_KEY)).toBe('Không xác định ngày vận hành');
    expect([UNKNOWN_SERVICE_DATE_KEY, '2026-07-25', '2026-07-24'].sort(compareServiceDateKeys))
      .toEqual(['2026-07-24', '2026-07-25', UNKNOWN_SERVICE_DATE_KEY]);
  });

  it('formats calendar date ranges as dd/MM/yyyy without browser-local conversion', () => {
    expect(formatPreviewDateRange('2026-08-10', '2026-08-16'))
      .toBe('10/08/2026 – 16/08/2026');
    expect(formatPreviewDateRange('2026-08-10', '')).toBe('10/08/2026');
    expect(formatPreviewDateRange('', '')).toBe('—');
  });
});
