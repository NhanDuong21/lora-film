import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  buildShowtimeQueryCacheKey,
  clearShowtimeQueryCache,
  readShowtimeQueryCache,
  runShowtimeQueryOnce,
  SHOWTIME_QUERY_CACHE_POLICY,
  writeShowtimeQueryCache,
} from './showtimeQueryCache';

describe('showtimeQueryCache', () => {
  beforeEach(() => clearShowtimeQueryCache());

  it('builds the same key regardless of parameter insertion order', () => {
    expect(buildShowtimeQueryCacheKey('admin', { date: '2026-08-11', page: 0 }))
      .toBe(buildShowtimeQueryCacheKey('admin', { page: 0, date: '2026-08-11' }));
  });

  it('serves a recent response as fresh and retains an older response for background refresh', () => {
    writeShowtimeQueryCache('query', { data: ['cached'] }, 1_000);

    expect(readShowtimeQueryCache('query', 1_000 + SHOWTIME_QUERY_CACHE_POLICY.freshForMs))
      .toMatchObject({ response: { data: ['cached'] }, isFresh: true });
    expect(readShowtimeQueryCache('query', 1_001 + SHOWTIME_QUERY_CACHE_POLICY.freshForMs))
      .toMatchObject({ response: { data: ['cached'] }, isFresh: false });
    expect(readShowtimeQueryCache('query', 1_001 + SHOWTIME_QUERY_CACHE_POLICY.retainForMs)).toBeNull();
  });

  it('deduplicates concurrent requests for the same query', async () => {
    let resolveRequest;
    const request = vi.fn(() => new Promise(resolve => { resolveRequest = resolve; }));
    const first = runShowtimeQueryOnce('query', request);
    const second = runShowtimeQueryOnce('query', request);

    expect(first).toBe(second);
    await Promise.resolve();
    expect(request).toHaveBeenCalledTimes(1);
    resolveRequest({ data: [] });
    await expect(first).resolves.toEqual({ data: [] });
  });
});
