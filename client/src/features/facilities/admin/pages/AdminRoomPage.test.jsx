import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import AdminRoomPage from './AdminRoomPage';

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useOutletContext: () => ({
      triggerToast: vi.fn(),
      triggerConfirm: vi.fn(),
    }),
  };
});

vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: {
    getCinemas: vi.fn(),
    getAdminCinemaDetail: vi.fn(),
  },
}));

vi.mock('@/features/scheduling/admin/services/adminShowtimeService', () => ({
  default: { getShowtimes: vi.fn() },
}));

const rooms = [
  {
    publicId: 'room-1',
    name: 'Screen 01 - Standard',
    screenType: 'STANDARD',
    soundType: 'DOLBY_ATMOS',
    capacity: 96,
    cleaningBufferMinutes: 15,
    status: 'ACTIVE',
  },
  {
    publicId: 'room-2',
    name: 'Screen 02 - Premium',
    screenType: 'STANDARD',
    soundType: 'DOLBY_ATMOS',
    capacity: 24,
    cleaningBufferMinutes: 20,
    status: 'MAINTENANCE',
  },
];

describe('AdminRoomPage', () => {
  beforeEach(() => {
    adminCinemaService.getCinemas.mockResolvedValue({
      success: true,
      data: {
        data: [{
          publicId: 'cinema-1',
          name: 'LoraFilm Landmark 81',
          slug: 'landmark-81',
          timezone: 'Asia/Ho_Chi_Minh',
        }],
      },
    });
    adminCinemaService.getAdminCinemaDetail.mockResolvedValue({
      success: true,
      data: { activeAuditoriums: rooms },
    });
    adminShowtimeService.getShowtimes.mockResolvedValue({
      success: true,
      data: { data: [] },
    });
  });

  it('shows rooms as scannable summary cards instead of table-like rows', async () => {
    render(
      <MemoryRouter>
        <AdminRoomPage />
      </MemoryRouter>,
    );

    const standardRoom = await screen.findByRole('article', { name: 'Screen 01 - Standard' });
    const premiumRoom = screen.getByRole('article', { name: 'Screen 02 - Premium' });

    expect(standardRoom).toHaveClass('rounded-3xl');
    expect(premiumRoom).toHaveClass('rounded-3xl');
    expect(screen.getByText('Danh sách phòng chiếu')).toBeInTheDocument();
    expect(screen.getByText('2 phòng')).toBeInTheDocument();
    expect(screen.getByText('1 sẵn sàng')).toBeInTheDocument();
    expect(screen.getByText('120 ghế')).toBeInTheDocument();
    expect(screen.getAllByText('Sơ đồ ghế')).toHaveLength(2);
    expect(screen.getAllByText('Dọn phòng')).toHaveLength(2);
    expect(screen.getByText('15 phút')).toBeInTheDocument();
    expect(screen.getByText('20 phút')).toBeInTheDocument();
    expect(screen.queryByText('Chưa đặt')).not.toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Quản lý phòng' })).toHaveLength(2);
    expect(screen.queryByRole('button', { name: 'Mở phòng' })).not.toBeInTheDocument();
  });
});
