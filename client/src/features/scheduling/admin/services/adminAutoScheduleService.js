import apiClient from '@/services/apiClient';

const adminAutoScheduleService = {
  generatePreview: async (requestData) => {
    const response = await apiClient.post('/api/admin/showtime-schedules/generate-preview', requestData);
    return response.data;
  },

  getPreview: async (previewPublicId) => {
    const response = await apiClient.get(`/api/admin/showtime-schedules/previews/${previewPublicId}`);
    return response.data;
  },

  applyPreview: async (previewPublicId) => {
    const response = await apiClient.post(`/api/admin/showtime-schedules/previews/${previewPublicId}/apply`);
    return response.data;
  },

  getPreviewHistory: async (params) => {
    const response = await apiClient.get('/api/admin/showtime-schedules/previews', { params });
    return response.data;
  }
};

export default adminAutoScheduleService;
