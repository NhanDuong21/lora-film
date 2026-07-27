import apiClient from '@/services/apiClient';

const normalizeMovie = movie => {
  if (!movie) return movie;
  return {
    ...movie,
    id: movie.publicId || movie.id,
    posterUrl: movie.primaryPoster || movie.posterUrl || movie.image || null,
    description: movie.synopsis || movie.description || ''
  };
};

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
  sort,
  signal
}) => {
  const params = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  if (search !== undefined) params.keyword = search;
  if (status !== undefined) {
    if (status === 'NOW_SHOWING') params.status = 'now-showing';
    else if (status === 'UPCOMING') params.status = 'coming-soon';
    else if (status === 'ALL') params.status = 'all';
    else params.status = status;
  } else {
    params.status = 'all';
  }
  if (genreId !== undefined) params.genrePublicId = genreId;
  if (releaseFrom !== undefined) params.releaseFrom = releaseFrom;
  if (releaseTo !== undefined) params.releaseTo = releaseTo;
  if (sort !== undefined) params.sort = sort;

  const response = await apiClient.get("/api/customer/movies", { params, signal });
  const pageData = response.data.data;
  if (!pageData) return pageData;
  const content = (pageData.content || pageData.data || []).map(normalizeMovie);
  return {
    ...pageData,
    content,
    data: content
  };
};

/**
 * Fetch movie detail by ID from the public API
 * @param {string|number} movieId - The ID of the movie to retrieve
 * @returns {Promise<Object>} The movie detail data object
 */
export const getMovieById = async (movieId) => {
  const response = await apiClient.get(`/api/customer/movies/${movieId}`);
  return response.data.data;
};

export const getBookingOptions = async (movieIdentifier, { from, to, signal } = {}) => {
  const response = await apiClient.get(
    `/api/customer/movies/${encodeURIComponent(movieIdentifier)}/booking-options`,
    { params: { from, to }, signal }
  );
  return response.data.data || [];
};

/**
 * Fetch genre list from the public API
 * @returns {Promise<Array>} The genres array
 */
export const getGenres = async () => {
  const response = await apiClient.get("/api/customer/genres");
  return response.data.data;
};

/**
 * Fetch cinemas list from the public API
 * @param {string} [city] - Optional city name to filter
 * @returns {Promise<Object>} The cinemas page response
 */
export const getCinemas = async (city) => {
  const params = {};
  if (city) params.city = city;
  const response = await apiClient.get("/api/cinemas", { params });
  return response.data.data;
};

/**
 * Fetch cinema details by slug
 * @param {string} slug - The cinema slug
 * @returns {Promise<Object>} The cinema details
 */
export const getCinemaBySlug = async (slug) => {
  const response = await apiClient.get(`/api/cinemas/${slug}`);
  return response.data.data;
};

/**
 * Fetch showtimes list from the public API
 * @param {Object} params - Query filters (movieSlug, cinemaSlug, city, date)
 * @returns {Promise<Array>} The list of showtimes
 */
export const getShowtimes = async ({ movieSlug, cinemaSlug, city, date }) => {
  const params = {};
  if (movieSlug) params.movieSlug = movieSlug;
  if (cinemaSlug) params.cinemaSlug = cinemaSlug;
  if (city) params.city = city;
  if (date) params.date = date; // date as YYYY-MM-DD
  const response = await apiClient.get("/api/showtimes", { params });
  return response.data.data;
};

/**
 * Fetch showtime details by public ID
 * @param {string} showtimePublicId - The public ID of the showtime
 * @returns {Promise<Object>} The showtime details
 */
export const getShowtimeDetail = async (showtimePublicId) => {
  const response = await apiClient.get(`/api/showtimes/${showtimePublicId}`);
  return response.data.data;
};

/**
 * Fetch seat layout and pricing by showtime public ID
 * @param {string} showtimePublicId - The public ID of the showtime
 * @returns {Promise<Object>} The auditorium seat layout with price lists
 */
export const getSeatLayout = async (showtimePublicId, { signal } = {}) => {
  const response = await apiClient.get(
    `/api/customer/showtimes/${encodeURIComponent(showtimePublicId)}/seat-layout`,
    { signal }
  );
  return response.data.data;
};
