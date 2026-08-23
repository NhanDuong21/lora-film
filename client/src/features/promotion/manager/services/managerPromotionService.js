import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data ?? response?.data ?? response;
const paramsFor = cinemaPublicId => ({ cinemaPublicId });

const managerPromotionService = {
  getWorkspace: async cinemaPublicId => unwrap(await apiClient.get(
    '/api/manager/promotions/workspace', { params: paramsFor(cinemaPublicId) },
  )),

  getCampaigns: async cinemaPublicId => unwrap(await apiClient.get(
    '/api/manager/promotions/campaigns', { params: paramsFor(cinemaPublicId) },
  )) || [],

  getAutomations: async cinemaPublicId => unwrap(await apiClient.get(
    '/api/manager/promotions/automations', { params: paramsFor(cinemaPublicId) },
  )) || [],

  getDistributionOptions: async cinemaPublicId => unwrap(await apiClient.get(
    '/api/manager/promotions/distribution-options', { params: paramsFor(cinemaPublicId) },
  )) || [],

  getIncidents: async cinemaPublicId => unwrap(await apiClient.get(
    '/api/manager/promotions/incidents', { params: paramsFor(cinemaPublicId) },
  )) || [],

  issueBenefit: async (cinemaPublicId, promotionPublicId, userPublicIds) => unwrap(
    await apiClient.post(
      `/api/manager/promotions/distribution-options/${promotionPublicId}/issue`,
      { userPublicIds },
      { params: paramsFor(cinemaPublicId) },
    ),
  ),
};

export default managerPromotionService;
