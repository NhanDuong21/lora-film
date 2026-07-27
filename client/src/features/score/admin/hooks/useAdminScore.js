import { useState, useCallback } from 'react';
import scoreAdminService from '../services/scoreAdminService';

export default function useAdminScore() {
  const [tiers, setTiers] = useState([]);
  const [isLoadingTiers, setIsLoadingTiers] = useState(false);
  const [errorTiers, setErrorTiers] = useState(null);

  const [userScore, setUserScore] = useState(null);
  const [userHistory, setUserHistory] = useState(null);
  const [isLoadingUserScore, setIsLoadingUserScore] = useState(false);
  const [errorUserScore, setErrorUserScore] = useState(null);

  const fetchTiers = useCallback(async () => {
    setIsLoadingTiers(true);
    setErrorTiers(null);
    try {
      const data = await scoreAdminService.getAllTiers();
      setTiers(data || []);
      return data;
    } catch (err) {
      const msg = err.response?.data?.message || 'Không thể tải danh sách hạng thẻ';
      setErrorTiers(msg);
      throw err;
    } finally {
      setIsLoadingTiers(false);
    }
  }, []);

  const fetchUserScore = useCallback(async (accountId) => {
    if (!accountId) return;
    setIsLoadingUserScore(true);
    setErrorUserScore(null);
    try {
      const data = await scoreAdminService.getScoreByAccount(accountId);
      setUserScore(data);
      return data;
    } catch (err) {
      const msg = err.response?.data?.message || `Không tìm thấy điểm thưởng cho tài khoản ID: ${accountId}`;
      setErrorUserScore(msg);
      setUserScore(null);
      throw err;
    } finally {
      setIsLoadingUserScore(false);
    }
  }, []);

  const fetchUserHistory = useCallback(async (accountId, params = {}) => {
    if (!accountId) return;
    try {
      const data = await scoreAdminService.getScoreHistoryByAccount(accountId, params);
      setUserHistory(data);
      return data;
    } catch (err) {
      console.error('Failed to fetch user score history in admin', err);
      setUserHistory(null);
    }
  }, []);

  const createTier = useCallback(async (tierData) => {
    const created = await scoreAdminService.createTier(tierData);
    await fetchTiers();
    return created;
  }, [fetchTiers]);

  const updateTier = useCallback(async (tierCode, tierData) => {
    const updated = await scoreAdminService.updateTier(tierCode, tierData);
    await fetchTiers();
    return updated;
  }, [fetchTiers]);

  return {
    tiers,
    isLoadingTiers,
    errorTiers,
    fetchTiers,
    createTier,
    updateTier,
    userScore,
    userHistory,
    isLoadingUserScore,
    errorUserScore,
    fetchUserScore,
    fetchUserHistory,
    setUserScore,
    setUserHistory
  };
}
