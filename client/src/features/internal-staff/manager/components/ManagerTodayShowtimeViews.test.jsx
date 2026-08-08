import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import ManagerTodayShowtimeViews from './ManagerTodayShowtimeViews';

const buildShowtime = ({ id, startTime, title, room, status = 'DRAFT', posterUrl }) => ({
  showtimePublicId: id,
  serviceDate: '2026-08-08',
  startTime,
  endTime: new Date(new Date(startTime).getTime() + (90 * 60_000)).toISOString(),
  status,
  movie: { publicId: `movie-${title}`, title, posterUrl },
  movieVersion: { versionName: '2D', format: '2D' },
  cinema: { publicId: 'cinema-1', name: 'LoraFilm Landmark 81', timezone: 'Asia/Ho_Chi_Minh' },
  auditorium: { publicId: `room-${room}`, name: room, cleaningBufferMinutes: 15 },
});

const showtimes = [
  buildShowtime({ id: 'showtime-1', startTime: '2026-08-08T06:00:00Z', title: 'Venganza', room: 'Screen 01', status: 'OPEN_FOR_BOOKING', posterUrl: '/posters/venganza.jpg' }),
  buildShowtime({ id: 'showtime-2', startTime: '2026-08-08T07:30:00Z', title: 'Huyền Thoại Aang', room: 'Screen 03' }),
  buildShowtime({ id: 'showtime-3', startTime: '2026-08-08T09:00:00Z', title: 'Venganza', room: 'Screen 01' }),
];

describe('ManagerTodayShowtimeViews', () => {
  it('switches between day, movie, list, and read-only timeline views', () => {
    render(
      <MemoryRouter>
        <ManagerTodayShowtimeViews showtimes={showtimes} serviceDate="2026-08-08" now="2026-08-08T05:00:00Z" />
      </MemoryRouter>,
    );

    const viewGroup = screen.getByRole('group', { name: 'Chế độ xem việc cần xử lý hôm nay' });
    expect(viewGroup).toHaveTextContent('Theo ngày');
    expect(viewGroup).toHaveTextContent('Theo phim');
    expect(viewGroup).toHaveTextContent('Danh sách');
    expect(viewGroup).toHaveTextContent('Sơ đồ');
    expect(screen.getByRole('region', { name: 'Lịch cần xử lý theo ngày' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Theo phim' }));
    expect(screen.getByRole('region', { name: 'Lịch cần xử lý theo phim' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Poster Venganza' })).toHaveAttribute('src', '/posters/venganza.jpg');
    expect(screen.getByText('3 suất còn xử lý ngày 08/08/2026')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Danh sách' }));
    expect(screen.getByRole('region', { name: 'Danh sách suất cần xử lý' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Sơ đồ' }));
    expect(screen.getByRole('region', { name: 'Phòng chiếu × thời gian' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Venganza, 13:00, Đang mở bán/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Mở chi tiết/ })).not.toBeInTheDocument();
  });

  it('hides past drafts from every operational view and reports the hidden count', () => {
    const pastDraft = buildShowtime({ id: 'past', startTime: '2026-08-08T03:00:00Z', title: 'Suất đã qua', room: 'Screen 02' });
    const futureDraft = buildShowtime({ id: 'future', startTime: '2026-08-08T07:00:00Z', title: 'Suất còn xử lý', room: 'Screen 02' });
    render(
      <MemoryRouter>
        <ManagerTodayShowtimeViews showtimes={[pastDraft, futureDraft]} serviceDate="2026-08-08" now="2026-08-08T05:00:00Z" />
      </MemoryRouter>,
    );

    expect(screen.getByText('1 suất còn xử lý ngày 08/08/2026')).toBeInTheDocument();
    expect(screen.getByText('1 quá giờ đã ẩn')).toBeInTheDocument();
    expect(screen.queryByText('Suất đã qua')).not.toBeInTheDocument();
    expect(screen.getByText('Suất còn xử lý')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Danh sách' }));
    expect(screen.queryByText('Suất đã qua')).not.toBeInTheDocument();
  });

  it('keeps loading and retry states inside the same dashboard section', () => {
    const onRetry = vi.fn();
    const { rerender } = render(
      <MemoryRouter>
        <ManagerTodayShowtimeViews serviceDate="2026-08-08" isLoading />
      </MemoryRouter>,
    );
    expect(screen.getByText('Đang tải lịch chiếu…')).toBeInTheDocument();

    rerender(
      <MemoryRouter>
        <ManagerTodayShowtimeViews serviceDate="2026-08-08" error="Không thể tải lịch." onRetry={onRetry} />
      </MemoryRouter>,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Tải lại' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});
