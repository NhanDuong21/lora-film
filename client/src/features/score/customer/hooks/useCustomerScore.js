import { useState, useCallback, useEffect, useRef } from 'react';
import scoreCustomerService from '@/features/score/customer/services/scoreCustomerService';
import { parseApiError } from '@/utils/apiErrorHandler';

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

  const fetchScoreAndTiers = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const [scoreRes, tiersRes] = await Promise.all([
        scoreCustomerService.getScoreBalance(),
        scoreCustomerService.getMembershipTiers()
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
      if (isMounted.current) {
        setError(parseApiError(err));
      }
    } finally {
      if (isMounted.current) {
        setIsLoading(false);
      }
    }
  }, []);

  const fetchHistory = useCallback(async (params = {}) => {
    setIsHistoryLoading(true);
    try {
      const res = await scoreCustomerService.getScoreHistory(params);
      if (isMounted.current && res?.success && res?.data) {
        setHistory(res.data);
      }
    } catch (err) {
      if (isMounted.current) {
        setError((prev) => prev || parseApiError(err));
        console.error('Failed to fetch score history:', err);
      }
    } finally {
      if (isMounted.current) {
        setIsHistoryLoading(false);
      }
    }
  }, []);

  const fetchExpiringPoints = useCallback(async () => {
    setIsExpiringLoading(true);
    try {
      const res = await scoreCustomerService.getExpiringPoints();
      if (isMounted.current && res?.success && res?.data) {
        setExpiringPoints(res.data);
      }
    } catch (err) {
      if (isMounted.current) {
        setError((prev) => prev || parseApiError(err));
        console.error('Failed to fetch expiring points:', err);
      }
    } finally {
      if (isMounted.current) {
        setIsExpiringLoading(false);
      }
    }
  }, []);

  const fetchTierHistory = useCallback(async () => {
    setIsTierHistoryLoading(true);
    try {
      const res = await scoreCustomerService.getTierHistory();
      if (isMounted.current && res?.success && res?.data) {
        setTierHistory(res.data);
      }
    } catch (err) {
      if (isMounted.current) {
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
    fetchScoreAndTiers();
    fetchHistory({ page: 0, size: 10 });
    fetchExpiringPoints();
    fetchTierHistory();
  }, [fetchScoreAndTiers, fetchHistory, fetchExpiringPoints, fetchTierHistory]);

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
    refreshScore: () => {
      fetchScoreAndTiers();
      fetchExpiringPoints();
      fetchTierHistory();
      fetchHistory({ page: 0, size: 10 });
    },
    fetchHistory,
    fetchExpiringPoints,
    fetchTierHistory
  };
}
