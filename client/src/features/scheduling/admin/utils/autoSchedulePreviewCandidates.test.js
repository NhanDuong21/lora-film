import { describe, expect, it } from 'vitest';
import {
  CANDIDATE_VIEWS,
  filterCandidatesByView,
  getCandidateMetrics,
  getCandidateViewCounts,
  getDefaultCandidateView,
  getMovieDistribution,
  getMovieDistributionSummary,
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

    expect(filterCandidatesByView(items, CANDIDATE_VIEWS.ISSUES, selected).map(row => row.itemPublicId))
      .toEqual(['rejected', 'conflict', 'failed']);
    expect(filterCandidatesByView(items, CANDIDATE_VIEWS.UNSELECTED_VALID, selected).map(row => row.itemPublicId))
      .toEqual(['conflict', 'failed', 'skipped', 'created']);
    expect(getCandidateViewCounts(items, selected)).toEqual({
      RECOMMENDED: 1,
      UNSELECTED_VALID: 4,
      ISSUES: 3,
      ALL: 6,
      CREATED: 1,
    });
    expect(getCandidateMetrics(items, selected)).toEqual({
      totalGenerated: 6,
      selectedRecommendations: 1,
      validUnselected: 4,
      rejectedCandidates: 1,
      applyConflictsFailures: 2,
      issueCandidates: 3,
      createdShowtimes: 1,
      skippedCandidates: 1,
    });
  });

  it('hard-limits a 3,615-item dataset to at most 100 rendered candidates and clamps pages', () => {
    const items = Array.from({ length: 3615 }, (_, index) => item(`item-${index}`));

    expect(paginateCandidates(items, 1, 50).items).toHaveLength(50);
    expect(paginateCandidates(items, 1, 100).items).toHaveLength(100);
    expect(paginateCandidates(items, 999, 100)).toMatchObject({ page: 37, totalPages: 37 });
    expect(paginateCandidates(items, 1, 1000).items).toHaveLength(50);
  });

  it('exposes movie coverage gaps and concentration from the selected schedule', () => {
    const candidates = [
      ...Array.from({ length: 7 }, (_, index) => ({
        movieKey: 'movie-a',
        movieTitle: 'Phim A',
        validationStatus: 'VALID',
        applyStatus: 'CREATED',
        selected: true,
        itemPublicId: `a-${index}`,
      })),
      {
        movieKey: 'movie-b',
        movieTitle: 'Phim B',
        validationStatus: 'VALID',
        applyStatus: 'CREATED',
        selected: true,
        itemPublicId: 'b-1',
      },
      {
        movieKey: 'movie-c',
        movieTitle: 'Phim C',
        validationStatus: 'VALID',
        applyStatus: 'SKIPPED',
        selected: false,
        itemPublicId: 'c-1',
      },
    ];

    const distribution = getMovieDistribution(candidates);
    expect(distribution.map(row => ({
      title: row.movieTitle,
      scheduled: row.scheduledCount,
      share: row.sharePercent,
      gap: row.hasCoverageGap,
    }))).toEqual([
      { title: 'Phim A', scheduled: 7, share: 87.5, gap: false },
      { title: 'Phim B', scheduled: 1, share: 12.5, gap: false },
      { title: 'Phim C', scheduled: 0, share: 0, gap: true },
    ]);
    expect(getMovieDistributionSummary(distribution)).toMatchObject({
      eligibleMovieCount: 3,
      representedMovieCount: 2,
      dominantMovieTitle: 'Phim A',
      dominantSharePercent: 87.5,
      hasCoverageGap: true,
      isHighlyConcentrated: true,
    });
  });
});
