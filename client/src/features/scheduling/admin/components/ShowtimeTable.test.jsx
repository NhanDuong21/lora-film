import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import ShowtimeTable from './ShowtimeTable';

const showtime = (timezone = 'Asia/Ho_Chi_Minh') => ({
  showtimePublicId: 'showtime-1',
  startTime: '2026-07-24T18:30:00Z',
  endTime: '2026-07-24T20:00:00Z',
  status: 'OPEN_FOR_BOOKING',
  movie: { title: 'Phim thử nghiệm' },
  movieVersion: { versionName: '2D', format: '2D', audioLanguage: 'vi' },
  cinema: { publicId: 'cinema-1', name: 'Lora Cinema', timezone },
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
  batchReadiness: null,
  batchReadinessError: '',
  isBatchReadinessLoading: false,
  onCheckBatch: vi.fn(),
  onOpenBatch: vi.fn(),
  onDeleteBatch: vi.fn(),
  isBatchActionLoading: false,
};

describe('ShowtimeTable cinema timezone', () => {
  it('opens the operational timeline by default and drills into a quick detail drawer', () => {
    const onViewDetail = vi.fn();
    render(<ShowtimeTable {...defaultProps} onViewDetail={onViewDetail} />);

    expect(screen.getByRole('region', { name: 'Phòng chiếu × thời gian' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Phim thử nghiệm.*Mở chi tiết/i }));
    expect(screen.getByRole('dialog', { name: 'Phim thử nghiệm' })).toHaveTextContent('sẵn sàng lúc');
    fireEvent.click(screen.getByRole('button', { name: 'Mở chi tiết và giá vé' }));
    expect(onViewDetail).toHaveBeenCalledWith('showtime-1');
  });

  it('formats list start and end in each Showtime cinema timezone', () => {
    render(<ShowtimeTable {...defaultProps} />);

    expect(screen.getByText('01:30')).toBeInTheDocument();
    expect(screen.getAllByText('03:00').length).toBeGreaterThan(0);
    expect(screen.getAllByText('25/07/2026').length).toBeGreaterThanOrEqual(2);
    expect(screen.queryByText('2026-07-25')).not.toBeInTheDocument();
  });

  it('shows a safe fallback indicator for invalid cinema timezone data', () => {
    render(<ShowtimeTable {...defaultProps} showtimes={[showtime('Invalid/Timezone')]} />);

    expect(screen.getByText('18:30')).toBeInTheDocument();
    expect(screen.getByText('Giờ hiển thị tạm thời')).toBeInTheDocument();
  });

  it('clears ordinary filters and only preserves batch context while preparing a batch', () => {
    const onClearFilters = vi.fn();
    const { rerender } = render(<ShowtimeTable {...defaultProps} onClearFilters={onClearFilters} />);

    fireEvent.click(screen.getByRole('button', { name: 'Xóa bộ lọc' }));
    expect(onClearFilters).toHaveBeenLastCalledWith({ preserveBatch: false });

    rerender(
      <MemoryRouter>
        <ShowtimeTable {...defaultProps} batchId="preview-1" onClearFilters={onClearFilters} />
      </MemoryRouter>,
    );
    fireEvent.click(screen.getByText('Bộ lọc và tùy chọn hiển thị'));
    fireEvent.click(screen.getByRole('button', { name: 'Xóa bộ lọc' }));
    expect(onClearFilters).toHaveBeenLastCalledWith({ preserveBatch: true });
  });

  it('warns when the room diagram only represents the current data page', () => {
    render(<ShowtimeTable {...defaultProps} totalElements={196} totalPages={2} />);

    expect(screen.getByRole('note')).toHaveTextContent('Sơ đồ đang hiển thị 1/196 suất');
    expect(screen.getByRole('navigation', { name: 'Phân trang sơ đồ lịch chiếu' })).toBeInTheDocument();
  });

  it('localizes statuses and routes replacement through the safe original-schedule flow', () => {
    const onOpenBatch = vi.fn();
    render(
      <MemoryRouter>
        <ShowtimeTable
          {...defaultProps}
          batchId="6f1d8ca0-1234-5678-9999-111111111111"
          source="AUTO"
          batchReadiness={{
            batchId: '6f1d8ca0-1234-5678-9999-111111111111',
            totalCount: 1,
            eligibleCount: 1,
            alreadyTargetCount: 0,
            skippedCount: 0,
            atomic: true,
            actionAllowed: true,
            reasonGroups: [],
          }}
          onOpenBatch={onOpenBatch}
        />
      </MemoryRouter>,
    );

    expect(screen.getAllByText('Đang mở bán').length).toBeGreaterThan(0);
    expect(screen.getByText('Lịch tạo tự động')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Lịch 6F1D8CA0' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Thay lịch' }))
      .toHaveAttribute('href', '/admin/showtime-schedules/6f1d8ca0-1234-5678-9999-111111111111');
    expect(screen.getByRole('link', { name: 'Mở bản lịch gốc' }))
      .toHaveAttribute('href', '/admin/showtime-schedules/6f1d8ca0-1234-5678-9999-111111111111');
    expect(screen.getByText('Toàn bộ 1 suất đã sẵn sàng')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Mở bán 1 suất' }));
    expect(onOpenBatch).toHaveBeenCalledTimes(1);
  });

  it('separates blocked showtimes from draft status and provides a direct repair action', () => {
    render(
      <MemoryRouter>
        <ShowtimeTable
          {...defaultProps}
          showtimes={[showtime()]}
          totalElements={1}
          batchId="preview-1"
          source="AUTO"
          batchReadiness={{
            batchId: 'preview-1',
            totalCount: 1,
            eligibleCount: 0,
            alreadyTargetCount: 0,
            skippedCount: 1,
            atomic: true,
            actionAllowed: false,
            reasonGroups: [{ reasonCode: 'PRICING_INCOMPLETE', count: 1 }],
          }}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText('Còn 1 suất bị chặn')).toBeInTheDocument();
    expect(screen.getByText('Chưa có bảng giá đầy đủ')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Thiết lập bảng giá' })).toHaveAttribute(
      'href',
      expect.stringContaining('/admin/pricing?cinema=cinema-1'),
    );
    expect(screen.getByRole('button', { name: 'Mở bán 0 suất' })).toBeDisabled();
    expect(screen.queryByText(/1 cần xử lý/i)).not.toBeInTheDocument();
  });
});
