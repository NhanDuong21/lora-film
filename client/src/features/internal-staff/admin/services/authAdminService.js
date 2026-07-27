import apiClient from '@/services/apiClient';

const data = (response) => response.data.data;

export const getAccounts = async (params = {}) => data(await apiClient.get('/api/accounts', { params }));
export const updateAccountStatus = async (id, status) =>
  data(await apiClient.put(`/api/accounts/${id}/status`, null, { params: { status } }));
export const updateAccountRole = async (id, roleId) =>
  data(await apiClient.put(`/api/accounts/${id}/role`, null, { params: { roleId } }));

export const getRoles = async () => data(await apiClient.get('/api/roles'));
export const createRole = async (payload) => data(await apiClient.post('/api/roles', payload));
export const updateRole = async (id, payload) => data(await apiClient.put(`/api/roles/${id}`, payload));
export const deleteRole = async (id) => data(await apiClient.delete(`/api/roles/${id}`));

export const getPermissions = async () => data(await apiClient.get('/api/permissions'));
export const createPermission = async (payload) => data(await apiClient.post('/api/permissions', payload));
export const updatePermission = async (id, payload) => data(await apiClient.put(`/api/permissions/${id}`, payload));
export const deletePermission = async (id) => data(await apiClient.delete(`/api/permissions/${id}`));

export const getAuthAudits = async (params = {}) => data(await apiClient.get('/api/audits', { params }));
