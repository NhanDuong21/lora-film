import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  getEmployeeDocuments,
  uploadEmployeeDocument
} from '../services/userAdminService';
import EmployeeDocumentsPanel from './EmployeeDocumentsPanel';

vi.mock('../services/userAdminService', () => ({
  deleteEmployeeDocument: vi.fn(),
  downloadEmployeeDocument: vi.fn(),
  getEmployeeDocuments: vi.fn(),
  uploadEmployeeDocument: vi.fn()
}));

const employee = {
  accountId: 42,
  employeeCode: 'EMP-0042',
  fullName: 'Nguyễn Văn A'
};

describe('EmployeeDocumentsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getEmployeeDocuments.mockResolvedValue([]);
  });

  it('loads active documents and can switch to history', async () => {
    render(<EmployeeDocumentsPanel employee={employee} onClose={vi.fn()} />);

    expect(await screen.findByText('Chưa có hồ sơ nào.')).toBeInTheDocument();
    expect(getEmployeeDocuments).toHaveBeenCalledWith(42, false);

    fireEvent.click(screen.getByLabelText('Hiện lịch sử đã xóa'));

    await waitFor(() => expect(getEmployeeDocuments).toHaveBeenLastCalledWith(42, true));
  });

  it('rejects an expiry date before the issue date before calling the API', async () => {
    const { container } = render(
      <EmployeeDocumentsPanel employee={employee} onClose={vi.fn()} />
    );
    await screen.findByText('Chưa có hồ sơ nào.');
    fireEvent.change(screen.getByLabelText('Ngày cấp'), { target: { value: '2027-01-01' } });
    fireEvent.change(screen.getByLabelText('Ngày hết hạn'), { target: { value: '2026-01-01' } });
    const input = container.querySelector('input[type="file"]');
    fireEvent.change(input, {
      target: {
        files: [new File(['%PDF-test'], 'contract.pdf', { type: 'application/pdf' })]
      }
    });
    fireEvent.submit(screen.getByRole('button', { name: 'Tải hồ sơ lên' }).closest('form'));

    expect(await screen.findByText('Ngày hết hạn không thể trước ngày cấp.')).toBeInTheDocument();
    expect(uploadEmployeeDocument).not.toHaveBeenCalled();
  });
});
