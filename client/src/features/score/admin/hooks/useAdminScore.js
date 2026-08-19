import { useState, useCallback, useRef, useEffect } from 'react';
import scoreAdminService from '../services/scoreAdminService';
import queryCache from '@/utils/queryCache';

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

  const isMounted = useRef(true);

  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
    };
  }, []);

  const fetchTiers = useCallback(async (options = {}) => {
    setIsLoadingTiers(true);
    setErrorTiers(null);
    try {
      const data = await queryCache.fetchQuery('admin-tiers', ({ signal }) => scoreAdminService.getAllTiers({ signal }), { staleTime: 60000, ...options });
      if (isMounted.current) setTiers(data || []);
      return data;
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        const msg = err.response?.data?.message || 'Không thể tải danh sách hạng thẻ';
        setErrorTiers(msg);
      }
      throw err;
    } finally {
      if (isMounted.current) setIsLoadingTiers(false);
    }
  }, []);

  const fetchUserScore = useCallback(async (accountId, options = {}) => {
    if (!accountId) return;
    setIsLoadingUserScore(true);
    setErrorUserScore(null);
    try {
      const data = await queryCache.fetchQuery(`admin-user-score-${accountId}`, ({ signal }) => scoreAdminService.getScoreByAccount(accountId, { signal }), { staleTime: 15000, ...options });
      if (isMounted.current) setUserScore(data);
      return data;
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        const msg = err.response?.data?.message || `Không tìm thấy điểm thưởng cho tài khoản ID: ${accountId}`;
        setErrorUserScore(msg);
        setUserScore(null);
      }
      throw err;
    } finally {
      if (isMounted.current) setIsLoadingUserScore(false);
    }
  }, []);

  const fetchUserHistory = useCallback(async (accountId, params = {}, options = {}) => {
    if (!accountId) return;
    try {
      const key = `admin-user-history-${accountId}-${JSON.stringify(params)}`;
      const data = await queryCache.fetchQuery(key, ({ signal }) => scoreAdminService.getScoreHistoryByAccount(accountId, params, { signal }), { staleTime: 15000, ...options });
      if (isMounted.current) setUserHistory(data);
      return data;
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        const msg = err.response?.data?.message || 'Lỗi khi tải lịch sử giao dịch';
        setErrorUserScore((prev) => prev || msg);
        setUserHistory(null);
      }
      throw err;
    }
  }, []);

  const fetchUserExpiringPoints = useCallback(async (accountId, options = {}) => {
    if (!accountId) return;
    try {
      const data = await queryCache.fetchQuery(`admin-user-expiring-${accountId}`, ({ signal }) => scoreAdminService.getUserExpiringPoints(accountId, { signal }), { staleTime: 30000, ...options });
      if (isMounted.current) setExpiringPoints(data || []);
      return data;
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        const msg = err.response?.data?.message || 'Lỗi khi tải danh sách điểm sắp hết hạn';
        setErrorUserScore((prev) => prev || msg);
        setExpiringPoints([]);
      }
      throw err;
    }
  }, []);

  const fetchUserTierHistory = useCallback(async (accountId, options = {}) => {
    if (!accountId) return;
    try {
      const data = await queryCache.fetchQuery(`admin-user-tier-history-${accountId}`, ({ signal }) => scoreAdminService.getUserTierHistory(accountId, { signal }), { staleTime: 30000, ...options });
      if (isMounted.current) setTierHistory(data || []);
      return data;
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        const msg = err.response?.data?.message || 'Lỗi khi tải lịch sử thăng/giáng hạng';
        setErrorUserScore((prev) => prev || msg);
        setTierHistory([]);
      }
      throw err;
    }
  }, []);

  const [reconciliationRuns, setReconciliationRuns] = useState(null);
  const [reconciliationDetails, setReconciliationDetails] = useState(null);
  const [auditLogs, setAuditLogs] = useState(null);
  const [dashboardStats, setDashboardStats] = useState(null);
  const [isLoadingOperations, setIsLoadingOperations] = useState(false);

  const createTier = useCallback(async (tierData) => {
    const created = await scoreAdminService.createTier(tierData);
    queryCache.invalidateQueries('admin-tiers');
    queryCache.invalidateQueries('customer-membership-tiers');
    await fetchTiers({ forceRefresh: true });
    return created;
  }, [fetchTiers]);

  const updateTier = useCallback(async (tierCode, tierData) => {
    const updated = await scoreAdminService.updateTier(tierCode, tierData);
    queryCache.invalidateQueries('admin-tiers');
    queryCache.invalidateQueries('customer-membership-tiers');
    await fetchTiers({ forceRefresh: true });
    return updated;
  }, [fetchTiers]);

  const adjustScore = useCallback(async (accountId, adjustmentData) => {
    setIsLoadingOperations(true);
    try {
      const res = await scoreAdminService.adjustScore(accountId, adjustmentData);
      queryCache.invalidateQueries(`admin-user-${accountId}`);
      queryCache.invalidateQueries('admin-dashboard');
      queryCache.invalidateQueries('customer-');
      await fetchUserScore(accountId, { forceRefresh: true });
      return res;
    } finally {
      if (isMounted.current) setIsLoadingOperations(false);
    }
  }, [fetchUserScore]);

  const reverseAdjustment = useCallback(async (accountId, reverseData) => {
    setIsLoadingOperations(true);
    try {
      const res = await scoreAdminService.reverseAdjustment(accountId, reverseData);
      queryCache.invalidateQueries(`admin-user-${accountId}`);
      queryCache.invalidateQueries('admin-dashboard');
      queryCache.invalidateQueries('customer-');
      await fetchUserScore(accountId, { forceRefresh: true });
      return res;
    } finally {
      if (isMounted.current) setIsLoadingOperations(false);
    }
  }, [fetchUserScore]);

  const recalculateTier = useCallback(async (accountId) => {
    setIsLoadingOperations(true);
    try {
      const res = await scoreAdminService.recalculateTier(accountId);
      queryCache.invalidateQueries(`admin-user-${accountId}`);
      queryCache.invalidateQueries('customer-');
      await fetchUserScore(accountId, { forceRefresh: true });
      return res;
    } finally {
      if (isMounted.current) setIsLoadingOperations(false);
    }
  }, [fetchUserScore]);

  const updateScoreAccountStatus = useCallback(async (accountId, statusData) => {
    setIsLoadingOperations(true);
    try {
      const res = await scoreAdminService.updateScoreAccountStatus(accountId, statusData);
      queryCache.invalidateQueries(`admin-user-${accountId}`);
      queryCache.invalidateQueries('admin-dashboard');
      queryCache.invalidateQueries('customer-');
      await fetchUserScore(accountId, { forceRefresh: true });
      return res;
    } finally {
      if (isMounted.current) setIsLoadingOperations(false);
    }
  }, [fetchUserScore]);

  const runReconciliation = useCallback(async (reconData = {}) => {
    setIsLoadingOperations(true);
    try {
      const res = await scoreAdminService.runReconciliation(reconData);
      queryCache.invalidateQueries('admin-reconciliation');
      queryCache.invalidateQueries('admin-dashboard');
      return res;
    } finally {
      if (isMounted.current) setIsLoadingOperations(false);
    }
  }, []);

  const fetchReconciliationRuns = useCallback(async (params = {}, options = {}) => {
    setIsLoadingOperations(true);
    try {
      const key = `admin-reconciliation-runs-${JSON.stringify(params)}`;
      const data = await queryCache.fetchQuery(key, ({ signal }) => scoreAdminService.getReconciliationRuns(params, { signal }), { staleTime: 30000, ...options });
      if (isMounted.current) setReconciliationRuns(data);
      return data;
    } finally {
      if (isMounted.current) setIsLoadingOperations(false);
    }
  }, []);

  const fetchReconciliationDetails = useCallback(async (params = {}, options = {}) => {
    setIsLoadingOperations(true);
    try {
      const key = `admin-reconciliation-details-${JSON.stringify(params)}`;
      const data = await queryCache.fetchQuery(key, ({ signal }) => scoreAdminService.getReconciliationDetails(params, { signal }), { staleTime: 30000, ...options });
      if (isMounted.current) setReconciliationDetails(data);
      return data;
    } finally {
      if (isMounted.current) setIsLoadingOperations(false);
    }
  }, []);

  const fetchAuditLogs = useCallback(async (params = {}, options = {}) => {
    setIsLoadingOperations(true);
    try {
      const key = `admin-audit-logs-${JSON.stringify(params)}`;
      const data = await queryCache.fetchQuery(key, ({ signal }) => scoreAdminService.getAuditLogs(params, { signal }), { staleTime: 30000, ...options });
      if (isMounted.current) setAuditLogs(data);
      return data;
    } finally {
      if (isMounted.current) setIsLoadingOperations(false);
    }
  }, []);

  const fetchDashboardStats = useCallback(async (options = {}) => {
    try {
      const data = await queryCache.fetchQuery('admin-dashboard-stats', ({ signal }) => scoreAdminService.getDashboardStats({ signal }), { staleTime: 30000, ...options });
      if (isMounted.current) setDashboardStats(data);
      return data;
    } catch (err) {
      if (isMounted.current && err?.name !== 'AbortError') {
        const msg = err.response?.data?.message || 'Lỗi khi tải thống kê Dashboard';
        setErrorUserScore((prev) => prev || msg);
        console.error('Failed to fetch dashboard stats', err);
      }
      throw err;
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
    updateScoreAccountStatus,
    runReconciliation,
    fetchReconciliationRuns,
    fetchReconciliationDetails,
    fetchAuditLogs,
    fetchDashboardStats,
    exportData
  };
}
