import { describe, expect, it } from 'vitest';
import {
  CANDIDATE_APPLY_PRESENTATION,
  CANDIDATE_VALIDATION_PRESENTATION,
  BATCH_STATUS_REASON_PRESENTATION,
  PREVIEW_STATUS_PRESENTATION,
  SHOWTIME_SOURCE_PRESENTATION,
  SHOWTIME_STATUS_PRESENTATION,
  getApplyModePresentation,
  getBatchStatusReasonPresentation,
  getCandidateApplyPresentation,
  getCandidateValidationPresentation,
  getHistoryActorLabel,
  getLocalizedHistoryReason,
  getPreviewShortCode,
  getPreviewStatusPresentation,
  getScoreBreakdownRows,
  getShowtimeSourcePresentation,
  getShowtimeStatusPresentation,
} from './schedulingPresentation';

describe('schedulingPresentation', () => {
  it('covers every known scheduling enum value', () => {
    expect(Object.keys(PREVIEW_STATUS_PRESENTATION)).toEqual([
      'GENERATING', 'PREVIEWED', 'APPLYING', 'APPLIED', 'EXPIRED', 'FAILED', 'CANCELLED',
    ]);
    expect(Object.keys(CANDIDATE_VALIDATION_PRESENTATION)).toEqual(['VALID', 'REJECTED']);
    expect(Object.keys(CANDIDATE_APPLY_PRESENTATION)).toEqual([
      'PENDING', 'CREATED', 'SKIPPED', 'CONFLICT', 'FAILED',
    ]);
    expect(Object.keys(SHOWTIME_STATUS_PRESENTATION)).toEqual([
      'DRAFT', 'OPEN_FOR_BOOKING', 'CLOSED', 'CANCELLED', 'FINISHED',
    ]);
    expect(Object.keys(SHOWTIME_SOURCE_PRESENTATION)).toEqual(['AUTO', 'MANUAL']);
    expect(Object.keys(BATCH_STATUS_REASON_PRESENTATION)).toEqual([
      'INVALID_SHOWTIME_STATUS_TRANSITION',
      'SHOWTIME_CANNOT_OPEN_AFTER_START',
      'SHOWTIME_PRICE_MISSING',
    ]);
  });

  it('localizes scheduling lifecycle values without losing technical values', () => {
    expect(getPreviewStatusPresentation('PREVIEWED')).toMatchObject({
      label: 'Sẵn sàng rà soát',
      technicalValue: 'PREVIEWED',
    });
    expect(getCandidateValidationPresentation('VALID').label).toBe('Hợp lệ');
    expect(getCandidateApplyPresentation('SKIPPED').label).toBe('Không được chọn');
    expect(getShowtimeStatusPresentation('OPEN_FOR_BOOKING').label).toBe('Đang mở bán');
    expect(getShowtimeSourcePresentation('AUTO').batchLabel).toBe('Đợt tạo tự động');
    expect(getApplyModePresentation('ALL_OR_NOTHING').label).toBe('Tất cả hoặc không tạo');
    expect(getBatchStatusReasonPresentation('SHOWTIME_PRICE_MISSING').label)
      .toBe('Chưa cấu hình đủ giá cho các loại ghế');
  });

  it('uses a safe fallback while retaining an unknown technical value', () => {
    expect(getShowtimeStatusPresentation('LEGACY_STATUS')).toMatchObject({
      label: 'Không xác định',
      technicalValue: 'LEGACY_STATUS',
    });
  });

  it('derives display-only preview codes and audit fallbacks', () => {
    expect(getPreviewShortCode('12345678-abcd-efab-cdef-1234567890ab')).toBe('12345678');
    expect(getPreviewShortCode(null)).toBe('—');
    expect(getHistoryActorLabel(42)).toBe('Người dùng #42');
    expect(getHistoryActorLabel(null)).toBe('Không xác định');
    expect(getLocalizedHistoryReason('Showtime created')).toBe('Đã tạo suất chiếu');
    expect(getLocalizedHistoryReason('  ')).toBe('Không ghi nhận lý do');
  });

  it('localizes score components while preserving raw keys', () => {
    expect(getScoreBreakdownRows({ base: 50, custom: 3 })).toEqual([
      { key: 'base', label: 'Điểm cơ bản', value: 50 },
      { key: 'custom', label: 'custom', value: 3 },
    ]);
  });
});
