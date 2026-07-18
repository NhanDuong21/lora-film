import apiClient from '../../../../services/apiClient';

/**
 * Service to interact with the Movie Service location proxy endpoint.
 * This is restricted to admin roles.
 */
export const searchLocationSuggestions = async ({ query, limit = 8, signal }) => {
  const trimmedQuery = query?.trim() || '';
  
  if (trimmedQuery.length < 2) {
    return { success: true, data: [] };
  }

  const response = await apiClient.get('/api/admin/locations/suggestions', {
    params: {
      q: trimmedQuery,
      limit
    },
    signal
  });

  return response.data;
};
