import apiClient from '@/services/apiClient';

const data = (response) => response.data.data;

export const getDashboard = async () => data(await apiClient.get('/api/users/dashboard'));
export const getUserAudits = async (params = {}) =>
  data(await apiClient.get('/api/admin/user-audits', { params }));
export const getCustomer = async (id) => data(await apiClient.get(`/api/users/customers/${id}`));
export const getCustomers = async (params = {}) => data(await apiClient.get('/api/users/customers', { params }));
export const setCustomerBlocked = async (customerId, blocked) =>
  data(await apiClient.put(`/api/users/customers/${customerId}/${blocked ? 'block' : 'unblock'}`));
export const getEmployees = async (params = {}) => data(await apiClient.get('/api/users/employees', { params }));
export const getEmployee = async (accountId) =>
  data(await apiClient.get(`/api/users/employees/${accountId}`));
export const createEmployee = async (payload) => data(await apiClient.post('/api/users/employees', payload));
export const updateEmployee = async (accountId, payload) =>
  data(await apiClient.put(`/api/users/employees/${accountId}`, payload));
export const changeEmployeeStatus = async (accountId, action) =>
  data(await apiClient.put(`/api/users/employees/${accountId}/${action}`));
export const transferEmployee = async (accountId, departmentId, positionId) =>
  data(await apiClient.put(`/api/users/employees/${accountId}/transfer`, null, {
    params: { departmentId, positionId }
  }));
export const getEmployeeDocuments = async (accountId, includeHistory = false) =>
  data(await apiClient.get(
    `/api/users/employees/${accountId}/documents${includeHistory ? '/history' : ''}`
  ));
export const uploadEmployeeDocument = async (accountId, payload) => {
  const formData = new FormData();
  formData.append('file', payload.file);
  formData.append('documentType', payload.documentType);
  if (payload.documentName) formData.append('documentName', payload.documentName);
  if (payload.issuedDate) formData.append('issuedDate', payload.issuedDate);
  if (payload.expiredDate) formData.append('expiredDate', payload.expiredDate);
  return data(await apiClient.post(`/api/users/employees/${accountId}/documents`, formData));
};
export const downloadEmployeeDocument = async (accountId, documentId) =>
  (await apiClient.get(`/api/users/employees/${accountId}/documents/${documentId}/file`, {
    responseType: 'blob'
  })).data;
export const deleteEmployeeDocument = async (accountId, documentId) =>
  data(await apiClient.delete(`/api/users/employees/${accountId}/documents/${documentId}`));
export const getDepartments = async () => data(await apiClient.get('/api/users/departments'));
export const searchDepartments = async (params = {}) =>
  data(await apiClient.get('/api/users/departments/search', { params }));
export const createDepartment = async (payload) => data(await apiClient.post('/api/users/departments', payload));
export const updateDepartment = async (id, payload) => data(await apiClient.put(`/api/users/departments/${id}`, payload));
export const deleteDepartment = async (id) => data(await apiClient.delete(`/api/users/departments/${id}`));
export const getPositions = async () => data(await apiClient.get('/api/users/positions'));
export const searchPositions = async (params = {}) =>
  data(await apiClient.get('/api/users/positions/search', { params }));
export const createPosition = async (payload) => data(await apiClient.post('/api/users/positions', payload));
export const updatePosition = async (id, payload) => data(await apiClient.put(`/api/users/positions/${id}`, payload));
export const deletePosition = async (id) => data(await apiClient.delete(`/api/users/positions/${id}`));
export const getPayrolls = async (params = {}) => data(await apiClient.get('/api/users/payrolls', { params }));
export const getPayroll = async (id) => data(await apiClient.get(`/api/users/payrolls/${id}`));
export const createPayroll = async (payload) => data(await apiClient.post('/api/users/payrolls', payload));
export const updatePayroll = async (id, payload) => data(await apiClient.put(`/api/users/payrolls/${id}`, payload));
export const changePayrollStatus = async (id, action) =>
  data(await apiClient.put(`/api/users/payrolls/${id}/${action}`));
export const getMyPayrolls = async (params = {}) =>
  data(await apiClient.get('/api/users/payrolls/me', { params }));
