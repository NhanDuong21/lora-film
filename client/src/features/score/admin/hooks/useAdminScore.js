import { useState, useCallback } from 'react';
import scoreAdminService from '../services/scoreAdminService';

export default function useAdminScore() {
  const [tiers, setTiers] = useState([]);
  const [isLoadingTiers, setIsLoadingTiers] = useState(false);
  const [errorTiers, setErrorTiers] = useState(null);

  const [userScore, setUserScore] = useState(null);
  const [userHistory, setUserHistory] = useState(null);
  const [expiringPoints, setExpiringPoints] = useState([]);
  const [tierHistory, setTierHistory] = useState([]);
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

  const fetchUserExpiringPoints = useCallback(async (accountId) => {
    if (!accountId) return;
    try {
      const data = await scoreAdminService.getUserExpiringPoints(accountId);
      setExpiringPoints(data || []);
      return data;
    } catch (err) {
      console.error('Failed to fetch user expiring points in admin', err);
      setExpiringPoints([]);
    }
  }, []);

  const fetchUserTierHistory = useCallback(async (accountId) => {
    if (!accountId) return;
    try {
      const data = await scoreAdminService.getUserTierHistory(accountId);
      setTierHistory(data || []);
      return data;
    } catch (err) {
      console.error('Failed to fetch user tier history in admin', err);
      setTierHistory([]);
    }
  }, []);


  const [reconciliationRuns, setReconciliationRuns] = useState(null);
  const [reconciliationDetails, setReconciliationDetails] = useState(null);
  const [auditLogs, setAuditLogs] = useState(null);
  const [dashboardStats, setDashboardStats] = useState(null);
  const [isLoadingOperations, setIsLoadingOperations] = useState(false);

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

  const adjustScore = useCallback(async (accountId, adjustmentData) => {
    setIsLoadingOperations(true);
    try {
      const res = await scoreAdminService.adjustScore(accountId, adjustmentData);
      await fetchUserScore(accountId);
      return res;
    } finally {
      setIsLoadingOperations(false);
    }
  }, [fetchUserScore]);

  const reverseAdjustment = useCallback(async (accountId, reverseData) => {
    setIsLoadingOperations(true);
    try {
      const res = await scoreAdminService.reverseAdjustment(accountId, reverseData);
      await fetchUserScore(accountId);
      return res;
    } finally {
      setIsLoadingOperations(false);
    }
  }, [fetchUserScore]);

  const recalculateTier = useCallback(async (accountId) => {
    setIsLoadingOperations(true);
    try {
      const res = await scoreAdminService.recalculateTier(accountId);
      await fetchUserScore(accountId);
      return res;
    } finally {
      setIsLoadingOperations(false);
    }
  }, [fetchUserScore]);

  const runReconciliation = useCallback(async (reconData = {}) => {
    setIsLoadingOperations(true);
    try {
      const res = await scoreAdminService.runReconciliation(reconData);
      return res;
    } finally {
      setIsLoadingOperations(false);
    }
  }, []);

  const fetchReconciliationRuns = useCallback(async (params = {}) => {
    setIsLoadingOperations(true);
    try {
      const data = await scoreAdminService.getReconciliationRuns(params);
      setReconciliationRuns(data);
      return data;
    } finally {
      setIsLoadingOperations(false);
    }
  }, []);

  const fetchReconciliationDetails = useCallback(async (params = {}) => {
    setIsLoadingOperations(true);
    try {
      const data = await scoreAdminService.getReconciliationDetails(params);
      setReconciliationDetails(data);
      return data;
    } finally {
      setIsLoadingOperations(false);
    }
  }, []);

  const fetchAuditLogs = useCallback(async (params = {}) => {
    setIsLoadingOperations(true);
    try {
      const data = await scoreAdminService.getAuditLogs(params);
      setAuditLogs(data);
      return data;
    } finally {
      setIsLoadingOperations(false);
    }
  }, []);

  const fetchDashboardStats = useCallback(async () => {
    try {
      const data = await scoreAdminService.getDashboardStats();
      setDashboardStats(data);
      return data;
    } catch (err) {
      console.error('Failed to fetch dashboard stats', err);
    }
  }, []);

  const exportData = useCallback(async (params = {}) => {
    return await scoreAdminService.exportData(params);
  }, []);

  return {
    tiers,
    isLoadingTiers,
    errorTiers,
    fetchTiers,
    createTier,
    updateTier,
    userScore,
    userHistory,
    expiringPoints,
    tierHistory,
    isLoadingUserScore,
    errorUserScore,
    fetchUserScore,
    fetchUserHistory,
    fetchUserExpiringPoints,
    fetchUserTierHistory,
    setUserScore,
    setUserHistory,
    reconciliationRuns,
    reconciliationDetails,
    auditLogs,
    dashboardStats,
    isLoadingOperations,
    adjustScore,
    reverseAdjustment,
    recalculateTier,
    runReconciliation,
    fetchReconciliationRuns,
    fetchReconciliationDetails,
    fetchAuditLogs,
    fetchDashboardStats,
    exportData
  };
}

