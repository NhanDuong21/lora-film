import { describe, expect, it } from 'vitest';
import { isSchedulableMovieStatus } from './movieSchedulingEligibility';

describe('movieSchedulingEligibility', () => {
  it.each(['UPCOMING', 'NOW_SHOWING'])('allows %s movies', status => {
    expect(isSchedulableMovieStatus(status)).toBe(true);
  });

  it.each(['DRAFT', 'ENDED', 'INACTIVE', undefined])('rejects %s movies', status => {
    expect(isSchedulableMovieStatus(status)).toBe(false);
  });
});
