import apiClient from '@/services/apiClient';

const adminAutoScheduleService = {
  generatePreview: async (requestData) => {
    const response = await apiClient.post('/api/admin/showtime-schedules/generate-preview', requestData);
    return response.data;
  },

  getPreview: async (previewPublicId, params, config = {}) => {
    const response = await apiClient.get(`/api/admin/showtime-schedules/${previewPublicId}`, {
      ...config,
      params,
    });
    return response.data;
  },

  updateSelections: async (previewPublicId, data) => {
    const response = await apiClient.put(`/api/admin/showtime-schedules/${previewPublicId}/items`, data);
    return response.data;
  },

  applyPreview: async (previewPublicId, data) => {
    const response = await apiClient.post(`/api/admin/showtime-schedules/${previewPublicId}/apply`, data);
    return response.data;
  },

  getPreviewHistory: async (params) => {
    const response = await apiClient.get('/api/admin/showtime-schedules', { params });
    return response.data;
  },

  getEligibleMovies: async (params) => {
    const response = await apiClient.get('/api/admin/showtime-schedules/eligible-movies', { params });
    return response.data;
  }
};

export default adminAutoScheduleService;
