import apiClient from '@/services/apiClient';

const ADMIN_SCORE_API_URL = '/api/admin/scores/users';
const ADMIN_TIER_API_URL = '/api/admin/membership-tiers';

const asNumber = value => {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
};

export const normalizeScoreDashboardStats = (raw = {}) => ({
  totalMembers: asNumber(raw.totalMembers),
  activeMembers: asNumber(raw.activeMembers),
  lockedMembers: asNumber(raw.lockedMembers),
  totalAvailablePoints: asNumber(raw.totalAvailablePoints),
  totalAccumulatedPoints: asNumber(raw.totalAccumulatedPoints),
  totalOutstandingPoints: asNumber(raw.totalOutstandingPoints),
  totalPointsEarned: asNumber(raw.totalPointsEarned),
  totalPointsRedeemed: asNumber(raw.totalPointsRedeemed),
  totalPointsHeld: asNumber(raw.totalPointsHeld),
  totalPointsExpired: asNumber(raw.totalPointsExpired),
  silverMembers: asNumber(raw.silverMembers),
  goldMembers: asNumber(raw.goldMembers),
  diamondMembers: asNumber(raw.diamondMembers),
  pendingReconciliationMismatches: asNumber(
    raw.pendingReconciliationMismatches ?? raw.pendingReconciliations,
  ),
  lastReconciliationBatch: raw.lastReconciliationBatch || 'N/A',
  lastReconciliationTime:
    raw.lastReconciliationTime ?? raw.lastReconciliationDate ?? null,
  lastReconciliationFinishedAt: raw.lastReconciliationFinishedAt ?? null,
  lastReconciliationStatus: raw.lastReconciliationStatus ?? null,
  lastReconciliationTotalUsers: asNumber(raw.lastReconciliationTotalUsers),
  lastReconciliationMatchedUsers: asNumber(raw.lastReconciliationMatchedUsers),
  lastReconciliationMismatchedUsers: asNumber(raw.lastReconciliationMismatchedUsers),
});

const scoreAdminService = {
  getScoreAccounts: async (params = {}) => {
    const response = await apiClient.get(ADMIN_SCORE_API_URL, { params });
    return response.data?.data || response.data;
  },

  /**
   * Get user score details by accountId (Lazy Initializes if not found in DB)
   */
  getScoreByAccount: async (accountId) => {
    const response = await apiClient.get(`${ADMIN_SCORE_API_URL}/${accountId}`);
    return response.data?.data || response.data;
  },

  /**
   * Get paginated score history for a user by accountId
   */
  getScoreHistoryByAccount: async (accountId, params = {}) => {
    const response = await apiClient.get(`${ADMIN_SCORE_API_URL}/${accountId}/history`, {
      params: {
        page: params.page || 0,
        size: params.size || 10,
        ...params
      }
    });
    return response.data?.data || response.data;
  },

  /**
   * Get user expiring point buckets by accountId
   */
  getUserExpiringPoints: async (accountId) => {
    const response = await apiClient.get(`${ADMIN_SCORE_API_URL}/${accountId}/expiring`);
    return response.data?.data || response.data;
  },

  /**
   * Get user membership tier history by accountId
   */
  getUserTierHistory: async (accountId) => {
    const response = await apiClient.get(`${ADMIN_SCORE_API_URL}/${accountId}/tier-history`);
    return response.data?.data || response.data;
  },

  /**
   * Get all membership tiers
   */
  getAllTiers: async () => {
    const response = await apiClient.get(ADMIN_TIER_API_URL);
    return response.data?.data || response.data;
  },


  /**
   * Get membership tier by code or id
   */
  getTierByCode: async (tierCode) => {
    const response = await apiClient.get(`${ADMIN_TIER_API_URL}/${tierCode}`);
    return response.data?.data || response.data;
  },

  /**
   * Create a new membership tier
   */
  createTier: async (tierData) => {
    const response = await apiClient.post(ADMIN_TIER_API_URL, tierData);
    return response.data?.data || response.data;
  },

  /**
   * Update an existing membership tier
   */
  updateTier: async (tierCode, tierData) => {
    const response = await apiClient.put(`${ADMIN_TIER_API_URL}/${tierCode}`, tierData);
    return response.data?.data || response.data;
  },

  /**
   * Adjust user score manually (add/deduct points)
   */
  adjustScore: async (accountId, adjustmentData) => {
    const response = await apiClient.post(`${ADMIN_SCORE_API_URL}/${accountId}/adjustments`, adjustmentData);
    return response.data?.data || response.data;
  },

  /**
   * Reverse a previous score adjustment
   */
  reverseAdjustment: async (accountId, reverseData) => {
    const response = await apiClient.post(`${ADMIN_SCORE_API_URL}/${accountId}/adjustments/reverse`, reverseData);
    return response.data?.data || response.data;
  },

  /**
   * Recalculate user membership tier
   */
  recalculateTier: async (accountId) => {
    const response = await apiClient.post(`${ADMIN_SCORE_API_URL}/${accountId}/recalculate-tier`);
    return response.data?.data || response.data;
  },

  updateScoreAccountStatus: async (accountId, statusData) => {
    const response = await apiClient.post(`${ADMIN_SCORE_API_URL}/${accountId}/status`, statusData);
    return response.data?.data || response.data;
  },

  /**
   * Trigger reconciliation job
   */
  runReconciliation: async (reconData = {}) => {
    const response = await apiClient.post('/api/admin/scores/reconciliation', reconData);
    return response.data?.data || response.data;
  },

  /**
   * Get reconciliation run batches with pagination and filters
   */
  getReconciliationRuns: async (params = {}) => {
    const response = await apiClient.get('/api/admin/scores/reconciliation/runs', { params });
    return response.data?.data || response.data;
  },

  /**
   * Get reconciliation details (discrepancies) for a batch
   */
  getReconciliationDetails: async (params = {}) => {
    const response = await apiClient.get('/api/admin/scores/reconciliation/details', { params });
    return response.data?.data || response.data;
  },

  /**
   * Get admin audit logs with pagination and filters
   */
  getAuditLogs: async (params = {}) => {
    const response = await apiClient.get('/api/admin/scores/audit', { params });
    return response.data?.data || response.data;
  },

  /**
   * Export score history, reconciliation, or audit logs as CSV blob
   */
  exportData: async (params = {}) => {
    const response = await apiClient.get('/api/admin/scores/export', {
      params,
      responseType: 'blob'
    });
    return response.data;
  },

  /**
   * Get KPI stats for loyalty admin dashboard
   */
  getDashboardStats: async () => {
    const response = await apiClient.get('/api/admin/scores/dashboard');
    return normalizeScoreDashboardStats(response.data?.data || response.data);
  }
};

export default scoreAdminService;
