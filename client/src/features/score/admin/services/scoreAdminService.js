import apiClient from '@/services/apiClient';

const ADMIN_SCORE_API_URL = '/api/v1/admin/scores';

const scoreAdminService = {
  /**
   * Get user score details by accountId (Lazy Initializes if not found in DB)
   */
  getScoreByAccount: async (accountId) => {
    const response = await apiClient.get(`${ADMIN_SCORE_API_URL}/users/${accountId}`);
    return response.data?.data || response.data;
  },

  /**
   * Get paginated score history for a user by accountId
   */
  getScoreHistoryByAccount: async (accountId, params = {}) => {
    const response = await apiClient.get(`${ADMIN_SCORE_API_URL}/users/${accountId}/history`, {
      params: {
        page: params.page || 0,
        size: params.size || 10,
        ...params
      }
    });
    return response.data?.data || response.data;
  },

  /**
   * Get all membership tiers
   */
  getAllTiers: async () => {
    const response = await apiClient.get(`${ADMIN_SCORE_API_URL}/tiers`);
    return response.data?.data || response.data;
  },

  /**
   * Get membership tier by code
   */
  getTierByCode: async (tierCode) => {
    const response = await apiClient.get(`${ADMIN_SCORE_API_URL}/tiers/${tierCode}`);
    return response.data?.data || response.data;
  },

  /**
   * Create a new membership tier
   */
  createTier: async (tierData) => {
    const response = await apiClient.post(`${ADMIN_SCORE_API_URL}/tiers`, tierData);
    return response.data?.data || response.data;
  },

  /**
   * Update an existing membership tier
   */
  updateTier: async (tierCode, tierData) => {
    const response = await apiClient.put(`${ADMIN_SCORE_API_URL}/tiers/${tierCode}`, tierData);
    return response.data?.data || response.data;
  }
};

export default scoreAdminService;
