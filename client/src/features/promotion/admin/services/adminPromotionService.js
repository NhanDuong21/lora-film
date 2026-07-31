import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data ?? response?.data ?? response;

const cleanParams = params => Object.fromEntries(
  Object.entries(params || {}).filter(([, value]) =>
    value !== undefined && value !== null && value !== ''
  )
);

const adminPromotionService = {
  searchCampaigns: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/promotion-campaigns', {
      params: cleanParams(params),
    })),

  getCampaign: async id =>
    unwrap(await apiClient.get(`/api/admin/promotion-campaigns/${id}`)),

  createCampaign: async payload =>
    unwrap(await apiClient.post('/api/admin/promotion-campaigns', payload)),

  updateCampaign: async (id, payload) =>
    unwrap(await apiClient.put(`/api/admin/promotion-campaigns/${id}`, payload)),

  deleteCampaign: async id =>
    unwrap(await apiClient.delete(`/api/admin/promotion-campaigns/${id}`)),

  transitionCampaign: async (id, action, comment) =>
    unwrap(await apiClient.patch(`/api/admin/promotion-campaigns/${id}/status`, null, {
      params: cleanParams({ action, comment }),
    })),

  approveCampaign: async (id, comment) =>
    unwrap(await apiClient.post(`/api/admin/promotion-campaigns/${id}/approve`, { comment })),

  rejectCampaign: async (id, comment) =>
    unwrap(await apiClient.post(`/api/admin/promotion-campaigns/${id}/reject`, { comment })),

  reviewCampaignLegal: async (id, payload) =>
    unwrap(await apiClient.post(`/api/admin/promotion-campaigns/${id}/legal-review`, payload)),

  getApprovalHistory: async id =>
    unwrap(await apiClient.get(`/api/admin/promotion-campaigns/${id}/approval-history`)),

  searchRules: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/promotion-rules', {
      params: cleanParams(params),
    })),

  getRule: async id =>
    unwrap(await apiClient.get(`/api/admin/promotion-rules/${id}`)),

  createRule: async payload =>
    unwrap(await apiClient.post('/api/admin/promotion-rules', payload)),

  updateRule: async (id, payload) =>
    unwrap(await apiClient.put(`/api/admin/promotion-rules/${id}`, payload)),

  deleteRule: async id =>
    unwrap(await apiClient.delete(`/api/admin/promotion-rules/${id}`)),

  cloneRule: async (id, payload) =>
    unwrap(await apiClient.post(`/api/admin/promotion-rules/${id}/clone`, payload)),

  previewRule: async payload =>
    unwrap(await apiClient.post('/api/admin/promotion-rules/preview', payload)),

  searchCoupons: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/coupons', { params: cleanParams(params) })),

  getCoupon: async id =>
    unwrap(await apiClient.get(`/api/admin/coupons/${id}`)),

  createCoupon: async payload =>
    unwrap(await apiClient.post('/api/admin/coupons', payload)),

  generateCoupons: async payload =>
    unwrap(await apiClient.post('/api/admin/coupons/generate', payload)),

  updateCoupon: async (id, payload) =>
    unwrap(await apiClient.put(`/api/admin/coupons/${id}`, payload)),

  disableCoupon: async id =>
    unwrap(await apiClient.delete(`/api/admin/coupons/${id}`)),

  importCoupons: async file => {
    const formData = new FormData();
    formData.append('file', file);
    return unwrap(await apiClient.post('/api/admin/coupons/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }));
  },

  exportCoupons: async (params = {}) =>
    (await apiClient.get('/api/admin/coupons/export', {
      params: cleanParams(params),
      responseType: 'blob',
    })).data,

  searchVouchers: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/vouchers', { params: cleanParams(params) })),

  getVoucher: async id =>
    unwrap(await apiClient.get(`/api/admin/vouchers/${id}`)),

  issueVoucher: async payload =>
    unwrap(await apiClient.post('/api/admin/vouchers', payload)),

  batchIssueVouchers: async vouchers =>
    unwrap(await apiClient.post('/api/admin/vouchers/batch', { vouchers })),

  updateVoucher: async (id, payload) =>
    unwrap(await apiClient.put(`/api/admin/vouchers/${id}`, payload)),

  revokeVoucher: async (id, reason) =>
    unwrap(await apiClient.post(`/api/admin/vouchers/${id}/revoke`, null, {
      params: cleanParams({ reason }),
    })),

  extendVoucher: async (id, payload) =>
    unwrap(await apiClient.post(`/api/admin/vouchers/${id}/extend`, payload)),

  searchCompensations: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/compensation-vouchers', {
      params: cleanParams(params),
    })),

  getCompensation: async id =>
    unwrap(await apiClient.get(`/api/admin/compensation-vouchers/${id}`)),

  issueCompensation: async payload =>
    unwrap(await apiClient.post('/api/admin/compensation-vouchers', payload)),

  updateCompensation: async (id, payload) =>
    unwrap(await apiClient.put(`/api/admin/compensation-vouchers/${id}`, payload)),

  searchRedemptions: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/redemptions', { params: cleanParams(params) })),

  searchReservations: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/reservations', { params: cleanParams(params) })),
};

export default adminPromotionService;
