import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data ?? response?.data ?? response;

const flattenWalletItem = item => ({
  ...(item?.promotion || {}),
  ...item,
  promotionPublicId: item?.promotion?.publicId,
  walletPublicId: item?.publicId,
  publicId: item?.publicId,
});

const normalizeWalletPage = response => {
  const page = unwrap(response) || {};
  const content = Array.isArray(page) ? page : (page.content || []);
  return Array.isArray(page)
    ? content.map(flattenWalletItem)
    : { ...page, content: content.map(flattenWalletItem) };
};

const customerPromotionService = {
  getMyPromotions: async (params = {}) =>
    normalizeWalletPage(await apiClient.get('/api/customers/me/promotions', { params })),

  getMyVouchers: async (params = {}) =>
    normalizeWalletPage(await apiClient.get('/api/customers/me/promotions', { params })),

  getPublicPromotions: async (params = {}) =>
    unwrap(await apiClient.get('/api/promotions/public', { params })),

  claimVoucher: async promotionPublicId =>
    flattenWalletItem(unwrap(await apiClient.post(`/api/promotions/${promotionPublicId}/claim`))),

  redeemCoupon: async code =>
    flattenWalletItem(unwrap(await apiClient.post('/api/promotions/coupons/redeem', { code }))),
};

export default customerPromotionService;
