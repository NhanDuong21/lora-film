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

  it('translates the server-side single-seat-gap rejection', () => {
    expect(getBookingErrorMessage({
      errorCode: 'SEAT_SINGLE_GAP_NOT_ALLOWED',
      message: 'Seat selection must not leave an isolated single seat'
    })).toBe('Không được để lại một ghế trống đơn lẻ. Vui lòng chọn lại ghế.');
  });

  it('reports an upstream showtime integration failure accurately', () => {
    expect(getBookingErrorMessage({
      errorCode: 'INTEGRATION_ERROR',
      message: 'Movie Service rejected public booking context request'
    })).toBe('Không thể xác thực thông tin suất chiếu lúc này. Vui lòng thử lại sau.');
  });

  it('uses the Vietnamese fallback for an unknown raw English error', () => {
    expect(getBookingErrorMessage(
      { message: 'Unexpected database constraint violation' },
      'Không thể tạo đơn. Vui lòng thử lại.'
    )).toBe('Không thể tạo đơn. Vui lòng thử lại.');
  });

  it('localizes Booking admin lifecycle conflicts without exposing backend text', () => {
    const message = getBookingErrorMessage({
      response: {
        status: 409,
        data: {
          errorCode: 'ADMIN_LIFECYCLE_COMMAND_NOT_ALLOWED',
          message: 'Admin command is not allowed from CANCELLED to COMPLETED'
        }
      }
    });

    expect(message).toBe(
      'Không thể thực hiện thao tác này với trạng thái hiện tại của đơn.'
    );
  });
});
