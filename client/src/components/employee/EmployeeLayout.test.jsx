import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuth } from '@/contexts/AuthContext';
import { EMPLOYEE_PERMISSIONS } from '@/features/internal-staff/employee/employeeAccess';
import EmployeeLayout from './EmployeeLayout';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));

const renderLayout = () => render(
  <MemoryRouter initialEntries={['/employee/dashboard']}>
    <Routes>
      <Route path="/employee" element={<EmployeeLayout />}>
        <Route path="dashboard" element={<div>Dashboard content</div>} />
      </Route>
    </Routes>
  </MemoryRouter>,
);

describe('EmployeeLayout permission menu', () => {
  beforeEach(() => {
    useAuth.mockReset();
  });

  it('shows only employee functions granted by the token', () => {
    useAuth.mockReturnValue({
      user: {
        role: 'EMPLOYEE',
        fullName: 'Nhân viên thử nghiệm',
        permissions: [
          EMPLOYEE_PERMISSIONS.DASHBOARD_VIEW,
          EMPLOYEE_PERMISSIONS.PAYROLL_VIEW,
        ],
      },
      logout: vi.fn(),
    });

    renderLayout();

    expect(screen.getByText('Tổng quan ca')).toBeInTheDocument();
    expect(screen.getByText('Phiếu lương')).toBeInTheDocument();
    expect(screen.getByText('Vận hành tại quầy')).toBeInTheDocument();
    expect(screen.getByText('Cá nhân')).toBeInTheDocument();
    expect(screen.queryByText('Chấm công')).not.toBeInTheDocument();
    expect(screen.queryByText('Thu tiền tại quầy')).not.toBeInTheDocument();
    expect(screen.queryByText('Bán vé tại quầy')).not.toBeInTheDocument();
  });

  it('chỉ hiện bán vé tại quầy khi có đủ quyền tạo đơn và thu tiền', () => {
    useAuth.mockReturnValue({
      user: {
        role: 'EMPLOYEE',
        permissions: [
          EMPLOYEE_PERMISSIONS.BOOKING_MANAGE,
          EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT,
        ],
      },
      logout: vi.fn(),
    });

    renderLayout();

    expect(screen.getByText('Bán vé tại quầy')).toBeInTheDocument();
    expect(screen.getByText('Đơn tại quầy')).toBeInTheDocument();
    expect(screen.getByText('Hỗ trợ & hoàn tiền')).toBeInTheDocument();
    expect(screen.getByText('Chốt ca & bàn giao')).toBeInTheDocument();
    expect(screen.queryByText('Thu tiền tại quầy')).not.toBeInTheDocument();
  });

  it('vẫn hiện thu tiền tại quầy cho nhân viên chỉ có quyền thu ngân', () => {
    useAuth.mockReturnValue({
      user: {
        role: 'EMPLOYEE',
        permissions: [EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT],
      },
      logout: vi.fn(),
    });

    renderLayout();

    expect(screen.getByText('Thu tiền tại quầy')).toBeInTheDocument();
    expect(screen.queryByText('Bán vé tại quầy')).not.toBeInTheDocument();
  });

  it('requires both attendance permissions before showing check-in', () => {
    useAuth.mockReturnValue({
      user: {
        role: 'EMPLOYEE',
        permissions: [EMPLOYEE_PERMISSIONS.ATTENDANCE_VIEW],
      },
      logout: vi.fn(),
    });

    renderLayout();

    expect(screen.queryByText('Chấm công')).not.toBeInTheDocument();
  });

  it('hiển thị không gian kiểm soát lối vào cho nhân viên soát vé', () => {
    useAuth.mockReturnValue({
      user: {
        role: 'EMPLOYEE',
        fullName: 'Trần Minh Nhân',
        permissions: [
          EMPLOYEE_PERMISSIONS.DASHBOARD_VIEW,
          EMPLOYEE_PERMISSIONS.TICKET_SCAN,
        ],
      },
      logout: vi.fn(),
    });

    renderLayout();

    expect(screen.getByText('Kiểm soát lối vào')).toBeInTheDocument();
    expect(screen.getByText('Soát vé')).toBeInTheDocument();
    expect(screen.getByText('Suất chiếu & cửa phòng')).toBeInTheDocument();
    expect(screen.getByText('Lịch sử soát & sự cố')).toBeInTheDocument();
    expect(screen.getByText('Bàn giao ca soát vé')).toBeInTheDocument();
    expect(screen.queryByText('Bán vé tại quầy')).not.toBeInTheDocument();
  });
});
