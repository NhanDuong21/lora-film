import apiClient from '@/services/apiClient';

const BASE_URL = '/api/admin/tmdb';

const adminTmdbService = {
  getSyncState: async () => {
    const response = await apiClient.get(`${BASE_URL}/sync/state`);
    return response.data.data;
  },
  
  syncMovieById: async (tmdbId) => {
    const response = await apiClient.post(`${BASE_URL}/sync/${tmdbId}`);
    return response.data;
  },
  
  startBulkSync: async () => {
    const response = await apiClient.post(`${BASE_URL}/sync/bulk/start`);
    return response.data;
  },
  
  resetBulkSync: async () => {
    const response = await apiClient.post(`${BASE_URL}/sync/bulk/reset`);
    return response.data;
  }
};

export default adminTmdbService;
