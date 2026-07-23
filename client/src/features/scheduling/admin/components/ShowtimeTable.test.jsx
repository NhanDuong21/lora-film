import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ShowtimeTable from './ShowtimeTable';

const showtime = (timezone = 'Asia/Ho_Chi_Minh') => ({
  showtimePublicId: 'showtime-1',
  startTime: '2026-07-24T18:30:00Z',
  endTime: '2026-07-24T20:00:00Z',
  status: 'OPEN_FOR_BOOKING',
  movie: { title: 'Phim thử nghiệm' },
  movieVersion: { versionName: '2D', format: '2D', audioLanguage: 'vi' },
  cinema: { name: 'Lora Cinema', timezone },
  auditorium: { name: 'Phòng 1' },
});

const defaultProps = {
  showtimes: [showtime()],
  cinemas: [],
  movies: [],
  isLoading: false,
  isOptionsLoading: false,
  cinemaSlug: '',
  setCinemaSlug: vi.fn(),
  movieSlug: '',
  setMovieSlug: vi.fn(),
  date: '',
  setDate: vi.fn(),
  status: '',
  setStatus: vi.fn(),
  currentPage: 0,
  setCurrentPage: vi.fn(),
  pageSize: 10,
  totalPages: 1,
  totalElements: 1,
  batchId: '',
  source: '',
  onOpenCreate: vi.fn(),
  onOpenAutoSchedule: vi.fn(),
  onViewDetail: vi.fn(),
  onClearBatch: vi.fn(),
  onClearFilters: vi.fn(),
  onTransitionBatch: vi.fn(),
  onDeleteBatch: vi.fn(),
  isBatchActionLoading: false,
};

describe('ShowtimeTable cinema timezone', () => {
  it('formats list start and end in each Showtime cinema timezone', () => {
    render(<ShowtimeTable {...defaultProps} />);

    expect(screen.getByText('01:30')).toBeInTheDocument();
    expect(screen.getByText('03:00')).toBeInTheDocument();
    expect(screen.getAllByText('25/07/2026')).toHaveLength(2);
  });

  it('shows a safe fallback indicator for invalid cinema timezone data', () => {
    render(<ShowtimeTable {...defaultProps} showtimes={[showtime('Invalid/Timezone')]} />);

    expect(screen.getByText('18:30')).toBeInTheDocument();
    expect(screen.getByText('UTC dự phòng')).toBeInTheDocument();
  });

  it('clears source and batch context through the shared clear-filter action', () => {
    const onClearFilters = vi.fn();
    render(<ShowtimeTable {...defaultProps} onClearFilters={onClearFilters} />);

    fireEvent.click(screen.getByRole('button', { name: 'Xóa bộ lọc' }));
    expect(onClearFilters).toHaveBeenCalledTimes(1);
  });
});
