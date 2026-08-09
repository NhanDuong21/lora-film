import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data;

const managerTicketControlService = {
  getSummary: async (cinemaPublicId, date) => unwrap(await apiClient.get(
    '/api/manager/ticket-operations/summary',
    { params: { cinemaPublicId, date } },
  )),

  getHistory: async (cinemaPublicId, date) => unwrap(await apiClient.get(
    '/api/manager/ticket-operations/history',
    { params: { cinemaPublicId, date } },
  )) || [],

  getHandoffs: async (cinemaPublicId, date) => unwrap(await apiClient.get(
    '/api/manager/ticket-operations/handoffs',
    { params: { cinemaPublicId, date } },
  )) || [],
};

export default managerTicketControlService;
