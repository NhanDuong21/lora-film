import apiClient from '@/services/apiClient';

const SCORE_API_URL = '/api/scores/me';
const TIERS_API_URL = '/api/membership-tiers';

const scoreCustomerService = {
  getScoreBalance: async () => {
    const response = await apiClient.get(SCORE_API_URL);
    return response.data;
  },

  getScoreTier: async () => {
    const response = await apiClient.get(`${SCORE_API_URL}/tier`);
    return response.data;
  },

  getScoreHistory: async (params = {}) => {
    const response = await apiClient.get(`${SCORE_API_URL}/history`, { params });
    return response.data;
  },

  getMembershipTiers: async () => {
    const response = await apiClient.get(TIERS_API_URL);
    return response.data;
  },

  redeemPreview: async (data) => {
    const response = await apiClient.post(`${SCORE_API_URL}/redeem-preview`, data);
    return response.data;
  }
};

export default scoreCustomerService;
