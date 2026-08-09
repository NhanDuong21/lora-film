import apiClient from '@/services/apiClient';

export const getMyEmployeeWorkContext = async () => {
  const response = await apiClient.get('/api/users/employees/me');
  return response.data.data;
};

