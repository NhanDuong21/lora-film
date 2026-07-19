import apiClient from '@/services/apiClient';

const adminShowtimeService = {
  getShowtimes: async (params) => {
    // using the public read endpoint for now since there's no admin list API in the requirement
    const response = await apiClient.get('/api/showtimes', { params });
    return response.data;
  },

  getShowtimeDetail: async (showtimePublicId) => {
    const response = await apiClient.get(`/api/showtimes/${showtimePublicId}`);
    return response.data;
  },

  createShowtime: async (requestData) => {
    const response = await apiClient.post('/api/admin/showtimes', requestData);
    return response.data;
  },
  
  updateShowtime: async (showtimePublicId, requestData) => {
    const response = await apiClient.put(`/api/admin/showtimes/${showtimePublicId}`, requestData);
    return response.data;
  },

  transitionStatus: async (showtimePublicId, requestData) => {
    const response = await apiClient.put(`/api/admin/showtimes/${showtimePublicId}/status`, requestData);
    return response.data;
  },

  getStatusHistory: async (showtimePublicId) => {
    const response = await apiClient.get(`/api/admin/showtimes/${showtimePublicId}/status-history`);
    return response.data;
  },

  getPrices: async (showtimePublicId) => {
    const response = await apiClient.get(`/api/admin/showtimes/${showtimePublicId}/prices`);
    return response.data;
  },

  updatePrices: async (showtimePublicId, requestData) => {
    const response = await apiClient.put(`/api/admin/showtimes/${showtimePublicId}/prices`, requestData);
    return response.data;
  }
};

export default adminShowtimeService;
