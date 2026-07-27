import { useState, useCallback, useEffect, useRef } from 'react';
import scoreCustomerService from '@/features/score/customer/services/scoreCustomerService';
import { parseApiError } from '@/utils/apiErrorHandler';
import queryCache from '@/utils/queryCache';

export default function useCustomerScore() {
  const [scoreData, setScoreData] = useState(null);
  const [tiers, setTiers] = useState([]);
  const [history, setHistory] = useState({ content: [], totalPages: 0, totalElements: 0, number: 0 });
  const [expiringPoints, setExpiringPoints] = useState([]);
  const [tierHistory, setTierHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);
  const [isExpiringLoading, setIsExpiringLoading] = useState(false);
  const [isTierHistoryLoading, setIsTierHistoryLoading] = useState(false);
  const [error, setError] = useState('');

  const isMounted = useRef(true);

  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
    };
  }, []);

  const fetchScoreAndTiers = useCallback(async (options = {}) => {
    setIsLoading(true);
    setError('');
    try {
      const [scoreRes, tiersRes] = await Promise.all([
        queryCache.fetchQuery('customer-score-balance', ({ signal }) => scoreCustomerService.getScoreBalance({ signal }), { staleTime: 30000, ...options }),
        queryCache.fetchQuery('customer-membership-tiers', ({ signal }) => scoreCustomerService.getMembershipTiers({ signal }), { staleTime: 60000, ...options })
      ]);

      if (!isMounted.current) return;

      if (scoreRes?.success && scoreRes?.data) {
        setScoreData(scoreRes.data);
      } else {
        setError('Không thể tải thông tin điểm thưởng.');
      }

      if (tiersRes?.success && tiersRes?.data) {
        setTiers(tiersRes.data);
      }
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        setError(parseApiError(err));
      }
    } finally {
      if (isMounted.current) {
        setIsLoading(false);
      }
    }
  }, []);

  const fetchHistory = useCallback(async (params = {}, options = {}) => {
    setIsHistoryLoading(true);
    try {
      const cacheKey = 'customer-score-history-' + JSON.stringify(params);
      const res = await queryCache.fetchQuery(cacheKey, ({ signal }) => scoreCustomerService.getScoreHistory(params, { signal }), { staleTime: 15000, ...options });
      if (isMounted.current && res?.success && res?.data) {
        setHistory(res.data);
      }
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        setError((prev) => prev || parseApiError(err));
        console.error('Failed to fetch score history:', err);
      }
    } finally {
      if (isMounted.current) {
        setIsHistoryLoading(false);
      }
    }
  }, []);

  const fetchExpiringPoints = useCallback(async (options = {}) => {
    setIsExpiringLoading(true);
    try {
      const res = await queryCache.fetchQuery('customer-expiring-points', ({ signal }) => scoreCustomerService.getExpiringPoints({ signal }), { staleTime: 30000, ...options });
      if (isMounted.current && res?.success && res?.data) {
        setExpiringPoints(res.data);
      }
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        setError((prev) => prev || parseApiError(err));
        console.error('Failed to fetch expiring points:', err);
      }
    } finally {
      if (isMounted.current) {
        setIsExpiringLoading(false);
      }
    }
  }, []);

  const fetchTierHistory = useCallback(async (options = {}) => {
    setIsTierHistoryLoading(true);
    try {
      const res = await queryCache.fetchQuery('customer-tier-history', ({ signal }) => scoreCustomerService.getTierHistory({ signal }), { staleTime: 30000, ...options });
      if (isMounted.current && res?.success && res?.data) {
        setTierHistory(res.data);
      }
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        setError((prev) => prev || parseApiError(err));
        console.error('Failed to fetch tier history:', err);
      }
    } finally {
      if (isMounted.current) {
        setIsTierHistoryLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    fetchScoreAndTiers({ signal });
    fetchHistory({ page: 0, size: 10 }, { signal });
    fetchExpiringPoints({ signal });
    fetchTierHistory({ signal });

    return () => {
      controller.abort();
    };
  }, [fetchScoreAndTiers, fetchHistory, fetchExpiringPoints, fetchTierHistory]);

  const refreshScore = useCallback(() => {
    queryCache.invalidateQueries('customer-');
    fetchScoreAndTiers({ forceRefresh: true });
    fetchExpiringPoints({ forceRefresh: true });
    fetchTierHistory({ forceRefresh: true });
    fetchHistory({ page: 0, size: 10 }, { forceRefresh: true });
  }, [fetchScoreAndTiers, fetchExpiringPoints, fetchTierHistory, fetchHistory]);

  return {
    scoreData,
    tiers,
    history,
    expiringPoints,
    tierHistory,
    isLoading,
    isHistoryLoading,
    isExpiringLoading,
    isTierHistoryLoading,
    error,
    refreshScore,
    fetchHistory,
    fetchExpiringPoints,
    fetchTierHistory
  };
}
