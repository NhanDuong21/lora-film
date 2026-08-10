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
  
  startBulkSync: async (request) => {
    const response = await apiClient.post(`${BASE_URL}/sync/bulk/start`, request);
    return response.data;
  },

  searchMovies: async (query, signal) => {
    const response = await apiClient.get(`${BASE_URL}/movies/search`, {
      params: { query, limit: 8 },
      signal,
    });
    return response.data.data || [];
  },
  
  resetBulkSync: async (request) => {
    const response = await apiClient.post(`${BASE_URL}/sync/bulk/reset`, request);
    return response.data;
  },

  stopBulkSync: async () => {
    const response = await apiClient.post(`${BASE_URL}/sync/bulk/stop`);
    return response.data;
  }
};

export default adminTmdbService;
