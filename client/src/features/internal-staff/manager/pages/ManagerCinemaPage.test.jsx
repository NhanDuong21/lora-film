import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ManagerCinemaPage from './ManagerCinemaPage';

const cinemaId = 'b1575c2d-9081-11f1-bf65-0ebab02bf6f5';
const reloadCinemas = vi.fn();
const selectedCinema = {
  publicId: cinemaId,
  name: 'LoraFilm Landmark 81',
  status: 'ACTIVE',
  address: '208 Nguyen Huu Canh, Vinhomes Central Park',
  city: 'Ho Chi Minh City',
  hotline: '1900 6868',
  timezone: 'Asia/Ho_Chi_Minh',
  activeAuditoriums: Array.from({ length: 4 }, (_, index) => ({
    publicId: `room-${index + 1}`,
    status: 'ACTIVE',
  })),
  operatingHours: Array.from({ length: 7 }, (_, index) => ({
    dayOfWeek: index + 1,
    openTime: '09:00:00',
    closeTime: '23:30:00',
    isClosed: false,
  })),
};

vi.mock('react-router-dom', async importOriginal => {
  const actual = await importOriginal();
  return {
    ...actual,
    useOutletContext: () => ({
      selectedCinema,
      selectedCinemaId: cinemaId,
      cinemaState: { loading: false, error: '' },
      reloadCinemas,
    }),
  };
});

const renderPage = () => render(<MemoryRouter><ManagerCinemaPage /></MemoryRouter>);

describe('ManagerCinemaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    reloadCinemas.mockResolvedValue(undefined);
  });

  it('hiển thị lịch chuẩn bằng tiếng Việt và đúng phạm vi rạp', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: 'Trung tâm vận hành rạp' })).toBeInTheDocument();
    expect(screen.getAllByText('LoraFilm Landmark 81').length).toBeGreaterThan(0);
    expect(screen.getAllByText('7/7 ngày').length).toBeGreaterThan(0);
    expect(screen.getByText('4/4 phòng')).toBeInTheDocument();
    expect(screen.getByText(/208 Nguyễn Hữu Cảnh/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Kiểm tra lịch chiếu/ })).toHaveAttribute('href', '/manager/showtimes');
    expect(screen.getByRole('link', { name: /Phòng chiếu & bảo trì/ })).toHaveAttribute('href', '/manager/rooms');
    expect(screen.queryByText('ACTIVE')).not.toBeInTheDocument();
  });

  it('chỉ cho Manager xem lịch, không hiển thị trường chỉnh sửa hoặc nút lưu', () => {
    renderPage();

    expect(screen.getByText('Chỉ xem')).toBeInTheDocument();
    expect(screen.getByText('Lịch do Quản trị viên quản lý')).toBeInTheDocument();
    expect(screen.getByText(/Hãy liên hệ Quản trị viên/)).toBeInTheDocument();
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: /Giờ mở cửa/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Lưu giờ mở cửa' })).not.toBeInTheDocument();
  });

  it('tải lại lịch mới nhất từ Admin khi người quản lý yêu cầu', async () => {
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: 'Tải lịch mới nhất' }));

    await waitFor(() => expect(reloadCinemas).toHaveBeenCalled());
    expect(await screen.findByText('Đã tải lịch mới nhất do Quản trị viên thiết lập.')).toBeInTheDocument();
  });
});
