import { describe, expect, it } from 'vitest';
import {
  formatHoldTimeLeft,
  getBookingDeadline,
  getBookingRecoveryState
} from './bookingRecovery';

describe('pending booking recovery', () => {
  const now = Date.parse('2026-07-26T12:00:00Z');

  it('allows recovery only while a pending booking deadline is still in the future', () => {
    const active = getBookingRecoveryState({
      status: 'PENDING_PAYMENT',
      expiredAt: '2026-07-26T12:05:00Z'
    }, now);

    expect(active.canRecover).toBe(true);
    expect(active.isExpiredPending).toBe(false);
    expect(active.remainingSeconds).toBe(300);
  });

  it('treats the exact deadline and missing deadline as non-recoverable', () => {
    expect(getBookingRecoveryState({
      status: 'PENDING_PAYMENT',
      expiredAt: '2026-07-26T12:00:00Z'
    }, now).isExpiredPending).toBe(true);
    expect(getBookingRecoveryState({
      status: 'PENDING_PAYMENT'
    }, now).canRecover).toBe(false);
  });

  it('does not expose recovery actions for a terminal booking', () => {
    expect(getBookingRecoveryState({
      status: 'CANCELLED',
      expiredAt: '2026-07-26T12:05:00Z'
    }, now).canRecover).toBe(false);
  });

  it('normalizes deadline aliases and formats the countdown', () => {
    expect(getBookingDeadline({ paymentDeadline: 'deadline' })).toBe('deadline');
    expect(formatHoldTimeLeft(65)).toBe('01:05');
    expect(formatHoldTimeLeft(-1)).toBe('00:00');
  });
});
