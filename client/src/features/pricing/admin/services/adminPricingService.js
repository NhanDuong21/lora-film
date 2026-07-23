import apiClient from '@/services/apiClient';

const adminPricingService = {
  searchPolicies: async (params = {}) => {
    const response = await apiClient.get('/api/admin/pricing/policies', { params });
    return response.data;
  },
  createPolicy: async (data) => {
    const response = await apiClient.post('/api/admin/pricing/policies', data);
    return response.data;
  },
  getPolicy: async (id) => {
    const response = await apiClient.get(`/api/admin/pricing/policies/${id}`);
    return response.data;
  },
  updatePolicy: async (id, data) => {
    const response = await apiClient.put(`/api/admin/pricing/policies/${id}`, data);
    return response.data;
  },
  activatePolicy: async (id, expectedVersion) => {
    const response = await apiClient.post(`/api/admin/pricing/policies/${id}/activate`, { expectedVersion });
    return response.data;
  },
  deactivatePolicy: async (id, expectedVersion, reason) => {
    const response = await apiClient.post(`/api/admin/pricing/policies/${id}/deactivate`, {
      expectedVersion,
      reason,
    });
    return response.data;
  },
  copyPolicy: async (id, expectedVersion, name) => {
    const response = await apiClient.post(`/api/admin/pricing/policies/${id}/copy`, {
      expectedVersion,
      name,
    });
    return response.data;
  },
  getUsage: async (id, params = {}) => {
    const response = await apiClient.get(`/api/admin/pricing/policies/${id}/usage`, { params });
    return response.data;
  },
  previewResolution: async (data) => {
    const response = await apiClient.post('/api/admin/pricing/resolve-preview', data);
    return response.data;
  },
};

export default adminPricingService;
