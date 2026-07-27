import apiClient from '@/services/apiClient';

export const getTopMoviesByRevenue = async ({
  limit = 10,
  startDate,
  endDate
} = {}) => {
  const params = {
    metric: 'REVENUE',
    direction: 'desc',
    limit
  };
  if (startDate) params.startDate = startDate;
  if (endDate) params.endDate = endDate;

  const response = await apiClient.get('/api/analytics/movies/top', { params });
  return response.data?.data || null;
};
