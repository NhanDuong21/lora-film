import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  createPaymentHandoff,
  getOrCreatePaymentAttemptKey
} from './paymentHandoffService';

vi.mock('@/services/apiClient', () => ({
  default: {
    post: vi.fn()
  }
}));

describe('paymentHandoffService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
  });

  it('sends only the public Booking identity and selected method', async () => {
    apiClient.post.mockResolvedValue({
      data: {
        data: {
          paymentPublicId: '22222222-2222-4222-8222-222222222222'
        }
      }
    });

    await createPaymentHandoff({
      bookingPublicId: '11111111-1111-4111-8111-111111111111',
      paymentMethod: 'VNPAY',
      idempotencyKey: 'attempt-key'
    });

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/payments',
      {
        bookingPublicId: '11111111-1111-4111-8111-111111111111',
        paymentMethod: 'VNPAY'
      },
      {
        headers: {
          'Idempotency-Key': 'attempt-key'
        }
      }
    );
    const requestBody = apiClient.post.mock.calls[0][1];
    expect(requestBody).not.toHaveProperty('amount');
    expect(requestBody).not.toHaveProperty('currency');
    expect(requestBody).not.toHaveProperty('expiresAt');
  });

  it('reuses the attempt key across refresh/retry for the same payload', () => {
    const first = getOrCreatePaymentAttemptKey('booking-1', 'VNPAY');
    const replay = getOrCreatePaymentAttemptKey('booking-1', 'VNPAY');

    expect(replay).toBe(first);
  });
});

