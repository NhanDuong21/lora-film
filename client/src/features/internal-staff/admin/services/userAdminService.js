import apiClient from '@/services/apiClient';

const data = (response) => response.data.data;

export const getDashboard = async (params = {}) => data(await apiClient.get('/api/users/dashboard', { params }));
export const getUserAudits = async (params = {}) =>
  data(await apiClient.get('/api/admin/user-audits', { params }));
export const reviewUserAudit = async (id, payload) =>
  data(await apiClient.put(`/api/admin/user-audits/${id}/review`, payload));
export const getUserProfiles = async (accountIds) => {
  if (!accountIds?.length) return [];
  return data(await apiClient.get('/api/users/admin/batch', { params: { accountIds } }));
};
export const searchUserProfiles = async (query, limit = 20) =>
  data(await apiClient.get('/api/users/admin/search', { params: { query, limit } }));
export const getCustomer = async (id) => data(await apiClient.get(`/api/users/customers/${id}`));
export const getCustomers = async (params = {}) => data(await apiClient.get('/api/users/customers', { params }));
export const applyCustomerAccessAction = async (customerId, payload) =>
  data(await apiClient.post(`/api/users/customers/${customerId}/access-actions`, payload));
export const getEmployees = async (params = {}) => data(await apiClient.get('/api/users/employees', { params }));
export const getEmployee = async (accountId) =>
  data(await apiClient.get(`/api/users/employees/${accountId}`));
export const getEligibleEmployeeAccounts = async (params = {}) =>
  data(await apiClient.get('/api/users/employees/eligible-accounts', { params }));
export const getEmploymentActions = async (accountId, params = {}) =>
  data(await apiClient.get(`/api/users/employees/${accountId}/actions`, { params }));
export const applyEmploymentAction = async (accountId, payload) =>
  data(await apiClient.post(`/api/users/employees/${accountId}/actions`, payload));
export const createEmployee = async (payload) => data(await apiClient.post('/api/users/employees', payload));
export const assignEmployeeCinema = async (accountId, cinemaPublicId) =>
  data(await apiClient.put(`/api/users/employees/${accountId}/cinema-assignment`, { cinemaPublicId }));
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
export const getPayrollSummary = async (month) =>
  data(await apiClient.get('/api/users/payrolls/summary', { params: { month } }));
export const getPayroll = async (id) => data(await apiClient.get(`/api/users/payrolls/${id}`));
export const createPayroll = async (payload) => data(await apiClient.post('/api/users/payrolls', payload));
export const updatePayroll = async (id, payload) => data(await apiClient.put(`/api/users/payrolls/${id}`, payload));
export const applyPayrollAction = async (id, payload) =>
  data(await apiClient.post(`/api/users/payrolls/${id}/actions`, payload));
export const getMyPayrolls = async (params = {}) =>
  data(await apiClient.get('/api/users/payrolls/me', { params }));
export const generatePayrollFromTimekeeping = async (month) =>
  data(await apiClient.post('/api/users/payrolls/generate', { month }));
export const getWorkShifts = async (params = {}) =>
  data(await apiClient.get('/api/users/workforce/shifts', { params }));
export const getMyWorkShifts = async (params = {}) =>
  data(await apiClient.get('/api/users/workforce/shifts/me', { params }));
export const createWorkShift = async (payload) =>
  data(await apiClient.post('/api/users/workforce/shifts', payload));
export const createWorkShiftBatch = async (payload) =>
  data(await apiClient.post('/api/users/workforce/shifts/batch', payload));
export const cancelWorkShift = async (id, payload) =>
  data(await apiClient.post(`/api/users/workforce/shifts/${id}/cancel`, payload));
export const getAttendance = async (params = {}) =>
  data(await apiClient.get('/api/users/workforce/attendance', { params }));
export const getMyAttendance = async (params = {}) =>
  data(await apiClient.get('/api/users/workforce/attendance/me', { params }));
export const checkInShift = async (shiftId) =>
  data(await apiClient.post('/api/users/workforce/attendance/check-in', { shiftId }));
export const checkOutShift = async (shiftId) =>
  data(await apiClient.post('/api/users/workforce/attendance/check-out', { shiftId }));
export const correctAttendance = async (shiftId, payload) =>
  data(await apiClient.post(`/api/users/workforce/attendance/${shiftId}/correction`, payload));
export const getLeaveRequests = async (params = {}) =>
  data(await apiClient.get('/api/users/workforce/leave-requests', { params }));
export const getMyLeaveRequests = async (params = {}) =>
  data(await apiClient.get('/api/users/workforce/leave-requests/me', { params }));
export const createLeaveRequest = async (payload) =>
  data(await apiClient.post('/api/users/workforce/leave-requests', payload));
export const applyLeaveRequestAction = async (id, payload) =>
  data(await apiClient.post(`/api/users/workforce/leave-requests/${id}/actions`, payload));
export const getPiiGovernanceSummary = async () =>
  data(await apiClient.get('/api/users/pii-governance/summary'));
