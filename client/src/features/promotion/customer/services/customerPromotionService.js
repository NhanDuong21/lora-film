import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data ?? response?.data ?? response;

const customerPromotionService = {
  getMyVouchers: async (params = {}) =>
    unwrap(await apiClient.get('/api/customers/me/vouchers', { params })),
};

export default customerPromotionService;
