import apiClient from '@/services/apiClient';

const adminMovieService = {
  // ─── Movie CRUD ────────────────────────────────────────────────────────────
  getMovies: async (params) => {
    const resolvedParams = { ...params };
    if (resolvedParams.search !== undefined) {
      resolvedParams.keyword = resolvedParams.search;
      delete resolvedParams.search;
    }
    const response = await apiClient.get('/api/admin/movies', { params: resolvedParams });
    return response.data;
  },

  getMovieById: async (movieId) => {
    const response = await apiClient.get(`/api/admin/movies/${movieId}`);
    return response.data;
  },

  createMovie: async (movieData) => {
    const response = await apiClient.post('/api/admin/movies', movieData);
    return response.data;
  },

  updateMovie: async (publicId, movieData) => {
    const response = await apiClient.put(`/api/admin/movies/${publicId}`, movieData);
    return response.data;
  },

  deleteMovie: async (publicId) => {
    const response = await apiClient.delete(`/api/admin/movies/${publicId}`);
    return response.data;
  },

  assignGenres: async (publicId, genreIds) => {
    const response = await apiClient.put(`/api/admin/movies/${publicId}/genres`, { genreIds });
    return response.data;
  },

  updateMovieStatus: async (publicId, status) => {
    const response = await apiClient.put(`/api/admin/movies/${publicId}/status`, null, {
      params: { status }
    });
    return response.data;
  },

  // ─── Genre auto-create ─────────────────────────────────────────────────────
  ensureGenreExists: async (genreName) => {
    try {
      const response = await apiClient.post('/api/admin/genres', { name: genreName, status: 'ACTIVE' });
      return response.data?.data;
    } catch (err) {
      // If 409 (duplicate), treat as success — genre already exists
      if (err?.response?.status === 409 || err?.response?.data?.errorCode === 'GENRE_DUPLICATED') {
        return null; // Caller will do name-matching fallback
      }
      throw err;
    }
  },

  ensurePersonExists: async (name, profileImageUrl) => {
    try {
      const searchRes = await apiClient.get('/api/admin/people/by-name', { params: { name } });
      if (searchRes.data?.success && searchRes.data?.data) {
        return searchRes.data.data;
      }
    } catch (err) {
      console.warn("Failed to check if person exists, creating new:", err);
    }
    
    const validUrl = (profileImageUrl && profileImageUrl.startsWith('http')) ? profileImageUrl : null;
    const response = await apiClient.post('/api/admin/people', {
      fullName: name,
      profileImageUrl: validUrl,
      status: 'ACTIVE'
    });
    return response.data?.data;
  },

  ensureProductionCompanyExists: async (name, logoUrl) => {
    try {
      const searchRes = await apiClient.get('/api/admin/production-companies/by-name', { params: { name } });
      if (searchRes.data?.success && searchRes.data?.data) {
        return searchRes.data.data;
      }
    } catch (err) {
      console.warn("Failed to check if production company exists, creating new:", err);
    }

    const validUrl = (logoUrl && logoUrl.startsWith('http')) ? logoUrl : null;
    const response = await apiClient.post('/api/admin/production-companies', {
      name: name,
      logoUrl: validUrl,
      status: 'ACTIVE'
    });
    return response.data?.data;
  },

  assignCredits: async (publicId, credits) => {
    const response = await apiClient.put(`/api/admin/movies/${publicId}/credits`, { credits });
    return response.data;
  },

  assignProductionCompanies: async (publicId, companies) => {
    const response = await apiClient.put(`/api/admin/movies/${publicId}/production-companies`, { companies });
    return response.data;
  },

  // ─── Media Management ──────────────────────────────────────────────────────
  getMovieMedia: async (movieId) => {
    const response = await apiClient.get(`/api/admin/movies/${movieId}/media`);
    return response.data;
  },

  createMovieMedia: async (movieId, mediaData) => {
    const response = await apiClient.post(`/api/admin/movies/${movieId}/media`, mediaData);
    return response.data;
  },

  updateMovieMedia: async (mediaId, mediaData) => {
    const response = await apiClient.put(`/api/admin/movie-media/${mediaId}`, mediaData);
    return response.data;
  },

  deleteMovieMedia: async (mediaId) => {
    const response = await apiClient.delete(`/api/admin/movie-media/${mediaId}`);
    return response.data;
  },

  // ─── Version Management ────────────────────────────────────────────────────
  getMovieVersions: async (movieId) => {
    const response = await apiClient.get(`/api/admin/movies/${movieId}/versions`);
    return response.data;
  },

  createMovieVersion: async (movieId, versionData) => {
    const response = await apiClient.post(`/api/admin/movies/${movieId}/versions`, versionData);
    return response.data;
  },

  updateMovieVersion: async (versionId, versionData) => {
    const response = await apiClient.put(`/api/admin/movie-versions/${versionId}`, versionData);
    return response.data;
  },

  deleteMovieVersion: async (versionId) => {
    const response = await apiClient.delete(`/api/admin/movie-versions/${versionId}`);
    return response.data;
  },

  // ─── TMDB Integration ──────────────────────────────────────────────────────
  searchTmdbSuggestions: async (keyword, signal) => {
    const response = await apiClient.get('/api/import/search/suggestions', {
      params: { keyword },
      signal,
      timeout: 8000 // Timeout search after 8 seconds
    });
    return response.data;
  },

  getTmdbMovieBundle: async (tmdbId) => {
    const response = await apiClient.get(`/api/import/movies/${tmdbId}/bundle`, {
      timeout: 15000 // Timeout import after 15 seconds
    });
    return response.data;
  },

  getTmdbMovieImages: async (tmdbId) => {
    const response = await apiClient.get(`/api/import/movies/${tmdbId}/images`, {
      timeout: 10000
    });
    return response.data;
  },

  getLatestTop20: async (limit = 20) => {
    const response = await apiClient.get('/api/tmdb/movies/latest-top20', {
      params: { limit }
    });
    return response.data;
  }
};

export default adminMovieService;
