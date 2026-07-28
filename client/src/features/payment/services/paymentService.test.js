import { describe, expect, it } from 'vitest';
import { paymentErrorCode, paymentErrorMessage } from './paymentService';

describe('paymentErrorMessage', () => {
  it('maps known backend error codes to clear Vietnamese guidance', () => {
    expect(paymentErrorMessage({
      response: { data: { errorCode: 'IDEMPOTENCY_KEY_REUSED' } }
    })).toBe('Yêu cầu này đã được dùng cho một thao tác khác. Vui lòng thử lại.');

    expect(paymentErrorMessage({
      errorCode: 'PAYMENT_PROVIDER_SESSION_ACTIVE'
    })).toContain('Phiên thanh toán đang hoạt động');

    const cancelled = {
      response: { data: { errorCode: 'BOOKING_CANCELLED' } }
    };
    expect(paymentErrorCode(cancelled)).toBe('BOOKING_CANCELLED');
    expect(paymentErrorMessage(cancelled)).toContain('ghế đã được trả lại');
  });

  it('never exposes an unknown English backend message to customers', () => {
    expect(paymentErrorMessage({
      message: 'The idempotency key was reused with a different request payload',
      response: { data: { message: 'Internal provider failure' } }
    })).toBe('Không thể xử lý thanh toán lúc này. Vui lòng thử lại sau.');
  });
});
