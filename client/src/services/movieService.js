import apiClient from "./apiClient";

/**
 * Fetch movies list from the public API
 * @param {Object} params - Query parameters
 * @returns {Promise<Object>} The API response data object containing content and pagination metadata
 */
export const getMovies = async ({
  page = 0,
  size = 8,
  search,
  status,
  genreId,
  releaseFrom,
  releaseTo,
  sort
}) => {
  const params = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  if (search !== undefined) params.search = search;
  if (status !== undefined) params.status = status;
  if (genreId !== undefined) params.genreId = genreId;
  if (releaseFrom !== undefined) params.releaseFrom = releaseFrom;
  if (releaseTo !== undefined) params.releaseTo = releaseTo;
  if (sort !== undefined) params.sort = sort;

  const response = await apiClient.get("/api/movies", { params });
  return response.data.data;
};

/**
 * Fetch movie detail by ID from the public API
 * @param {string|number} movieId - The ID of the movie to retrieve
 * @returns {Promise<Object>} The movie detail data object
 */
export const getMovieById = async (movieId) => {
  const response = await apiClient.get(`/api/movies/${movieId}`);
  return response.data.data;
};
