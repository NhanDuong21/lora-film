import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data ?? response?.data ?? response;
const cleanParams = params => Object.fromEntries(
  Object.entries(params || {}).filter(([, value]) => value !== undefined && value !== null && value !== '')
);

const adminPromotionService = {
  searchCampaigns: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/promotion-campaigns', { params: cleanParams(params) })),
  getCampaign: async id => unwrap(await apiClient.get(`/api/admin/promotion-campaigns/${id}`)),
  createCampaign: async payload => unwrap(await apiClient.post('/api/admin/promotion-campaigns', payload)),
  updateCampaign: async (id, payload) => unwrap(await apiClient.put(`/api/admin/promotion-campaigns/${id}`, payload)),
  deleteCampaign: async id => unwrap(await apiClient.delete(`/api/admin/promotion-campaigns/${id}`)),
  transitionCampaign: async (id, action, comment) => unwrap(await apiClient.patch(
    `/api/admin/promotion-campaigns/${id}/status`, null, { params: cleanParams({ action, comment }) }
  )),
  approveCampaign: async (id, comment) => unwrap(await apiClient.post(
    `/api/admin/promotion-campaigns/${id}/approve`, { comment }
  )),
  reviewCampaignLegal: async (id, status, comment, legalNotificationRef = null) => unwrap(await apiClient.post(
    `/api/admin/promotion-campaigns/${id}/legal-review`, { status, comment, legalNotificationRef }
  )),

  searchPromotions: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/promotions', { params: cleanParams(params) })),
  getPromotion: async id => unwrap(await apiClient.get(`/api/admin/promotions/${id}`)),
  createPromotion: async payload => unwrap(await apiClient.post('/api/admin/promotions', payload)),
  updatePromotion: async (id, payload) => unwrap(await apiClient.put(`/api/admin/promotions/${id}`, payload)),
  deletePromotion: async id => unwrap(await apiClient.delete(`/api/admin/promotions/${id}`)),
  activatePromotion: async id => unwrap(await apiClient.post(`/api/admin/promotions/${id}/activate`)),
  pausePromotion: async id => unwrap(await apiClient.post(`/api/admin/promotions/${id}/pause`)),
  getCloneDraft: async id =>
    unwrap(await apiClient.get(`/api/admin/promotions/${id}/clone-draft`)),
  issuePromotion: async (id, userPublicIds) => unwrap(await apiClient.post(
    `/api/admin/promotions/${id}/issue`, { userPublicIds }
  )),

  searchReservations: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/reservations', { params: cleanParams(params) })),
  getPromotionMonitoring: async () =>
    unwrap(await apiClient.get('/api/admin/promotion-monitoring/summary')),
  getBookingMonitoring: async () =>
    unwrap(await apiClient.get('/api/admin/monitoring/summary')),
};

export default adminPromotionService;
