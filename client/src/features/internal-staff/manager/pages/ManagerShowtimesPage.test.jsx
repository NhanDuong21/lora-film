import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import managerCinemaService from '../services/managerCinemaService';
import { clearShowtimeQueryCache } from '@/features/scheduling/admin/utils/showtimeQueryCache';
import ManagerShowtimesPage from './ManagerShowtimesPage';

vi.mock('../services/managerCinemaService', () => ({
  default: {
    getShowtimes: vi.fn(),
    transitionShowtimeStatus: vi.fn(),
  },
}));

const draftShowtime = {
  showtimePublicId: 'showtime-1',
  serviceDate: '2099-08-08',
  startTime: '2099-08-08T06:00:00Z',
  endTime: '2099-08-08T07:30:00Z',
  status: 'DRAFT',
  movie: { publicId: 'movie-1', slug: 'phim-thu-nghiem', title: 'Phim thử nghiệm', posterUrl: '/poster.jpg' },
  movieVersion: { versionName: '2D - Phụ đề', format: '2D' },
  cinema: { publicId: 'cinema-1', slug: 'lorafilm-landmark-81', name: 'LoraFilm Landmark 81', timezone: 'Asia/Ho_Chi_Minh' },
  auditorium: { publicId: 'room-1', name: 'Screen 01 - Standard', cleaningBufferMinutes: 15 },
};

const response = {
  data: [draftShowtime],
  pageNo: 0,
  pageSize: 100,
  totalElements: 1,
  totalPages: 1,
  last: true,
};

const context = {
  selectedCinemaId: 'cinema-1',
  selectedCinema: {
    publicId: 'cinema-1',
    slug: 'lorafilm-landmark-81',
    name: 'LoraFilm Landmark 81',
    address: '208 Nguyen Huu Canh',
  },
  cinemaState: { loading: false, error: '' },
};

const renderPage = () => render(
  <MemoryRouter initialEntries={['/manager/showtimes']}>
    <Routes>
      <Route element={<Outlet context={context} />}>
        <Route path="/manager/showtimes" element={<ManagerShowtimesPage />} />
      </Route>
    </Routes>
  </MemoryRouter>,
);

describe('ManagerShowtimesPage shared scheduling workspace', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearShowtimeQueryCache();
    managerCinemaService.getShowtimes.mockResolvedValue(response);
  });

  it('reuses the admin schedule workspace while locking the assigned cinema and hiding admin-only creation', async () => {
    renderPage();

    expect(await screen.findByRole('region', { name: 'Phòng chiếu × thời gian' })).toBeInTheDocument();
    expect(screen.getByText('Điều phối tại rạp')).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Chế độ xem lịch chiếu' })).toHaveTextContent('Theo ngày');
    expect(screen.getByRole('group', { name: 'Chế độ xem lịch chiếu' })).toHaveTextContent('Theo phim');
    expect(screen.getByRole('group', { name: 'Chế độ xem lịch chiếu' })).toHaveTextContent('Danh sách');
    expect(screen.getByRole('group', { name: 'Chế độ xem lịch chiếu' })).toHaveTextContent('Sơ đồ');
    expect(screen.getByText('LoraFilm Landmark 81', { selector: 'div' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Thêm suất chiếu' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Tạo lịch tuần' })).not.toBeInTheDocument();
    expect(managerCinemaService.getShowtimes).toHaveBeenCalledWith(expect.objectContaining({
      cinemaPublicId: 'cinema-1',
      page: 0,
      size: 100,
    }));
  });

  it('reuses the recent cinema-date response when the manager returns to the screen', async () => {
    const firstView = renderPage();
    expect(await screen.findByRole('region', { name: 'Phòng chiếu × thời gian' })).toBeInTheDocument();
    firstView.unmount();

    renderPage();
    expect(await screen.findByRole('region', { name: 'Phòng chiếu × thời gian' })).toBeInTheDocument();
    expect(managerCinemaService.getShowtimes).toHaveBeenCalledTimes(1);
  });

  it('keeps manager transitions in the shared quick drawer without exposing admin pricing/detail actions', async () => {
    managerCinemaService.transitionShowtimeStatus.mockResolvedValue({
      ...draftShowtime,
      status: 'OPEN_FOR_BOOKING',
    });
    renderPage();

    const timelineItem = await screen.findByRole('button', { name: /Phim thử nghiệm.*Mở chi tiết/i });
    fireEvent.click(timelineItem);
    expect(screen.getByRole('dialog', { name: 'Phim thử nghiệm' })).toBeInTheDocument();
    expect(screen.queryByText('Tình trạng giá')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Mở trang chỉnh sửa đầy đủ/ })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Mở bán' }));
    await waitFor(() => expect(managerCinemaService.transitionShowtimeStatus).toHaveBeenCalledWith(
      'showtime-1',
      'OPEN_FOR_BOOKING',
      null,
    ));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Phim thử nghiệm' })).not.toBeInTheDocument());
  });

  it('lets an operator reopen a future showtime after the room is ready again', async () => {
    const closedShowtime = { ...draftShowtime, status: 'CLOSED' };
    managerCinemaService.getShowtimes.mockResolvedValue({ ...response, data: [closedShowtime] });
    managerCinemaService.transitionShowtimeStatus.mockResolvedValue({
      ...closedShowtime,
      status: 'OPEN_FOR_BOOKING',
    });
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /Phim thử nghiệm.*Mở chi tiết/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Mở bán lại' }));

    await waitFor(() => expect(managerCinemaService.transitionShowtimeStatus).toHaveBeenCalledWith(
      'showtime-1',
      'OPEN_FOR_BOOKING',
      null,
    ));
  });
});
