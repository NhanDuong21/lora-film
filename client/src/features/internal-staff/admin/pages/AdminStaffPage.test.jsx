import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminStaffPage from './AdminStaffPage';
import {
  assignEmployeeCinema,
  getDashboard,
  getDepartments,
  getEligibleEmployeeAccounts,
  getEmployee,
  getEmployees,
  getEmploymentActions,
  getPositions,
} from '../services/userAdminService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';

const access = vi.hoisted(() => () => true);
const triggerToast = vi.hoisted(() => vi.fn());

vi.mock('../hooks/useAdminAccess', () => ({ default: () => access }));
vi.mock('@/contexts/AuthContext', () => ({ useAuth: () => ({ accountId: 99 }) }));
vi.mock('react-router-dom', async importOriginal => {
  const actual = await importOriginal();
  return { ...actual, useOutletContext: () => ({ triggerToast }) };
});
vi.mock('../services/userAdminService', () => ({
  applyEmploymentAction: vi.fn(),
  assignEmployeeCinema: vi.fn(),
  createEmployee: vi.fn(),
  getDashboard: vi.fn(),
  getDepartments: vi.fn(),
  getEligibleEmployeeAccounts: vi.fn(),
  getEmployee: vi.fn(),
  getEmployees: vi.fn(),
  getEmploymentActions: vi.fn(),
  getPositions: vi.fn(),
}));
vi.mock('../services/authAdminService', () => ({ createEmployeeAccount: vi.fn() }));
vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: { getCinemas: vi.fn() },
}));

const landmarkId = 'b1575c2d-9081-11f1-bf65-0ebab02bf6f5';
const crescentId = 'b1576780-9081-11f1-bf65-0ebab02bf6f5';
const employee = {
  accountId: 3,
  employeeCode: 'EMP-0003',
  fullName: 'Nguyễn Hoàng Nhân',
  avatarUrl: 'https://cdn.example.com/nhan.jpg',
  email: 'nhan@example.com',
  departmentId: 1,
  departmentName: 'Vận hành rạp',
  positionId: 2,
  positionName: 'Nhân viên quầy vé',
  hireDate: '2026-08-08',
  baseSalary: 9500000,
  cinemaPublicId: landmarkId,
  status: 'ACTIVE',
  version: 0,
};

describe('AdminStaffPage phân công rạp', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getEmployees.mockResolvedValue({ content: [employee], totalPages: 1, totalElements: 1 });
    getDepartments.mockResolvedValue([{ id: 1, name: 'Vận hành rạp' }]);
    getPositions.mockResolvedValue([{ id: 2, departmentId: 1, name: 'Nhân viên quầy vé' }]);
    getEligibleEmployeeAccounts.mockResolvedValue({ content: [] });
    getDashboard.mockResolvedValue({ totalEmployees: 1, employeesByStatus: { ACTIVE: 1 } });
    getEmployee.mockResolvedValue(employee);
    getEmploymentActions.mockResolvedValue({ content: [] });
    adminCinemaService.getCinemas.mockResolvedValue({
      data: {
        data: [
          { publicId: landmarkId, name: 'LoraFilm Landmark 81', district: 'Bình Thạnh', city: 'TP.HCM', status: 'ACTIVE' },
          { publicId: crescentId, name: 'LoraFilm Crescent Mall', district: 'Quận 7', city: 'TP.HCM', status: 'ACTIVE' },
        ],
      },
    });
  });

  const renderPage = () => render(<MemoryRouter><AdminStaffPage /></MemoryRouter>);

  it('hiển thị và lọc nhân viên theo tên rạp dễ hiểu', async () => {
    renderPage();

    expect((await screen.findAllByText('LoraFilm Landmark 81')).length).toBeGreaterThan(0);
    expect(screen.getByRole('img', { name: 'Ảnh đại diện của Nguyễn Hoàng Nhân' })).toHaveAttribute(
      'src',
      'https://cdn.example.com/nhan.jpg'
    );
    const cinemaFilter = screen.getByRole('combobox', { name: 'Lọc rạp làm việc' });
    expect(cinemaFilter).toHaveTextContent('Chưa phân công rạp');

    fireEvent.change(cinemaFilter, { target: { value: crescentId } });

    await waitFor(() => expect(getEmployees).toHaveBeenLastCalledWith(expect.objectContaining({
      cinemaPublicId: crescentId,
      excludeCurrentAccount: true,
    })));
  });

  it('cho phép đổi rạp ngay trong hồ sơ nhân viên', async () => {
    assignEmployeeCinema.mockResolvedValue({ ...employee, cinemaPublicId: crescentId, version: 1 });
    renderPage();

    const employeeCard = await screen.findByRole('button', { name: /Nguyễn Hoàng Nhân.*Landmark 81/s });
    fireEvent.click(employeeCard);
    fireEvent.click(await screen.findByRole('button', { name: 'Đổi hoặc gỡ phân công rạp' }));

    const cinemaSelect = screen.getByRole('combobox', { name: 'Rạp làm việc mới' });
    fireEvent.click(cinemaSelect);
    const crescentOptions = await screen.findAllByRole('option', { name: /LoraFilm Crescent Mall/ });
    fireEvent.click(crescentOptions.find(option => option.tagName === 'BUTTON'));
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phân công' }));

    await waitFor(() => expect(assignEmployeeCinema).toHaveBeenCalledWith(3, crescentId));
    expect(triggerToast).toHaveBeenCalledWith('Đã cập nhật rạp làm việc cho nhân viên.');
  });
});
