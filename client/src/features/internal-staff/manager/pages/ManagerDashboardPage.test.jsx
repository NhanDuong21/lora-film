import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import managerCinemaService from '../services/managerCinemaService';
import ManagerDashboardPage from './ManagerDashboardPage';

vi.mock('../services/managerCinemaService', () => ({
  default: { getShowtimes: vi.fn() },
}));

const showtime = ({ id, title, startTime, endTime }) => ({
  showtimePublicId: id,
  serviceDate: '2026-08-08',
  startTime,
  endTime,
  status: 'DRAFT',
  movie: { publicId: `movie-${id}`, title },
  movieVersion: { versionName: '2D' },
  cinema: { publicId: 'cinema-1', name: 'LoraFilm Landmark 81', timezone: 'Asia/Ho_Chi_Minh' },
  auditorium: { publicId: `room-${id}`, name: 'Screen 01', cleaningBufferMinutes: 15 },
});

const context = {
  cinemas: [{ publicId: 'cinema-1' }],
  selectedCinemaId: 'cinema-1',
  selectedCinema: {
    publicId: 'cinema-1',
    name: 'LoraFilm Landmark 81',
    address: '208 Nguyen Huu Canh',
    activeAuditoriums: [],
  },
  cinemaState: { loading: false, error: '' },
  reloadCinemas: vi.fn(),
};

describe('ManagerDashboardPage today showtimes', () => {
  beforeEach(() => vi.clearAllMocks());

  it('keeps past drafts out of the work queue while retaining upcoming showtimes', async () => {
    managerCinemaService.getShowtimes.mockResolvedValue({
      data: [
        showtime({ id: 'past', title: 'Suất đã qua giờ', startTime: '2020-01-01T01:00:00Z', endTime: '2020-01-01T03:00:00Z' }),
        showtime({ id: 'future', title: 'Suất sắp chiếu', startTime: '2099-01-01T01:00:00Z', endTime: '2099-01-01T03:00:00Z' }),
      ],
    });

    render(
      <MemoryRouter>
        <Routes>
          <Route element={<Outlet context={context} />}>
            <Route index element={<ManagerDashboardPage />} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByText(/1 suất còn xử lý ngày/)).toBeInTheDocument());
    expect(screen.getByText('1 quá giờ đã ẩn')).toBeInTheDocument();
    expect(screen.queryByText('Suất đã qua giờ')).not.toBeInTheDocument();
    expect(screen.getByText('Suất sắp chiếu')).toBeInTheDocument();
  });
});
