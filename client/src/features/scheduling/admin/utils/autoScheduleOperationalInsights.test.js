import { describe, expect, it } from 'vitest';
import {
  getDailyOperationalSummaries,
  getMovieOperationalState,
  getOperationalReasonPresentation,
  summarizeRejectionReasons,
} from './autoScheduleOperationalInsights';

const candidate = (overrides = {}) => ({
  serviceDate: '2026-08-04',
  validationStatus: 'REJECTED',
  applyStatus: 'PENDING',
  selected: false,
  technicalDetails: { rejectionCode: 'SHOWTIME_OVERLAP_CONFLICT' },
  ...overrides,
});

describe('auto-schedule operational insights', () => {
  it('translates backend rejection codes into actionable operator language', () => {
    expect(getOperationalReasonPresentation('SHOWTIME_OVERLAP_CONFLICT')).toMatchObject({
      category: 'EXISTING_SCHEDULE',
      label: 'Lịch hiện có đang chiếm khung giờ',
    });
    expect(getOperationalReasonPresentation('SHOWTIME_OUTSIDE_RELEASE_WINDOW')).toMatchObject({
      category: 'RELEASE_WINDOW',
      label: 'Ngoài thời gian phát hành',
    });
  });

  it('distinguishes no valid option from a valid option not selected by the optimizer', () => {
    expect(getMovieOperationalState({
      scheduledCount: 0,
      validCount: 0,
      rejectionReasons: summarizeRejectionReasons([candidate(), candidate()]),
    })).toMatchObject({
      code: 'EXISTING_SCHEDULE',
      label: 'Lịch hiện có đang chiếm khung giờ',
    });

    expect(getMovieOperationalState({
      scheduledCount: 0,
      validCount: 12,
      rejectionReasons: [],
    })).toMatchObject({
      code: 'VALID_NOT_SELECTED',
      label: 'Hợp lệ nhưng chưa được chọn',
    });
  });

  it('summarizes each service date instead of hiding blocked days in a range total', () => {
    const summaries = getDailyOperationalSummaries([
      candidate({
        serviceDate: '2026-08-04',
        validationStatus: 'VALID',
        selected: true,
        technicalDetails: { rejectionCode: null },
      }),
      candidate({ serviceDate: '2026-08-05' }),
      candidate({ serviceDate: '2026-08-05' }),
    ]);

    expect(summaries).toEqual([
      expect.objectContaining({
        serviceDate: '2026-08-04',
        scheduledCount: 1,
        state: 'HAS_RECOMMENDATIONS',
      }),
      expect.objectContaining({
        serviceDate: '2026-08-05',
        scheduledCount: 0,
        validCount: 0,
        state: 'NO_VALID_OPTIONS',
        label: 'Lịch hiện có đang chiếm khung giờ',
      }),
    ]);
  });
});
