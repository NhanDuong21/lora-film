import apiClient from '@/services/apiClient';

const ADMIN_SCORE_API_URL = '/api/admin/scores/users';
const ADMIN_TIER_API_URL = '/api/admin/membership-tiers';

const scoreAdminService = {
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
  }
};

export default scoreAdminService;
