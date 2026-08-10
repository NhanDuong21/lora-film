import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  deleteEmployeeDocument,
  downloadEmployeeDocument,
  applyCustomerAccessAction,
  applyEmploymentAction,
  assignEmployeeCinema,
  applyPayrollAction,
  getEligibleEmployeeAccounts,
  getPayrollSummary,
  generatePayrollFromTimekeeping,
  createWorkShift,
  createWorkShiftBatch,
  correctAttendance,
  applyLeaveRequestAction,
  getEmployeeDocuments,
  uploadEmployeeDocument
} from './userAdminService';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}));

describe('userAdminService employee document contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiClient.get.mockResolvedValue({ data: { data: [] } });
    apiClient.post.mockResolvedValue({ data: { data: { id: 7 } } });
    apiClient.put.mockResolvedValue({ data: { data: { id: 7 } } });
    apiClient.delete.mockResolvedValue({ data: { data: null } });
  });

  it('uses active and history endpoints', async () => {
    await getEmployeeDocuments(42);
    await getEmployeeDocuments(42, true);

    expect(apiClient.get).toHaveBeenNthCalledWith(1, '/api/users/employees/42/documents');
    expect(apiClient.get).toHaveBeenNthCalledWith(2, '/api/users/employees/42/documents/history');
  });

  it('sends document metadata as multipart form data', async () => {
    const file = new File(['%PDF-test'], 'contract.pdf', { type: 'application/pdf' });

    await uploadEmployeeDocument(42, {
      file,
      documentType: 'LABOR_CONTRACT',
      documentName: 'Employment contract',
      issuedDate: '2026-01-01',
      expiredDate: '2027-01-01'
    });

    const [url, formData] = apiClient.post.mock.calls[0];
    expect(url).toBe('/api/users/employees/42/documents');
    expect(formData.get('file')).toBe(file);
    expect(formData.get('documentType')).toBe('LABOR_CONTRACT');
    expect(formData.get('documentName')).toBe('Employment contract');
    expect(formData.get('issuedDate')).toBe('2026-01-01');
    expect(formData.get('expiredDate')).toBe('2027-01-01');
  });

  it('downloads as a blob and deletes by employee and document identity', async () => {
    const blob = new Blob(['document']);
    apiClient.get.mockResolvedValueOnce({ data: blob });

    await expect(downloadEmployeeDocument(42, 7)).resolves.toBe(blob);
    await deleteEmployeeDocument(42, 7);

    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/users/employees/42/documents/7/file',
      { responseType: 'blob' }
    );
    expect(apiClient.delete).toHaveBeenCalledWith('/api/users/employees/42/documents/7');
  });

  it('uses audited action endpoints for customer, workforce and payroll transitions', async () => {
    await applyCustomerAccessAction(12, { type: 'BLOCK', reason: 'Fraud review' });
    await applyEmploymentAction(42, { type: 'SUSPEND', reason: 'Policy review' });
    await applyPayrollAction(9, { type: 'APPROVE', reason: 'Reviewed' });

    expect(apiClient.post).toHaveBeenNthCalledWith(1, '/api/users/customers/12/access-actions',
      { type: 'BLOCK', reason: 'Fraud review' });
    expect(apiClient.post).toHaveBeenNthCalledWith(2, '/api/users/employees/42/actions',
      { type: 'SUSPEND', reason: 'Policy review' });
    expect(apiClient.post).toHaveBeenNthCalledWith(3, '/api/users/payrolls/9/actions',
      { type: 'APPROVE', reason: 'Reviewed' });
  });

  it('updates or clears the employee cinema assignment', async () => {
    await assignEmployeeCinema(42, 'cinema-public-id');
    await assignEmployeeCinema(42, null);

    expect(apiClient.put).toHaveBeenNthCalledWith(1, '/api/users/employees/42/cinema-assignment',
      { cinemaPublicId: 'cinema-public-id' });
    expect(apiClient.put).toHaveBeenNthCalledWith(2, '/api/users/employees/42/cinema-assignment',
      { cinemaPublicId: null });
  });

  it('loads eligible workforce accounts and exact payroll summary period', async () => {
    await getEligibleEmployeeAccounts({ page: 0, size: 100 });
    await getPayrollSummary('2026-08');

    expect(apiClient.get).toHaveBeenNthCalledWith(1, '/api/users/employees/eligible-accounts',
      { params: { page: 0, size: 100 } });
    expect(apiClient.get).toHaveBeenNthCalledWith(2, '/api/users/payrolls/summary',
      { params: { month: '2026-08' } });
  });

  it('uses controlled workforce time and payroll generation contracts', async () => {
    await createWorkShift({ employeeId: 42, scheduledStart: '2026-08-10T08:00:00', scheduledEnd: '2026-08-10T16:00:00' });
    await createWorkShiftBatch({
      employeeId: 42,
      periods: [
        { scheduledStart: '2026-08-11T08:00:00', scheduledEnd: '2026-08-11T12:00:00' },
        { scheduledStart: '2026-08-11T14:00:00', scheduledEnd: '2026-08-11T18:00:00' }
      ],
      location: 'LoraFilm Quận 1'
    });
    await correctAttendance(7, { checkInAt: '2026-08-10T08:00:00', checkOutAt: '2026-08-10T17:00:00', reason: 'Verified' });
    await applyLeaveRequestAction(8, { type: 'APPROVE', note: 'Coverage confirmed' });
    await generatePayrollFromTimekeeping('2026-08');

    expect(apiClient.post).toHaveBeenNthCalledWith(1, '/api/users/workforce/shifts',
      { employeeId: 42, scheduledStart: '2026-08-10T08:00:00', scheduledEnd: '2026-08-10T16:00:00' });
    expect(apiClient.post).toHaveBeenNthCalledWith(2, '/api/users/workforce/shifts/batch', {
      employeeId: 42,
      periods: [
        { scheduledStart: '2026-08-11T08:00:00', scheduledEnd: '2026-08-11T12:00:00' },
        { scheduledStart: '2026-08-11T14:00:00', scheduledEnd: '2026-08-11T18:00:00' }
      ],
      location: 'LoraFilm Quận 1'
    });
    expect(apiClient.post).toHaveBeenNthCalledWith(3, '/api/users/workforce/attendance/7/correction',
      { checkInAt: '2026-08-10T08:00:00', checkOutAt: '2026-08-10T17:00:00', reason: 'Verified' });
    expect(apiClient.post).toHaveBeenNthCalledWith(4, '/api/users/workforce/leave-requests/8/actions',
      { type: 'APPROVE', note: 'Coverage confirmed' });
    expect(apiClient.post).toHaveBeenNthCalledWith(5, '/api/users/payrolls/generate', { month: '2026-08' });
  });
});
