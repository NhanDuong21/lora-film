import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  clearAllBookingCreationAttempts,
  clearBookingCreationAttempt,
  getBookingCreationStorageKey,
  getOrCreateBookingCreationKey
} from './bookingCreationIdempotency';

describe('booking creation idempotency attempts', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.spyOn(crypto, 'randomUUID')
      .mockReturnValueOnce('11111111-1111-4111-8111-111111111111')
      .mockReturnValueOnce('22222222-2222-4222-8222-222222222222')
      .mockReturnValueOnce('33333333-3333-4333-8333-333333333333');
  });

  it('reuses one key only for the same normalized request payload', () => {
    const first = getOrCreateBookingCreationKey({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-b', 'seat-a']
    });
    const replay = getOrCreateBookingCreationKey({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-a', 'seat-b']
    });

    expect(replay).toBe(first);
  });

  it('rotates the key when the selected seat payload changes', () => {
    const first = getOrCreateBookingCreationKey({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-a']
    });
    const changed = getOrCreateBookingCreationKey({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-b']
    });

    expect(changed).not.toBe(first);
  });

  it('does not reuse the legacy UUID-only value because its payload is unknown', () => {
    sessionStorage.setItem(
      getBookingCreationStorageKey('showtime-1'),
      'legacy-request-key'
    );

    expect(getOrCreateBookingCreationKey({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-a']
    })).toBe('11111111-1111-4111-8111-111111111111');
  });

  it('clears a completed or abandoned creation attempt', () => {
    getOrCreateBookingCreationKey({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-a']
    });

    clearBookingCreationAttempt('showtime-1');

    expect(sessionStorage.getItem(
      getBookingCreationStorageKey('showtime-1')
    )).toBeNull();
  });

  it('clears all saved creation attempts after a customer cancels a booking', () => {
    sessionStorage.setItem('unrelated:key', 'keep-me');
    getOrCreateBookingCreationKey({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-a']
    });
    getOrCreateBookingCreationKey({
      showtimePublicId: 'showtime-2',
      seatPublicIds: ['seat-b']
    });

    clearAllBookingCreationAttempts();

    expect(sessionStorage.getItem('booking:create:showtime-1')).toBeNull();
    expect(sessionStorage.getItem('booking:create:showtime-2')).toBeNull();
    expect(sessionStorage.getItem('unrelated:key')).toBe('keep-me');
  });
});
