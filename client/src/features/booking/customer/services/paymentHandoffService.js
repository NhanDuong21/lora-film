import apiClient from '@/services/apiClient';

const PAYMENT_ATTEMPT_KEY_PREFIX = 'lorafilm.payment-attempt';

const storageKey = (bookingPublicId, paymentMethod) =>
  `${PAYMENT_ATTEMPT_KEY_PREFIX}:${bookingPublicId}:${paymentMethod}`;

export const getOrCreatePaymentAttemptKey = (bookingPublicId, paymentMethod) => {
  const key = storageKey(bookingPublicId, paymentMethod);
  const existing = sessionStorage.getItem(key);
  if (existing) return existing;

  const generated = crypto.randomUUID();
  sessionStorage.setItem(key, generated);
  return generated;
};

export const resetPaymentAttemptKey = (bookingPublicId, paymentMethod) => {
  if (!bookingPublicId || !paymentMethod) return;
  sessionStorage.removeItem(storageKey(bookingPublicId, paymentMethod));
};

export const createPaymentHandoff = async ({
  bookingPublicId,
  paymentMethod,
  idempotencyKey
}) => {
  const response = await apiClient.post(
    '/api/payments',
    {
      bookingPublicId,
      paymentMethod
    },
    {
      headers: {
        'Idempotency-Key': idempotencyKey
      }
    }
  );
  return response.data?.data ?? response.data;
};

