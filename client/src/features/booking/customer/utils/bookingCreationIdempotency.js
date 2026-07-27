const STORAGE_PREFIX = 'booking:create:';
const STORAGE_VERSION = 1;

const createUuid = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, character => {
    const random = Math.random() * 16 | 0;
    const value = character === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
};

export const getBookingCreationStorageKey = showtimePublicId =>
  `${STORAGE_PREFIX}${showtimePublicId}`;

export const buildBookingCreationFingerprint = ({
  showtimePublicId,
  seatPublicIds
}) => JSON.stringify({
  showtimePublicId,
  seatPublicIds: [...seatPublicIds].sort()
});

export const getOrCreateBookingCreationKey = ({
  showtimePublicId,
  seatPublicIds
}) => {
  const storageKey = getBookingCreationStorageKey(showtimePublicId);
  const fingerprint = buildBookingCreationFingerprint({
    showtimePublicId,
    seatPublicIds
  });

  try {
    const savedValue = sessionStorage.getItem(storageKey);
    if (savedValue) {
      const savedAttempt = JSON.parse(savedValue);
      if (
        savedAttempt?.version === STORAGE_VERSION
        && savedAttempt?.fingerprint === fingerprint
        && typeof savedAttempt?.idempotencyKey === 'string'
      ) {
        return savedAttempt.idempotencyKey;
      }
    }
  } catch {
    // Legacy UUID-only or damaged storage cannot prove it belongs to this payload.
  }

  const idempotencyKey = createUuid();
  try {
    sessionStorage.setItem(storageKey, JSON.stringify({
      version: STORAGE_VERSION,
      fingerprint,
      idempotencyKey
    }));
  } catch {
    // The request remains valid when browser storage is disabled; only replay
    // protection after a lost response is unavailable for this attempt.
  }
  return idempotencyKey;
};

export const clearBookingCreationAttempt = showtimePublicId => {
  if (showtimePublicId) {
    try {
      sessionStorage.removeItem(getBookingCreationStorageKey(showtimePublicId));
    } catch {
      // Storage cleanup must not block a completed Booking lifecycle command.
    }
  }
};

export const clearAllBookingCreationAttempts = () => {
  try {
    Object.keys(sessionStorage)
      .filter(key => key.startsWith(STORAGE_PREFIX))
      .forEach(key => sessionStorage.removeItem(key));
  } catch {
    // Storage cleanup must not block cancellation or navigation.
  }
};
