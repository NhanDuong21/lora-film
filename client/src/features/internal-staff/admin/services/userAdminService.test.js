import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  deleteEmployeeDocument,
  downloadEmployeeDocument,
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
});
