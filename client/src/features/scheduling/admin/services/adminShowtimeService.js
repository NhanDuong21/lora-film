import apiClient from '@/services/apiClient';

const adminShowtimeService = {
  // Currently using customer API as admin API is not available
  // Documented gap: No filtering for non-open showtimes, keyword, or auditorium
  getShowtimes: async (params = {}) => {
    const response = await apiClient.get('/api/showtimes', { params });
    return response.data;
  },

  createShowtime: async (showtimeData) => {
    const response = await apiClient.post('/api/admin/showtimes', showtimeData);
    return response.data;
  },

  updateShowtime: async (showtimePublicId, showtimeData) => {
    const response = await apiClient.put(`/api/admin/showtimes/${showtimePublicId}`, showtimeData);
    return response.data;
  },

  updateShowtimeStatus: async (showtimePublicId, statusData) => {
    const response = await apiClient.put(`/api/admin/showtimes/${showtimePublicId}/status`, statusData);
    return response.data;
  },

  getStatusHistory: async (showtimePublicId) => {
    const response = await apiClient.get(`/api/admin/showtimes/${showtimePublicId}/status-history`);
    return response.data;
  }
};

export default adminShowtimeService;
