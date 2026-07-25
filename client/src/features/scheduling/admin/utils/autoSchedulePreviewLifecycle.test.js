import { describe, expect, it } from 'vitest';
import {
  CANDIDATE_APPLY_STATE,
  derivePreviewCapabilities,
  getCandidateApplyStateMeta,
  isCandidateSelectable,
} from './autoSchedulePreviewLifecycle';

const futureExpiry = '2099-08-22T00:00:00Z';

describe('preview lifecycle capabilities', () => {
  it.each([
    ['GENERATING', true, false, false],
    ['PREVIEWED', false, true, true],
    ['APPLYING', true, false, false],
    ['APPLIED', true, false, false],
    ['FAILED', true, false, false],
    ['EXPIRED', true, false, false],
    ['CANCELLED', true, false, false],
  ])('derives truthful capabilities for %s', (status, isReadOnly, isEditable, canApply) => {
    const capabilities = derivePreviewCapabilities({
      status,
      expiresAt: futureExpiry,
      applyMode: 'ALL_OR_NOTHING',
    }, { selectedCount: 1 });

    expect(capabilities.effectiveStatus).toBe(status);
    expect(capabilities.isReadOnly).toBe(isReadOnly);
    expect(capabilities.isEditable).toBe(isEditable);
    expect(capabilities.canApply).toBe(canApply);
    expect(capabilities.lifecycleMessage).toBeTruthy();
  });

  it('derives EXPIRED from a persisted PREVIEWED summary using the existing expiry field', () => {
    const capabilities = derivePreviewCapabilities({
      status: 'PREVIEWED',
      expiresAt: '2026-01-01T00:00:00Z',
      applyMode: 'ALL_OR_NOTHING',
    }, { selectedCount: 2, now: Date.parse('2026-01-02T00:00:00Z') });

    expect(capabilities.effectiveStatus).toBe('EXPIRED');
    expect(capabilities.canSelect).toBe(false);
    expect(capabilities.canApply).toBe(false);
  });

  it('requires ALL_OR_NOTHING, a selection, and a complete safe snapshot before apply', () => {
    const preview = { status: 'PREVIEWED', expiresAt: futureExpiry, applyMode: 'ALL_OR_NOTHING' };

    expect(derivePreviewCapabilities(preview, { selectedCount: 0 }).isApplicable).toBe(false);
    expect(derivePreviewCapabilities({ ...preview, applyMode: 'BEST_EFFORT' }, { selectedCount: 1 }).isApplicable).toBe(false);
    expect(derivePreviewCapabilities(preview, { selectedCount: 1, isSnapshotUpdating: true }).canApply).toBe(false);
    expect(derivePreviewCapabilities(preview, { selectedCount: 1, hasUnsafeSnapshot: true }).canSelect).toBe(false);
  });

  it('maps all five backend item states without a fabricated state', () => {
    const expected = {
      [CANDIDATE_APPLY_STATE.PENDING]: 'Đang chờ',
      [CANDIDATE_APPLY_STATE.CREATED]: 'Đã tạo suất chiếu',
      [CANDIDATE_APPLY_STATE.SKIPPED]: 'Không được chọn',
      [CANDIDATE_APPLY_STATE.CONFLICT]: 'Xung đột',
      [CANDIDATE_APPLY_STATE.FAILED]: 'Thất bại',
    };

    Object.entries(expected).forEach(([state, label]) => {
      expect(getCandidateApplyStateMeta(state).label).toBe(label);
    });
    expect(Object.keys(expected)).not.toContain('APPLIED');
  });

  it('allows selection only for an editable VALID plus PENDING candidate', () => {
    const editable = { canSelect: true };
    const pending = { validationStatus: 'VALID', applyStatus: 'PENDING' };

    expect(isCandidateSelectable(pending, editable)).toBe(true);
    expect(isCandidateSelectable({ ...pending, validationStatus: 'REJECTED' }, editable)).toBe(false);
    expect(isCandidateSelectable({ ...pending, applyStatus: 'CREATED' }, editable)).toBe(false);
    expect(isCandidateSelectable(pending, { canSelect: false })).toBe(false);
  });
});
