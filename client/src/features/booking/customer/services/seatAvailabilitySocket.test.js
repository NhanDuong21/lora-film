import { describe, expect, it } from 'vitest';
import { applySeatAvailabilityUpdates } from './seatAvailabilitySocket';

const seat = {
  publicId: 'seat-1',
  operationalStatus: 'ACTIVE',
  blockedForShowtime: false,
  priced: true,
  price: 100000,
  sellable: true
};

describe('applySeatAvailabilityUpdates', () => {
  it('marks HELD unavailable and restores sellability after release', () => {
    const held = applySeatAvailabilityUpdates([seat], [{
      seatPublicId: 'seat-1',
      status: 'HELD',
      expiresAt: '2026-07-26T10:00:00Z'
    }])[0];
    expect(held.sellable).toBe(false);
    expect(held.reservationStatus).toBe('HELD');

    const released = applySeatAvailabilityUpdates([held], [{
      seatPublicId: 'seat-1',
      status: 'RELEASED'
    }])[0];
    expect(released.sellable).toBe(true);
    expect(released.reservationStatus).toBeNull();
  });

  it('keeps BOOKED unavailable', () => {
    const booked = applySeatAvailabilityUpdates([seat], [{
      seatPublicId: 'seat-1',
      status: 'BOOKED'
    }])[0];
    expect(booked.sellable).toBe(false);
    expect(booked.reservationStatus).toBe('BOOKED');
  });
});
