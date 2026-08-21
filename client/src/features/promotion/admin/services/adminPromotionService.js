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
  transitionCampaign: async (id, action, comment, expectedVersion) => unwrap(await apiClient.patch(
    `/api/admin/promotion-campaigns/${id}/status`, null, { params: cleanParams({ action, comment, expectedVersion }) }
  )),
  approveCampaign: async (id, comment) => unwrap(await apiClient.post(
    `/api/admin/promotion-campaigns/${id}/approve`, { comment }
  )),
  rejectCampaign: async (id, comment) => unwrap(await apiClient.post(
    `/api/admin/promotion-campaigns/${id}/reject`, { comment }
  )),
  getApprovalHistory: async id => unwrap(await apiClient.get(
    `/api/admin/promotion-campaigns/${id}/approval-history`
  )),
  overrideCampaignApproval: async (id, campaignCode, incidentReference, reason) => unwrap(await apiClient.post(
    `/api/admin/promotion-campaigns/${id}/override-approval`, { campaignCode, incidentReference, reason }
  )),
  getForceReleaseImpact: async id => unwrap(await apiClient.get(
    `/api/admin/promotion-campaigns/${id}/force-release-impact`
  )),
  forceReleaseCampaignHolds: async (id, campaignCode, reason, impact, idempotencyKey) => unwrap(await apiClient.post(
    `/api/admin/promotion-campaigns/${id}/force-release`, {
      campaignCode,
      reason,
      impactToken: impact?.impactToken,
      campaignVersion: impact?.campaignVersion,
    }, { headers: { 'Idempotency-Key': idempotencyKey } }
  )),
  reviewCampaignLegal: async (id, status, comment, legalNotificationRef) => unwrap(await apiClient.post(
    `/api/admin/promotion-campaigns/${id}/legal-review`, { status, comment, legalNotificationRef }
  )),

  getPromotionOpportunities: async () => unwrap(await apiClient.get(
    '/api/admin/promotion-opportunities'
  )),
  getPromotionPlaybooks: async () => unwrap(await apiClient.get(
    '/api/admin/promotion-playbooks'
  )),
  updatePromotionPlaybook: async (id, payload) => unwrap(await apiClient.put(
    `/api/admin/promotion-playbooks/${id}`, payload
  )),
  submitPromotionPlaybook: async id => unwrap(await apiClient.post(
    `/api/admin/promotion-playbooks/${id}/submit`
  )),
  approvePromotionPlaybook: async id => unwrap(await apiClient.post(
    `/api/admin/promotion-playbooks/${id}/approve`
  )),
  pausePromotionPlaybook: async id => unwrap(await apiClient.post(
    `/api/admin/promotion-playbooks/${id}/pause`
  )),
  runPromotionPlaybook: async id => unwrap(await apiClient.post(
    `/api/admin/promotion-playbooks/${id}/run`
  )),
  getPromotionRuns: async () => unwrap(await apiClient.get(
    '/api/admin/promotion-runs'
  )),
  getPromotionRun: async id => unwrap(await apiClient.get(
    `/api/admin/promotion-runs/${id}`
  )),
  createPromotionIssueJob: async (id, batchSize = 200) => unwrap(await apiClient.post(
    `/api/admin/promotion-runs/${id}/issue-jobs`, { batchSize }
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
  searchPromotionOperations: async (params = {}) =>
    unwrap(await apiClient.get('/api/admin/promotion-operations/search', { params: cleanParams(params) })),
  getBookingMonitoring: async () =>
    unwrap(await apiClient.get('/api/admin/monitoring/summary')),
};

export default adminPromotionService;
