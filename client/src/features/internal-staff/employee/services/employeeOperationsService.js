import apiClient from '@/services/apiClient';

const unwrap = response => {
  const body = response?.data;
  return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body;
};

export const getCurrentCounterSession = async () =>
  unwrap(await apiClient.get('/api/employee/payments/counter-sessions/current'));

export const getCounterSessionHistory = async () =>
  unwrap(await apiClient.get('/api/employee/payments/counter-sessions/history')) || [];

export const openCounterSession = async payload =>
  unwrap(await apiClient.post('/api/employee/payments/counter-sessions', payload));

export const closeCounterSession = async (sessionPublicId, payload) =>
  unwrap(await apiClient.post(
    `/api/employee/payments/counter-sessions/${sessionPublicId}/close`,
    payload,
  ));

export const searchCounterCustomers = async keyword =>
  unwrap(await apiClient.get('/api/users/customers/counter-search', {
    params: { keyword, page: 0, size: 8, sort: 'joinedAt,desc' },
  }));
