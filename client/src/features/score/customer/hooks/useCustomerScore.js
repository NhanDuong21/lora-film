import { useState, useCallback, useEffect } from 'react';
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

  const fetchScoreAndTiers = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const [scoreRes, tiersRes] = await Promise.all([
        scoreCustomerService.getScoreBalance(),
        scoreCustomerService.getMembershipTiers()
      ]);

      if (scoreRes?.success && scoreRes?.data) {
        setScoreData(scoreRes.data);
      } else {
        setError('Không thể tải thông tin điểm thưởng.');
      }

      if (tiersRes?.success && tiersRes?.data) {
        setTiers(tiersRes.data);
      }
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fetchHistory = useCallback(async (params = {}) => {
    setIsHistoryLoading(true);
    try {
      const res = await scoreCustomerService.getScoreHistory(params);
      if (res?.success && res?.data) {
        setHistory(res.data);
      }
    } catch (err) {
      console.error('Failed to fetch score history:', err);
    } finally {
      setIsHistoryLoading(false);
    }
  }, []);

  const fetchExpiringPoints = useCallback(async () => {
    setIsExpiringLoading(true);
    try {
      const res = await scoreCustomerService.getExpiringPoints();
      if (res?.success && res?.data) {
        setExpiringPoints(res.data);
      }
    } catch (err) {
      console.error('Failed to fetch expiring points:', err);
    } finally {
      setIsExpiringLoading(false);
    }
  }, []);

  const fetchTierHistory = useCallback(async () => {
    setIsTierHistoryLoading(true);
    try {
      const res = await scoreCustomerService.getTierHistory();
      if (res?.success && res?.data) {
        setTierHistory(res.data);
      }
    } catch (err) {
      console.error('Failed to fetch tier history:', err);
    } finally {
      setIsTierHistoryLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
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
    },
    fetchHistory,
    fetchExpiringPoints,
    fetchTierHistory
  };
}

