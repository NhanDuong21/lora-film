import apiClient from '@/services/apiClient';
import { getCinemas } from '@/features/catalog/customer/services/movieService';

export const getMyEmployeeWorkContext = async () => {
  const response = await apiClient.get('/api/users/employees/me');
  return response.data.data;
};

export const getMyEmployeeCinemaContext = async () => {
  const [employee, cinemaPage] = await Promise.all([
    getMyEmployeeWorkContext(),
    getCinemas({ page: 0, size: 100 }),
  ]);
  const cinemas = cinemaPage?.content || cinemaPage?.data || [];
  const cinema = cinemas.find(item => item.publicId === employee?.cinemaPublicId) || null;
  return {
    ...employee,
    cinemaName: cinema?.name || null,
    cinema,
  };
};
