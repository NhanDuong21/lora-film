import { describe, expect, it } from 'vitest';
import {
  buildQuickNonOverlappingSelection,
  buildSelectedItemsIndex,
  findSelectionBlock,
  occupancyIntervalsOverlap,
  SELECTION_BLOCK_TYPES,
  validateBulkSelection,
  validatePreviewItemInterval,
  validateSingleSelectionChange,
} from './autoSchedulePreviewSelection';

const item = ({
  id,
  auditorium = 'aud-1',
  start,
  end,
  occupancyEnd,
  score = 1,
  selected = false,
  validationStatus = 'VALID',
  applyStatus = 'PENDING',
}) => ({
  itemPublicId: id,
  auditoriumPublicId: auditorium,
  startTime: start,
  endTime: end,
  occupancyEndTime: occupancyEnd,
  score,
  rankingPosition: 1,
  selected,
  validationStatus,
  applyStatus,
});

describe('canonical preview occupancy selection helpers', () => {
  it('detects cleaning-only overlap and allows exact adjacency', () => {
    const first = item({
      id: 'first', start: '2026-07-24T10:00:00Z', end: '2026-07-24T11:00:00Z', occupancyEnd: '2026-07-24T11:15:00Z',
    });
    const cleaningConflict = item({
      id: 'conflict', start: '2026-07-24T11:05:00Z', end: '2026-07-24T12:00:00Z', occupancyEnd: '2026-07-24T12:15:00Z',
    });
    const adjacent = item({
      id: 'adjacent', start: '2026-07-24T11:15:00Z', end: '2026-07-24T12:15:00Z', occupancyEnd: '2026-07-24T12:30:00Z',
    });
    const firstInterval = validatePreviewItemInterval(first).interval;

    expect(occupancyIntervalsOverlap(firstInterval, validatePreviewItemInterval(cleaningConflict).interval)).toBe(true);
    expect(occupancyIntervalsOverlap(firstInterval, validatePreviewItemInterval(adjacent).interval)).toBe(false);
  });

  it('looks across calendar groups but keeps auditoriums independent', () => {
    const selected = item({
      id: 'selected', start: '2026-07-24T23:30:00Z', end: '2026-07-25T00:30:00Z', occupancyEnd: '2026-07-25T00:45:00Z',
    });
    const nextDate = item({
      id: 'next', start: '2026-07-25T00:40:00Z', end: '2026-07-25T01:40:00Z', occupancyEnd: '2026-07-25T01:55:00Z',
    });
    const otherAuditorium = { ...nextDate, itemPublicId: 'other', auditoriumPublicId: 'aud-2' };
    const index = buildSelectedItemsIndex([selected, nextDate, otherAuditorium], new Set(['selected']));

    expect(findSelectionBlock(nextDate, index)?.type).toBe(SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP);
    expect(findSelectionBlock(otherAuditorium, index)).toBeNull();
  });

  it('blocks malformed candidates and additions beside malformed selected items', () => {
    const malformed = item({
      id: 'malformed', start: '2026-07-24T10:00:00Z', end: '2026-07-24T11:00:00Z', occupancyEnd: null,
    });
    const candidate = item({
      id: 'candidate', start: '2026-07-24T12:00:00Z', end: '2026-07-24T13:00:00Z', occupancyEnd: '2026-07-24T13:15:00Z',
    });
    const index = buildSelectedItemsIndex([malformed, candidate], new Set(['malformed']));

    expect(findSelectionBlock(malformed, index)?.type).toBe(SELECTION_BLOCK_TYPES.MALFORMED_ITEM);
    expect(findSelectionBlock(candidate, index)?.type).toBe(SELECTION_BLOCK_TYPES.MALFORMED_SELECTED_ITEM);
    expect(validateSingleSelectionChange([malformed], new Set(['malformed']), 'malformed', false).valid).toBe(true);
  });

  it('rejects unsafe single and bulk selections with the same rules', () => {
    const first = item({
      id: 'first', start: '2026-07-24T10:00:00Z', end: '2026-07-24T11:00:00Z', occupancyEnd: '2026-07-24T11:15:00Z',
    });
    const second = item({
      id: 'second', start: '2026-07-24T11:05:00Z', end: '2026-07-24T12:00:00Z', occupancyEnd: '2026-07-24T12:15:00Z',
    });
    const rejected = { ...second, itemPublicId: 'rejected', validationStatus: 'REJECTED' };

    expect(validateSingleSelectionChange([first, second], new Set(['first']), 'second', true).type)
      .toBe(SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP);
    expect(validateBulkSelection([first, second], ['first', 'second']).type)
      .toBe(SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP);
    expect(validateSingleSelectionChange([rejected], new Set(), 'rejected', true).type)
      .toBe(SELECTION_BLOCK_TYPES.REJECTED);
  });

  it.each(['CREATED', 'SKIPPED', 'CONFLICT', 'FAILED'])(
    'blocks the exact non-pending backend item state %s',
    applyStatus => {
      const candidate = item({
        id: applyStatus.toLowerCase(),
        start: '2026-07-24T10:00:00Z',
        end: '2026-07-24T11:00:00Z',
        occupancyEnd: '2026-07-24T11:15:00Z',
        applyStatus,
      });

      expect(validateSingleSelectionChange([candidate], new Set(), candidate.itemPublicId, true).type)
        .toBe(SELECTION_BLOCK_TYPES.ITEM_NOT_PENDING);
      expect(validateSingleSelectionChange(
        [candidate],
        new Set([candidate.itemPublicId]),
        candidate.itemPublicId,
        false,
      ).type).toBe(SELECTION_BLOCK_TYPES.ITEM_NOT_PENDING);
      expect(validateBulkSelection([candidate], [candidate.itemPublicId]).type)
        .toBe(SELECTION_BLOCK_TYPES.ITEM_NOT_PENDING);
    },
  );

  it('validates the complete proposed bulk final set', () => {
    const first = item({
      id: 'first', start: '2026-07-24T10:00:00Z', end: '2026-07-24T11:00:00Z', occupancyEnd: '2026-07-24T11:15:00Z',
    });
    const cleaningConflict = item({
      id: 'cleaning-conflict', start: '2026-07-24T11:05:00Z', end: '2026-07-24T12:05:00Z', occupancyEnd: '2026-07-24T12:20:00Z',
    });
    const adjacent = item({
      id: 'adjacent', start: '2026-07-24T11:15:00Z', end: '2026-07-24T12:15:00Z', occupancyEnd: '2026-07-24T12:30:00Z',
    });

    // The validator receives the complete proposed ID set; it does not compare changes
    // independently against a previous selection.
    expect(validateBulkSelection([first, cleaningConflict], ['first', 'cleaning-conflict']).type)
      .toBe(SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP);
    expect(validateBulkSelection([first, adjacent], ['first', 'adjacent'])).toEqual({ valid: true });
    expect(validateBulkSelection([first, cleaningConflict], [])).toEqual({ valid: true });
  });

  it('keeps the manual helper greedy, occupancy-safe, and distinct from S3 WIS', () => {
    const long = item({
      id: 'long', start: '2026-07-24T18:00:00Z', end: '2026-07-24T20:30:00Z', occupancyEnd: '2026-07-24T20:45:00Z', score: 85,
    });
    const shortOne = item({
      id: 'short-1', start: '2026-07-24T18:00:00Z', end: '2026-07-24T19:00:00Z', occupancyEnd: '2026-07-24T19:15:00Z', score: 60,
    });
    const shortTwo = item({
      id: 'short-2', start: '2026-07-24T19:15:00Z', end: '2026-07-24T20:15:00Z', occupancyEnd: '2026-07-24T20:30:00Z', score: 60,
    });
    const rejected = { ...shortTwo, itemPublicId: 'rejected', validationStatus: 'REJECTED' };
    const malformed = { ...shortTwo, itemPublicId: 'malformed', occupancyEndTime: null };
    const source = [long, shortOne, shortTwo, rejected, malformed];
    const snapshot = structuredClone(source);

    expect(buildQuickNonOverlappingSelection(source)).toEqual(['long']);
    expect(long.score).toBeLessThan(shortOne.score + shortTwo.score);
    expect(source).toEqual(snapshot);
  });
});
