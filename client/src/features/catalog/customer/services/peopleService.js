import apiClient from '@/services/apiClient';

export const getPeople = async ({
  role,
  query,
  availability = 'ALL',
  sort = 'POPULAR',
  page = 0,
  size = 20,
  signal,
}) => {
  const params = { role, availability, sort, page, size };
  if (query?.trim()) params.query = query.trim();
  const response = await apiClient.get('/api/public/people', { params, signal });
  return response.data.data;
};

export const getPerson = async (identifier, { signal } = {}) => {
  const response = await apiClient.get(
    `/api/public/people/${encodeURIComponent(identifier)}`,
    { signal },
  );
  return response.data.data;
};

export const getPersonMovies = async (identifier, { availability = 'ALL', signal } = {}) => {
  const response = await apiClient.get(
    `/api/public/people/${encodeURIComponent(identifier)}/movies`,
    { params: { availability }, signal },
  );
  return response.data.data || [];
};
