import { describe, expect, it } from 'vitest';
import { buildAutoScheduleRecreateDraft } from './autoScheduleRecreateDraft';

describe('buildAutoScheduleRecreateDraft', () => {
  it('preserves the original scope and deduplicates room and movie version inputs', () => {
    expect(buildAutoScheduleRecreateDraft({
      cinemaPublicId: 'cinema-1',
      scheduleFrom: '2026-08-04',
      scheduleTo: '2026-08-06',
      slotGranularityMinutes: 30,
    }, [
      { auditoriumPublicId: 'aud-1', movieVersionPublicId: 'version-1' },
      { auditoriumPublicId: 'aud-1', movieVersionPublicId: 'version-2' },
      { auditoriumPublicId: 'aud-2', movieVersionPublicId: 'version-1' },
    ])).toEqual({
      cinemaPublicId: 'cinema-1',
      scheduleFrom: '2026-08-04',
      scheduleTo: '2026-08-06',
      slotGranularityMinutes: 30,
      auditoriumPublicIds: ['aud-1', 'aud-2'],
      movieVersionPublicIds: ['version-1', 'version-2'],
    });
  });
});
