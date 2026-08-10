/**
 * Production-ready query cache and deduplication utility for customer & admin scores.
 * Features:
 * - In-memory data caching with TTL (Time-To-Live / staleTime)
 * - Request deduplication (in-flight promise sharing)
 * - Automatic retry with exponential backoff for network/5xx errors
 * - AbortController support for request cancellation
 * - Cache invalidation and listeners
 */

class QueryCache {
  constructor() {
    this.cache = new Map(); // key -> { data, timestamp }
    this.inFlight = new Map(); // key -> promise
    this.listeners = new Map(); // key -> Set of callbacks
  }

  /**
   * Execute or fetch from cache with deduplication and retry.
   * @param {string} key - Unique cache key (e.g. 'customer-score', 'history-page-0')
   * @param {Function} fetcher - Async function returning data, accepts { signal }
   * @param {Object} options - { staleTime = 30000, maxRetries = 2, retryDelay = 1000, signal, forceRefresh = false }
   */
  async fetchQuery(key, fetcher, options = {}) {
    const {
      staleTime = 30000,
      maxRetries = 2,
      retryDelay = 1000,
      signal,
      forceRefresh = false
    } = options;

    // 1. Check if valid cache exists (unless forceRefresh is set)
    if (!forceRefresh) {
      const cached = this.cache.get(key);
      if (cached && Date.now() - cached.timestamp < staleTime) {
        return cached.data;
      }
    }

    // 2. Request deduplication: check if promise is already in flight
    if (this.inFlight.has(key)) {
      return this.inFlight.get(key);
    }

    // 3. Execute fetcher with retry logic
    const promise = this.executeWithRetry(fetcher, { maxRetries, retryDelay, signal })
      .then((data) => {
        this.setCache(key, data);
        this.inFlight.delete(key);
        return data;
      })
      .catch((err) => {
        this.inFlight.delete(key);
        throw err;
      });

    this.inFlight.set(key, promise);
    return promise;
  }

  async executeWithRetry(fetcher, { maxRetries, retryDelay, signal }) {
    let lastError;
    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      if (signal?.aborted) {
        throw new DOMException('Aborted', 'AbortError');
      }
      try {
        return await fetcher({ signal });
      } catch (err) {
        lastError = err;
        // Do not retry 4xx errors (client errors like validation or conflict) or AbortError
        const status = err?.response?.status || err?.status;
        if (err?.name === 'AbortError' || (status >= 400 && status < 500)) {
          throw err;
        }
        if (attempt < maxRetries) {
          const delay = retryDelay * Math.pow(2, attempt);
          await new Promise((res) => setTimeout(res, delay));
        }
      }
    }
    throw lastError;
  }

  setCache(key, data) {
    this.cache.set(key, { data, timestamp: Date.now() });
    this.notifyListeners(key, data);
  }

  getCache(key) {
    const cached = this.cache.get(key);
    return cached ? cached.data : null;
  }

  invalidateQueries(keyPrefix = '') {
    for (const key of this.cache.keys()) {
      if (key.startsWith(keyPrefix)) {
        this.cache.delete(key);
        this.inFlight.delete(key);
      }
    }
  }

  clear() {
    this.cache.clear();
    this.inFlight.clear();
  }

  subscribe(key, callback) {
    if (!this.listeners.has(key)) {
      this.listeners.set(key, new Set());
    }
    this.listeners.get(key).add(callback);
    return () => {
      const set = this.listeners.get(key);
      if (set) {
        set.delete(callback);
        if (set.size === 0) this.listeners.delete(key);
      }
    };
  }

  notifyListeners(key, data) {
    const set = this.listeners.get(key);
    if (set) {
      set.forEach((cb) => cb(data));
    }
  }
}

export const queryCache = new QueryCache();
export default queryCache;
