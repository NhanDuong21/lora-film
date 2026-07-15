import apiClient from './apiClient';

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
  }
};

export default adminCinemaService;
