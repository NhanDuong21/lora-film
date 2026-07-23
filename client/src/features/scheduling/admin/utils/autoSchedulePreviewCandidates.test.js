import { describe, expect, it } from 'vitest';
import {
  CANDIDATE_VIEWS,
  filterCandidatesByView,
  getCandidateViewCounts,
  getDefaultCandidateView,
  paginateCandidates,
} from './autoSchedulePreviewCandidates';

const item = (id, overrides = {}) => ({
  itemPublicId: id,
  validationStatus: 'VALID',
  applyStatus: 'PENDING',
  ...overrides,
});

describe('bounded candidate views', () => {
  it('defaults by lifecycle capability', () => {
    expect(getDefaultCandidateView({ isEditable: true, effectiveStatus: 'PREVIEWED' }))
      .toBe(CANDIDATE_VIEWS.RECOMMENDED);
    expect(getDefaultCandidateView({ isEditable: false, effectiveStatus: 'APPLIED' }))
      .toBe(CANDIDATE_VIEWS.CREATED);
    expect(getDefaultCandidateView({ isEditable: false, effectiveStatus: 'FAILED' }))
      .toBe(CANDIDATE_VIEWS.ALL);
  });

  it('keeps SKIPPED separate from rejected/conflict outcomes', () => {
    const items = [
      item('recommended'),
      item('rejected', { validationStatus: 'REJECTED' }),
      item('conflict', { applyStatus: 'CONFLICT' }),
      item('failed', { applyStatus: 'FAILED' }),
      item('skipped', { applyStatus: 'SKIPPED' }),
      item('created', { applyStatus: 'CREATED' }),
    ];
    const selected = new Set(['recommended']);

    expect(filterCandidatesByView(items, CANDIDATE_VIEWS.REJECTED, selected).map(row => row.itemPublicId))
      .toEqual(['rejected', 'conflict', 'failed']);
    expect(getCandidateViewCounts(items, selected)).toEqual({
      RECOMMENDED: 1,
      REJECTED: 3,
      ALL: 6,
      CREATED: 1,
    });
  });

  it('hard-limits a 3,615-item dataset to at most 100 rendered candidates and clamps pages', () => {
    const items = Array.from({ length: 3615 }, (_, index) => item(`item-${index}`));

    expect(paginateCandidates(items, 1, 50).items).toHaveLength(50);
    expect(paginateCandidates(items, 1, 100).items).toHaveLength(100);
    expect(paginateCandidates(items, 999, 100)).toMatchObject({ page: 37, totalPages: 37 });
    expect(paginateCandidates(items, 1, 1000).items).toHaveLength(50);
  });
});
