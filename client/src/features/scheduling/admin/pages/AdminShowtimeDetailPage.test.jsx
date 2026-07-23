import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useShowtimeDetail from '../hooks/useShowtimeDetail';
import AdminShowtimeDetailPage from './AdminShowtimeDetailPage';

vi.mock('../hooks/useShowtimeDetail');

const detailValue = (timezone = 'Asia/Ho_Chi_Minh') => ({
  showtime: {
    showtimePublicId: 'showtime-1',
    startTime: '2026-07-24T18:30:00Z',
    endTime: '2026-07-24T20:00:00Z',
    status: 'FINISHED',
    movie: { title: 'Phim thử nghiệm' },
    movieVersion: { versionName: '2D', format: '2D', audioLanguage: 'vi' },
    cinema: { name: 'Lora Cinema', timezone },
    auditorium: { name: 'Phòng 1' },
  },
  history: [{ newStatus: 'FINISHED', changedAt: '2026-07-24T18:45:00Z', reason: '' }],
  prices: { prices: [] },
  isLoading: false,
  isUpdatingStatus: false,
  handleUpdateStatus: vi.fn(),
  fetchDetail: vi.fn(),
});

const renderPage = () => render(
  <MemoryRouter initialEntries={['/admin/showtimes/showtime-1']}>
    <Routes>
      <Route path="/admin/showtimes/:id" element={<AdminShowtimeDetailPage />} />
    </Routes>
  </MemoryRouter>,
);

describe('AdminShowtimeDetailPage cinema timezone', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useShowtimeDetail.mockReturnValue(detailValue());
  });

  it('formats detail and status-history timestamps in the cinema timezone', () => {
    renderPage();

    expect(screen.getByText('01:30')).toBeInTheDocument();
    expect(screen.getByText('03:00')).toBeInTheDocument();
    expect(screen.getByText(/Ngày 25\/07\/2026/)).toBeInTheDocument();
    expect(screen.getByText(/01:45 - 25\/07\/2026/)).toBeInTheDocument();
    expect(screen.getByText(/Múi giờ: Asia\/Ho_Chi_Minh/)).toBeInTheDocument();
  });

  it('warns and formats in UTC when cinema timezone is invalid', () => {
    useShowtimeDetail.mockReturnValue(detailValue('Invalid/Timezone'));
    renderPage();

    expect(screen.getByRole('status')).toHaveTextContent('UTC dự phòng');
    expect(screen.getByText('18:30')).toBeInTheDocument();
  });
});
