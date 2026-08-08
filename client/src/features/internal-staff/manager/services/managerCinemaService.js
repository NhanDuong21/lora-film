import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data;

const managerCinemaService = {
  getAssignedCinemas: async () => unwrap(await apiClient.get('/api/manager/cinemas')) || [],

  getShowtimes: async params => unwrap(await apiClient.get('/api/manager/showtimes', { params })) || {
    data: [],
    pageNo: 0,
    pageSize: 20,
    totalElements: 0,
    totalPages: 0,
    last: true,
  },

  updateOperatingHours: async (cinemaPublicId, operatingHours) => unwrap(await apiClient.put(
    `/api/manager/cinemas/${cinemaPublicId}/operating-hours`,
    operatingHours,
  )),
};

export default managerCinemaService;
