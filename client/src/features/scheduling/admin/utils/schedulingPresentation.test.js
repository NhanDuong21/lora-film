import { describe, expect, it } from 'vitest';
import {
  CANDIDATE_APPLY_PRESENTATION,
  CANDIDATE_VALIDATION_PRESENTATION,
  BATCH_STATUS_REASON_PRESENTATION,
  PREVIEW_STATUS_PRESENTATION,
  SHOWTIME_SOURCE_PRESENTATION,
  SHOWTIME_STATUS_PRESENTATION,
  SHOWTIME_TRANSITION_ACTION_PRESENTATION,
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
  getShowtimeTransitionActionPresentation,
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
    expect(Object.keys(SHOWTIME_TRANSITION_ACTION_PRESENTATION)).toEqual([
      'OPEN_FOR_BOOKING', 'CLOSED', 'CANCELLED', 'FINISHED',
    ]);
    expect(Object.keys(SHOWTIME_SOURCE_PRESENTATION)).toEqual(['AUTO', 'MANUAL']);
    expect(Object.keys(BATCH_STATUS_REASON_PRESENTATION)).toEqual([
      'INVALID_SHOWTIME_STATUS_TRANSITION',
      'SHOWTIME_CANNOT_OPEN_AFTER_START',
      'SHOWTIME_PRICE_MISSING',
      'PRICING_INCOMPLETE',
      'PRICE_POLICY_NOT_FOUND',
      'PRICING_AMBIGUOUS',
      'PRICE_POLICY_OVERLAP',
      'INVALID_CINEMA_TIMEZONE',
      'SHOWTIME_OUTSIDE_RELEASE_WINDOW',
      'MOVIE_NOT_AVAILABLE_FOR_SCHEDULING',
      'MOVIE_VERSION_NOT_ACTIVE',
      'CINEMA_NOT_ACTIVE',
      'AUDITORIUM_NOT_ACTIVE',
      'CINEMA_OPERATING_HOURS_NOT_CONFIGURED',
      'SHOWTIME_OUTSIDE_OPERATING_HOURS',
      'SHOWTIME_OVERLAPS_CINEMA_CLOSURE',
      'SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE',
    ]);
  });

  it('localizes scheduling lifecycle values without losing technical values', () => {
    expect(getPreviewStatusPresentation('PREVIEWED')).toMatchObject({
      label: 'Chờ kiểm tra',
      technicalValue: 'PREVIEWED',
    });
    expect(getCandidateValidationPresentation('VALID').label).toBe('Hợp lệ');
    expect(getCandidateApplyPresentation('SKIPPED').label).toBe('Không được chọn');
    expect(getShowtimeStatusPresentation('OPEN_FOR_BOOKING').label).toBe('Đang mở bán');
    expect(getShowtimeTransitionActionPresentation('CLOSED').label).toBe('Đóng bán');
    expect(getShowtimeTransitionActionPresentation('CANCELLED').label).toBe('Hủy suất chiếu');
    expect(getShowtimeSourcePresentation('AUTO').batchLabel).toBe('Đợt tạo tự động');
    expect(getApplyModePresentation('ALL_OR_NOTHING').label).toBe('Tất cả hoặc không tạo');
    expect(getBatchStatusReasonPresentation('PRICING_INCOMPLETE')).toMatchObject({
      label: 'Chưa có bảng giá đầy đủ',
      technicalValue: 'PRICING_INCOMPLETE',
      isFallback: false,
    });
    expect(getBatchStatusReasonPresentation('PRICE_POLICY_NOT_FOUND').label)
      .toBe('Chưa có bảng giá đang áp dụng');
    expect(getBatchStatusReasonPresentation('SHOWTIME_OUTSIDE_RELEASE_WINDOW').label)
      .toBe('Phim nằm ngoài thời gian được phép chiếu');
    expect(getBatchStatusReasonPresentation('AUDITORIUM_NOT_ACTIVE').label)
      .toBe('Phòng chiếu đang tạm ngừng hoạt động');
  });

  it('uses a safe fallback while retaining an unknown technical value', () => {
    expect(getShowtimeStatusPresentation('LEGACY_STATUS')).toMatchObject({
      label: 'Không xác định',
      technicalValue: 'LEGACY_STATUS',
    });
    expect(getBatchStatusReasonPresentation(null)).toMatchObject({
      label: 'Chưa xác định nguyên nhân — vui lòng kiểm tra danh sách suất',
      technicalValue: null,
      isFallback: true,
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
    expect(getScoreBreakdownRows({
      base: 50,
      coverageSearchAdjustment: 20,
      custom: 3,
    })).toEqual([
      { key: 'base', label: 'Điểm cơ bản', value: 50 },
      {
        key: 'coverageSearchAdjustment',
        label: 'Điều chỉnh cân bằng phim',
        value: 20,
      },
      { key: 'custom', label: 'Thành phần bổ sung', value: 3 },
    ]);
  });
});
