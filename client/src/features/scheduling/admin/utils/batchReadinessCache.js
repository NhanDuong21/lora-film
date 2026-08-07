const CACHE_PREFIX = 'lorafilm:showtime-batch-readiness:';
const CACHE_LIFETIME_MS = 15 * 60 * 1000;

const getStorage = () => {
  try {
    return typeof window !== 'undefined' ? window.sessionStorage : null;
  } catch {
    return null;
  }
};

export const readBatchReadinessCache = batchId => {
  if (!batchId) return null;
  const storage = getStorage();
  if (!storage) return null;
  try {
    const cached = JSON.parse(storage.getItem(`${CACHE_PREFIX}${batchId}`) || 'null');
    if (!cached?.summary || cached.summary.batchId !== batchId || !Number.isFinite(cached.checkedAt)) return null;
    if (Date.now() - cached.checkedAt > CACHE_LIFETIME_MS) {
      storage.removeItem(`${CACHE_PREFIX}${batchId}`);
      return null;
    }
    return cached;
  } catch {
    storage.removeItem(`${CACHE_PREFIX}${batchId}`);
    return null;
  }
};

export const writeBatchReadinessCache = (batchId, summary) => {
  if (!batchId || !summary) return;
  const storage = getStorage();
  if (!storage) return;
  try {
    storage.setItem(`${CACHE_PREFIX}${batchId}`, JSON.stringify({
      checkedAt: Date.now(),
      summary: { ...summary, batchId: summary.batchId || batchId },
    }));
  } catch {
    // Bộ nhớ phiên có thể bị trình duyệt giới hạn; kiểm tra trực tiếp vẫn tiếp tục hoạt động.
  }
};

export const clearBatchReadinessCache = batchId => {
  if (!batchId) return;
  getStorage()?.removeItem(`${CACHE_PREFIX}${batchId}`);
};
