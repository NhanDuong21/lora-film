import apiClient from '@/services/apiClient';

const unwrap = response => {
  const body = response?.data;
  return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body;
};

export const scanTicket = async payload =>
  unwrap(await apiClient.post('/api/employee/ticket-operations/scan', payload));

export const getTicketCheckerSummary = async date =>
  unwrap(await apiClient.get('/api/employee/ticket-operations/summary', { params: { date } }));

export const getTicketCheckerShowtimes = async date =>
  unwrap(await apiClient.get('/api/employee/ticket-operations/showtimes', { params: { date } })) || [];

export const getTicketScanHistory = async ({ date, result } = {}) =>
  unwrap(await apiClient.get('/api/employee/ticket-operations/history', {
    params: { date, result: result || undefined },
  })) || [];

export const saveTicketGateHandoff = async (payload, date) =>
  unwrap(await apiClient.post('/api/employee/ticket-operations/handoffs', payload, { params: { date } }));

export const getTicketGateHandoffs = async () =>
  unwrap(await apiClient.get('/api/employee/ticket-operations/handoffs')) || [];
