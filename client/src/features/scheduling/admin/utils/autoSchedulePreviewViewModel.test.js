import { describe, expect, it } from 'vitest';
import {
  buildCandidateViewModels,
  getDefaultActiveServiceDate,
  getMoviePalette,
  getPrimaryTimelineCandidates,
  getRelevantAuditoriums,
  sortCandidateViewModels,
} from './autoSchedulePreviewViewModel';

const item = (id, overrides = {}) => ({
  itemPublicId: id,
  moviePublicId: `movie-${id}`,
  movieTitle: `Phim ${id}`,
  movieVersionPublicId: `version-${id}`,
  versionName: '2D',
  auditoriumPublicId: 'aud-1',
  auditoriumName: 'Phòng 1',
  serviceDate: '2026-07-24',
  startTime: '2026-07-24T10:00:00Z',
  endTime: '2026-07-24T11:00:00Z',
  occupancyEndTime: '2026-07-24T11:15:00Z',
  validationStatus: 'VALID',
  applyStatus: 'PENDING',
  rankingPosition: 1,
  score: 80,
  ...overrides,
});

describe('auto schedule preview candidate view models', () => {
  it('defaults to the earliest authoritative date with a selected candidate', () => {
    const items = [
      item('early', { serviceDate: '2026-07-24' }),
      item('selected-late', {
        serviceDate: '2026-07-25',
        startTime: '2026-07-25T10:00:00Z',
        endTime: '2026-07-25T11:00:00Z',
        occupancyEndTime: '2026-07-25T11:15:00Z',
      }),
      item('selected-later', {
        serviceDate: '2026-07-26',
        startTime: '2026-07-26T10:00:00Z',
        endTime: '2026-07-26T11:00:00Z',
        occupancyEndTime: '2026-07-26T11:15:00Z',
      }),
    ];
    const models = buildCandidateViewModels(items, {
      selectedItemIds: new Set(['selected-late', 'selected-later']),
      timezone: 'UTC',
    });

    expect(getDefaultActiveServiceDate(models)).toBe('2026-07-25');
  });

  it('falls back to the earliest available authoritative date', () => {
    const models = buildCandidateViewModels([
      item('later', {
        serviceDate: '2026-07-26',
        startTime: '2026-07-26T10:00:00Z',
        endTime: '2026-07-26T11:00:00Z',
        occupancyEndTime: '2026-07-26T11:15:00Z',
      }),
      item('earlier'),
    ], { selectedItemIds: new Set(), timezone: 'UTC' });

    expect(getDefaultActiveServiceDate(models)).toBe('2026-07-24');
  });

  it('uses authoritative serviceDate for overnight offsets beyond midnight', () => {
    const [model] = buildCandidateViewModels([item('overnight', {
      serviceDate: '2026-07-24',
      startTime: '2026-07-24T23:30:00Z',
      endTime: '2026-07-25T01:00:00Z',
      occupancyEndTime: '2026-07-25T01:20:00Z',
    })], { selectedItemIds: new Set(['overnight']), timezone: 'UTC' });

    expect(model).toMatchObject({
      startMinuteOffset: 1410,
      endMinuteOffset: 1500,
      occupancyEndMinuteOffset: 1520,
      timelineEligible: true,
    });
  });

  it('sorts selected first, then date, auditorium, local start, rank, and stable ID', () => {
    const models = buildCandidateViewModels([
      item('unselected'),
      item('rank-2', { rankingPosition: 2 }),
      item('rank-1-b', { rankingPosition: 1 }),
      item('rank-1-a', { rankingPosition: 1 }),
    ], {
      selectedItemIds: new Set(['rank-2', 'rank-1-b', 'rank-1-a']),
      timezone: 'UTC',
    });

    expect(sortCandidateViewModels(models).map(model => model.id))
      .toEqual(['rank-1-a', 'rank-1-b', 'rank-2', 'unselected']);
  });

  it('keeps palette assignment stable and limits the timeline to selected plus one overlay', () => {
    expect(getMoviePalette('movie-stable')).toEqual(getMoviePalette('movie-stable'));
    const models = buildCandidateViewModels([
      item('selected'),
      item('diagnostic'),
      item('other'),
    ], { selectedItemIds: new Set(['selected']), timezone: 'UTC' });
    const diagnostic = models.find(model => model.id === 'diagnostic');

    expect(getPrimaryTimelineCandidates(models, '2026-07-24', diagnostic).map(model => model.id))
      .toEqual(['selected', 'diagnostic']);
    expect(getPrimaryTimelineCandidates(models, '2026-07-24', diagnostic).filter(model => model.diagnostic))
      .toHaveLength(1);
    expect(getRelevantAuditoriums(models, '2026-07-24')).toEqual([
      expect.objectContaining({ key: 'aud-1', name: 'Phòng 1' }),
    ]);
  });
});
