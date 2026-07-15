import apiClient from '@/services/apiClient';

const adminCinemaService = {
  // Fetch paged cinemas list with keyword, status, city, etc.
  getCinemas: async (params = {}) => {
    const response = await apiClient.get('/api/admin/cinemas', { params });
    return response.data;
  },

  // Create a new cinema (which defaults to DRAFT status in backend)
  createCinema: async (cinemaData) => {
    const response = await apiClient.post('/api/admin/cinemas', cinemaData);
    return response.data;
  },

  // Update a cinema's status
  updateCinemaStatus: async (publicId, status) => {
    const response = await apiClient.put(`/api/admin/cinemas/${publicId}/status`, { status });
    return response.data;
  },

  // Update a cinema's operating hours
  updateOperatingHours: async (publicId, operatingHours) => {
    const response = await apiClient.put(`/api/admin/cinemas/${publicId}/operating-hours`, operatingHours);
    return response.data;
  },

  // Soft delete a cinema
  deleteCinema: async (publicId) => {
    const response = await apiClient.delete(`/api/admin/cinemas/${publicId}`);
    return response.data;
  },

  // Geocoding Suggestions
  suggestAddress: async (keyword) => {
    const response = await apiClient.get('/api/v1/address/suggest', { params: { q: keyword } });
    return response.data;
  },

  // Forward Geocode
  forwardGeocode: async (address) => {
    const response = await apiClient.post('/api/v1/geocode', { address });
    return response.data;
  },

  // Reverse Geocode
  reverseGeocode: async (lat, lon) => {
    const response = await apiClient.get('/api/v1/geocode/reverse', { params: { lat, lon } });
    return response.data;
  },

  // Save Cinema Media
  createCinemaMedia: async (cinemaPublicId, mediaData) => {
    const response = await apiClient.post(`/api/admin/cinemas/${cinemaPublicId}/media`, mediaData);
    return response.data;
  },

  // Get admin cinema detail
  getAdminCinemaDetail: async (publicId) => {
    const response = await apiClient.get(`/api/admin/cinemas/${publicId}`);
    return response.data;
  },

  // Update cinema details
  updateCinema: async (publicId, cinemaData) => {
    const response = await apiClient.put(`/api/admin/cinemas/${publicId}`, cinemaData);
    return response.data;
  },

  // Get closure periods
  getClosurePeriods: async (publicId, params = {}) => {
    const response = await apiClient.get(`/api/admin/cinemas/${publicId}/closure-periods`, { params });
    return response.data;
  },

  // Create closure period
  createClosurePeriod: async (publicId, closureData) => {
    const response = await apiClient.post(`/api/admin/cinemas/${publicId}/closure-periods`, closureData);
    return response.data;
  },

  // Cancel closure period
  cancelClosurePeriod: async (closurePeriodId) => {
    const response = await apiClient.put(`/api/admin/closure-periods/${closurePeriodId}/cancel`);
    return response.data;
  }
};

export default adminCinemaService;
