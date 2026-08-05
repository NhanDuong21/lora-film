import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import AdminPricingPolicyFormPage from './AdminPricingPolicyFormPage';

vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: {
    getCinemas: vi.fn(),
    getAdminCinemaDetail: vi.fn(),
  },
}));
vi.mock('@/features/facilities/admin/services/adminRoomService', () => ({
  default: { getSeatTypes: vi.fn() },
}));
vi.mock('../services/adminPricingService', () => ({
  default: {
    createPolicy: vi.fn(),
    getPolicy: vi.fn(),
    updatePolicy: vi.fn(),
    previewResolution: vi.fn(),
  },
}));

describe('AdminPricingPolicyFormPage auditorium scope', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminCinemaService.getCinemas.mockResolvedValue({
      data: { data: [
        { publicId: 'cinema-1', name: 'Rạp 1', timezone: 'Asia/Ho_Chi_Minh' },
        { publicId: 'cinema-2', name: 'Rạp 2', timezone: 'Asia/Ho_Chi_Minh' },
      ] },
    });
    adminRoomService.getSeatTypes.mockResolvedValue({
      data: [{ publicId: 'seat-vip', name: 'Ghế VIP', code: 'VIP', status: 'ACTIVE' }],
    });
    adminCinemaService.getAdminCinemaDetail.mockImplementation(cinemaId => Promise.resolve({
      data: {
        auditoriums: cinemaId === 'cinema-1'
          ? [{ publicId: 'room-1', name: 'Phòng 1', screenType: 'IMAX' }]
          : [{ publicId: 'room-2', name: 'Phòng 2', screenType: 'STANDARD' }],
      },
    }));
  });

  it('supports explicit auditorium scope, derives screen type, and clears an invalid room on cinema change', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/pricing/create']}>
        <Routes>
          <Route path="/admin/pricing/create" element={<AdminPricingPolicyFormPage />} />
        </Routes>
      </MemoryRouter>,
    );

    const cinemaSelect = await screen.findByRole('combobox', { name: 'Rạp' });
    fireEvent.change(cinemaSelect, { target: { value: 'cinema-1' } });
    const scope = screen.getByRole('combobox', { name: 'Phạm vi quy tắc 1' });
    fireEvent.change(scope, { target: { value: 'AUDITORIUM' } });
    const room = await screen.findByRole('combobox', { name: 'Phòng chiếu quy tắc 1' });
    await waitFor(() => expect(room.querySelector('option[value="room-1"]')).not.toBeNull());
    fireEvent.change(room, { target: { value: 'room-1' } });
    expect(screen.getByText('Loại màn hình: IMAX')).toBeInTheDocument();

    fireEvent.change(cinemaSelect, { target: { value: 'cinema-2' } });
    await waitFor(() => expect(screen.getByRole('combobox', { name: 'Phòng chiếu quy tắc 1' })).toHaveValue(''));
    expect(screen.queryByText('Loại màn hình: IMAX')).not.toBeInTheDocument();
  });

  it('prefills the cinema and effective dates when opened from an auto-schedule repair action', async () => {
    render(
      <MemoryRouter initialEntries={[
        '/admin/pricing/create?cinema=cinema-1&effectiveFrom=2026-07-24&effectiveTo=2026-07-26&returnTo=%2Fadmin%2Fshowtime-schedules%2Fpreview-1',
      ]}>
        <Routes>
          <Route path="/admin/pricing/create" element={<AdminPricingPolicyFormPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByRole('status')).toHaveTextContent('Tạo bảng giá để tiếp tục lịch đang kiểm tra');
    expect(screen.getByRole('combobox', { name: 'Rạp' })).toHaveValue('cinema-1');
    expect(screen.getByLabelText('Bắt đầu áp dụng')).toHaveValue('2026-07-24');
    expect(screen.getByLabelText(/Kết thúc áp dụng/)).toHaveValue('2026-07-26');
    await waitFor(() => expect(screen.getByLabelText('Tên bảng giá')).toHaveValue('Bảng giá Rạp 1'));
  });
});
