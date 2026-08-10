import apiClient from '@/services/apiClient';

const unwrap = response => response.data?.data ?? null;

export const getAnalyticsDashboard = async ({ startDate, endDate, cinemaKey } = {}) =>
  unwrap(await apiClient.get('/api/analytics/dashboard', {
    params: {
      startDate,
      endDate,
      ...(cinemaKey ? { cinemaKey } : {})
    }
  }));

export const getCinemaKpis = async ({ startDate, endDate, limit = 20 } = {}) =>
  unwrap(await apiClient.get('/api/analytics/cinemas', {
    params: { startDate, endDate, limit }
  })) || [];

export const getCinemaDirectory = async (cinemaKeys = []) => {
  let cinemas = [];

  try {
    const page = unwrap(await apiClient.get('/api/admin/cinemas', {
      params: { page: 0, size: 200, showDeleted: false }
    }));
    cinemas = Array.isArray(page?.data) ? page.data : [];
  } catch {
    try {
      const page = unwrap(await apiClient.get('/api/cinemas', {
        params: { page: 0, size: 200 }
      }));
      cinemas = Array.isArray(page?.data) ? page.data : [];
    } catch {
      // A failed directory request must not make the revenue dashboard unavailable.
    }
  }

  const knownKeys = new Set(cinemas
    .map(cinema => cinema?.publicId || cinema?.cinemaPublicId || cinema?.id)
    .filter(Boolean)
    .map(String));
  const missingKeys = [...new Set(cinemaKeys.filter(Boolean).map(String))]
    .filter(cinemaKey => !knownKeys.has(cinemaKey));

  const resolvedCinemas = await Promise.all(missingKeys.map(async cinemaKey => {
    try {
      return unwrap(await apiClient.get(`/api/cinemas/${encodeURIComponent(cinemaKey)}`));
    } catch {
      return null;
    }
  }));

  return [...cinemas, ...resolvedCinemas.filter(Boolean)];
};

export const acknowledgeAnalyticsAlert = async id =>
  unwrap(await apiClient.patch(`/api/analytics/alerts/${id}/acknowledge`));

export const updateAnalyticsRecommendation = async (id, status) =>
  unwrap(await apiClient.patch(`/api/analytics/recommendations/${id}/status`, { status }));
