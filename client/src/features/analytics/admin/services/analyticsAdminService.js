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

export const acknowledgeAnalyticsAlert = async id =>
  unwrap(await apiClient.patch(`/api/analytics/alerts/${id}/acknowledge`));

export const updateAnalyticsRecommendation = async (id, status) =>
  unwrap(await apiClient.patch(`/api/analytics/recommendations/${id}/status`, { status }));
