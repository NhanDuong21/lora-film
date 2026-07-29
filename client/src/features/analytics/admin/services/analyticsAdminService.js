import apiClient from '@/services/apiClient';

const unwrap = response => response.data?.data ?? null;

export const getAnalyticsDashboard = async ({ startDate, endDate } = {}) =>
  unwrap(await apiClient.get('/api/analytics/dashboard', {
    params: { startDate, endDate }
  }));

export const getDailyKpis = async ({ startDate, endDate } = {}) =>
  unwrap(await apiClient.get('/api/analytics/daily', {
    params: { startDate, endDate }
  })) || [];

export const getCinemaKpis = async ({ startDate, endDate, limit = 20 } = {}) =>
  unwrap(await apiClient.get('/api/analytics/cinemas', {
    params: { startDate, endDate, limit }
  })) || [];

export const getMovieKpis = async ({ startDate, endDate, limit = 20 } = {}) =>
  unwrap(await apiClient.get('/api/analytics/movie-performance', {
    params: { startDate, endDate, limit }
  })) || [];

export const getPromotionKpis = async ({ startDate, endDate, limit = 20 } = {}) =>
  unwrap(await apiClient.get('/api/analytics/promotions', {
    params: { startDate, endDate, limit }
  })) || [];

export const getCustomerSegments = async date =>
  unwrap(await apiClient.get('/api/analytics/customer-segments', {
    params: { date }
  })) || [];

export const getForecasts = async ({ startDate, endDate } = {}) =>
  unwrap(await apiClient.get('/api/analytics/forecasts', {
    params: { startDate, endDate }
  })) || [];

export const getInsights = async ({ startDate, endDate } = {}) =>
  unwrap(await apiClient.get('/api/analytics/insights', {
    params: { startDate, endDate }
  })) || [];

export const getRecommendations = async () =>
  unwrap(await apiClient.get('/api/analytics/recommendations')) || [];

export const getAnalyticsAlerts = async () =>
  unwrap(await apiClient.get('/api/analytics/alerts')) || [];

export const getAnalyticsDataQuality = async () =>
  unwrap(await apiClient.get('/api/analytics/data-quality'));

export const acknowledgeAnalyticsAlert = async id =>
  unwrap(await apiClient.patch(`/api/analytics/alerts/${id}/acknowledge`));

export const updateAnalyticsRecommendation = async (id, status) =>
  unwrap(await apiClient.patch(`/api/analytics/recommendations/${id}/status`, { status }));

export const getAnalyticsHealthScore = async date =>
  unwrap(await apiClient.get('/api/analytics/health-score', {
    params: { date }
  }));

export const getAnalyticsAnomalies = async ({ startDate, endDate } = {}) =>
  unwrap(await apiClient.get('/api/analytics/anomalies', {
    params: { startDate, endDate }
  })) || [];

export const getForecastQuality = async date =>
  unwrap(await apiClient.get('/api/analytics/forecast-quality', {
    params: { date }
  })) || [];
