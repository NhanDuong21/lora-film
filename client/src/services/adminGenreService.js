import apiClient from './apiClient';

const API_URL = '/api/admin/genres';

const adminGenreService = {
  getAllGenres: async () => {
    const response = await apiClient.get(API_URL);
    return response.data;
  },

  getGenreById: async (id) => {
    const response = await apiClient.get(`${API_URL}/${id}`);
    return response.data;
  },

  createGenre: async (genreData) => {
    const response = await apiClient.post(API_URL, genreData);
    return response.data;
  },

  updateGenre: async (id, genreData) => {
    const response = await apiClient.put(`${API_URL}/${id}`, genreData);
    return response.data;
  },

  deleteGenre: async (id) => {
    const response = await apiClient.delete(`${API_URL}/${id}`);
    return response.data;
  }
};

export default adminGenreService;
