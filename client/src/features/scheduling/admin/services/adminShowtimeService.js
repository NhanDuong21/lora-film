import apiClient from '@/services/apiClient';

const adminShowtimeService = {
  getShowtimes: async (params) => {
    const response = await apiClient.get('/api/admin/showtimes', { params });
    return response.data;
  },

  getShowtimeDetail: async (showtimePublicId) => {
    const response = await apiClient.get(`/api/admin/showtimes/${showtimePublicId}`);
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
  },

  transitionBatchStatus: async (batchId, requestData) => {
    const response = await apiClient.put(`/api/admin/showtimes/batch/${batchId}/status`, requestData);
    return response.data;
  },

  getPricing: async (showtimePublicId) => {
    const response = await apiClient.get(`/api/admin/showtimes/${showtimePublicId}/pricing`);
    return response.data;
  },

  resolvePricing: async (showtimePublicId, expectedShowtimeVersion) => {
    const response = await apiClient.post(`/api/admin/showtimes/${showtimePublicId}/pricing/resolve`, {
      expectedShowtimeVersion,
    });
    return response.data;
  },

  previewBatchStatus: async (batchId, targetStatus) => {
    const response = await apiClient.get(`/api/admin/showtimes/batch/${batchId}/status-preview`, {
      params: { targetStatus },
    });
    return response.data;
  },

  getSeatControl: async (showtimePublicId) => {
    const response = await apiClient.get(`/api/admin/showtimes/${showtimePublicId}/seat-control`);
    return response.data?.data;
  },

  blockSeats: async (showtimePublicId, seatPublicIds, reason) => {
    const response = await apiClient.post(`/api/admin/showtimes/${showtimePublicId}/blocked-seats`, {
      seatPublicIds,
      reason,
    });
    return response.data?.data;
  },

  releaseBlockedSeats: async (showtimePublicId, seatPublicIds, reason) => {
    const response = await apiClient.put(`/api/admin/showtimes/${showtimePublicId}/blocked-seats/release`, {
      seatPublicIds,
      reason,
    });
    return response.data?.data;
  },
};

export default adminShowtimeService;
