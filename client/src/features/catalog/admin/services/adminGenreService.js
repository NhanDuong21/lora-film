import apiClient from '@/services/apiClient';

const API_URL = '/api/admin/genres';
const PAGE_SIZE = 100;

const adminGenreService = {
  getAllGenres: async () => {
    const firstResponse = await apiClient.get(API_URL, {
      params: { page: 1, size: PAGE_SIZE },
    });
    const firstEnvelope = firstResponse.data;
    const firstPage = firstEnvelope?.data;
    const totalPages = Number(firstPage?.totalPages || 0);

    if (!Array.isArray(firstPage?.content) || totalPages <= 1) {
      return firstEnvelope;
    }

    const remainingResponses = await Promise.all(
      Array.from({ length: totalPages - 1 }, (_, index) => (
        apiClient.get(API_URL, {
          params: { page: index + 2, size: PAGE_SIZE },
        })
      )),
    );
    const content = [
      ...firstPage.content,
      ...remainingResponses.flatMap(response => (
        Array.isArray(response.data?.data?.content) ? response.data.data.content : []
      )),
    ];

    return {
      ...firstEnvelope,
      data: {
        ...firstPage,
        content,
        pageNumber: 0,
        pageSize: content.length,
        totalElements: Number(firstPage.totalElements || content.length),
        totalPages: 1,
        last: true,
      },
    };
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
