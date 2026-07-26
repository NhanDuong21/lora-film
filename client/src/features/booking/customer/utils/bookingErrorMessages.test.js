import { describe, expect, it } from 'vitest';
import {
  getBookingErrorCode,
  getBookingErrorMessage,
  seatConflictErrorCodes
} from './bookingErrorMessages';

describe('booking error messages', () => {
  it('maps plain interceptor payloads to Vietnamese conflict guidance', () => {
    const error = { errorCode: 'BOOKING_SEAT_CONFLICT', message: 'One or more seats...' };
    expect(getBookingErrorCode(error)).toBe('BOOKING_SEAT_CONFLICT');
    expect(getBookingErrorMessage(error)).toContain('ghế');
    expect(seatConflictErrorCodes.has(getBookingErrorCode(error))).toBe(true);
  });

  it('supports Axios-shaped errors and preserves customer-safe Vietnamese messages', () => {
    expect(getBookingErrorMessage({
      response: { data: { errorCode: 'SEAT_003' } }
    })).toContain('đang được khách khác giữ');
    expect(getBookingErrorMessage({ message: 'Lỗi khác' })).toBe('Lỗi khác');
  });

  it('translates the backend idempotency conflict instead of leaking English', () => {
    const message = getBookingErrorMessage({
      errorCode: 'IDEMPOTENCY_PAYLOAD_CONFLICT',
      message: 'The idempotency key was reused with a different request payload'
    });

    expect(message).toContain('Phiên đặt vé cũ');
    expect(message).not.toContain('idempotency');
  });

  it('uses the Vietnamese fallback for an unknown raw English error', () => {
    expect(getBookingErrorMessage(
      { message: 'Unexpected database constraint violation' },
      'Không thể tạo đơn. Vui lòng thử lại.'
    )).toBe('Không thể tạo đơn. Vui lòng thử lại.');
  });
});
