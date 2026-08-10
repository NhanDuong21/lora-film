import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ManagerStaffPage from './ManagerStaffPage';
import managerCinemaService from '../services/managerCinemaService';

const cinemaId = 'b1575c2d-9081-11f1-bf65-0ebab02bf6f5';
const selectedCinema = { publicId: cinemaId, name: 'LoraFilm Landmark 81' };

vi.mock('react-router-dom', async importOriginal => {
  const actual = await importOriginal();
  return {
    ...actual,
    useOutletContext: () => ({
      selectedCinema,
      selectedCinemaId: cinemaId,
      cinemaState: { loading: false, error: '' },
    }),
  };
});

vi.mock('../services/managerCinemaService', () => ({
  default: {
    getStaff: vi.fn(),
    getShifts: vi.fn(),
    getAttendance: vi.fn(),
    getLeaveRequests: vi.fn(),
    createShift: vi.fn(),
    cancelShift: vi.fn(),
    reviewLeave: vi.fn(),
  },
}));

const staff = [
  {
    accountId: 2,
    employeeCode: 'EMP-0002',
    fullName: 'Đặng Thành Nhân',
    avatarUrl: null,
    email: 'nhandt@example.com',
    departmentCode: 'OPS',
    departmentName: 'Vận hành rạp',
    positionCode: 'OPS_MANAGER',
    positionName: 'Quản lý vận hành',
    status: 'ACTIVE',
  },
  {
    accountId: 3,
    employeeCode: 'EMP-0003',
    fullName: 'Nguyễn Hoàng Nhân',
    avatarUrl: null,
    email: 'nhan@example.com',
    departmentCode: 'OPS',
    departmentName: 'Vận hành rạp',
    positionCode: 'BOX_OFFICE',
    positionName: 'Nhân viên quầy vé',
    status: 'ACTIVE',
  },
];

describe('ManagerStaffPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    managerCinemaService.getStaff.mockResolvedValue(staff);
    managerCinemaService.getShifts.mockResolvedValue([]);
    managerCinemaService.getAttendance.mockResolvedValue([]);
    managerCinemaService.getLeaveRequests.mockResolvedValue([]);
    managerCinemaService.createShift.mockResolvedValue({ id: 101 });
  });

  it('ưu tiên lịch tuần và hiển thị đúng nhân sự trong phạm vi rạp', async () => {
    render(<ManagerStaffPage />);

    expect(await screen.findByRole('heading', { name: 'Lịch ca & nhân sự' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Lịch ca tuần' })).toHaveClass('bg-white');
    expect(screen.getByText('2 nhân viên chưa có ca')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Ảnh mặc định theo vai trò của Đặng Thành Nhân' })).toHaveAttribute('src', '/images/manager_avt.png');
    expect(screen.getByRole('img', { name: 'Ảnh mặc định theo vai trò của Nguyễn Hoàng Nhân' })).toHaveAttribute('src', '/images/employee_banve.png');

    expect(managerCinemaService.getStaff).toHaveBeenCalledWith(cinemaId);
    expect(managerCinemaService.getShifts).toHaveBeenCalledWith(expect.objectContaining({
      cinemaPublicId: cinemaId,
      from: expect.any(String),
      to: expect.any(String),
    }));
  });

  it('xếp ca từ đúng ô nhân viên và ngày làm việc bằng modal', async () => {
    render(<ManagerStaffPage />);

    const scheduleButtons = await screen.findAllByRole('button', { name: /Xếp ca cho Nguyễn Hoàng Nhân ngày/ });
    fireEvent.click(scheduleButtons[0]);

    expect(screen.getByRole('heading', { name: 'Xếp ca làm việc' })).toBeInTheDocument();
    expect(screen.getAllByText('Nguyễn Hoàng Nhân').length).toBeGreaterThan(1);
    fireEvent.change(screen.getByLabelText(/Bắt đầu/), { target: { value: '14:00' } });
    fireEvent.change(screen.getByLabelText(/Kết thúc/), { target: { value: '22:00' } });
    fireEvent.change(screen.getByPlaceholderText(/Quầy vé ca sáng/), { target: { value: 'Quầy vé ca chiều' } });
    fireEvent.click(screen.getByRole('button', { name: 'Lưu ca làm' }));

    await waitFor(() => expect(managerCinemaService.createShift).toHaveBeenCalledWith(
      cinemaId,
      expect.objectContaining({
        employeeId: 3,
        scheduledStart: expect.stringContaining('T14:00:00'),
        scheduledEnd: expect.stringContaining('T22:00:00'),
        location: 'LoraFilm Landmark 81',
        note: 'Quầy vé ca chiều',
      })
    ));
    expect(await screen.findByText('Đã xếp ca và cập nhật vào lịch tuần của nhân viên.')).toBeInTheDocument();
  });
});
