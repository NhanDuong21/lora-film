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
  cinemas: [{ publicId: 'cinema-1', slug: 'lora-cinema', name: 'Lora Cinema' }],
  movies: [],
  isLoading: false,
  isOptionsLoading: false,
  cinemaSlug: 'lora-cinema',
  setCinemaSlug: vi.fn(),
  movieSlug: '',
  setMovieSlug: vi.fn(),
  date: '2026-07-25',
  setDate: vi.fn(),
  status: '',
  setStatus: vi.fn(),
  currentPage: 0,
  setCurrentPage: vi.fn(),
  pageSize: 25,
  setPageSize: vi.fn(),
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
    fireEvent.click(screen.getByRole('button', { name: 'Mở trang chỉnh sửa đầy đủ' }));
    expect(onViewDetail).toHaveBeenCalledWith('showtime-1');
  });

  it('offers day, movie, list, and timeline views on the regular showtime page', () => {
    render(<ShowtimeTable {...defaultProps} movies={[{ title: 'Phim thử nghiệm', primaryPoster: '/poster.jpg' }]} />);

    const viewGroup = screen.getByRole('group', { name: 'Chế độ xem lịch chiếu' });
    expect(viewGroup).toHaveTextContent('Theo ngày');
    expect(viewGroup).toHaveTextContent('Theo phim');
    expect(viewGroup).toHaveTextContent('Danh sách');
    expect(viewGroup).toHaveTextContent('Sơ đồ');

    fireEvent.click(screen.getByRole('button', { name: 'Theo ngày' }));
    expect(screen.getByRole('region', { name: 'Lịch chiếu theo ngày' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Theo phim' }));
    expect(screen.getByRole('region', { name: 'Lịch chiếu theo phim' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Danh sách', exact: true }));
    expect(screen.getByRole('region', { name: 'Danh sách suất chiếu' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Sơ đồ' }));
    expect(screen.getByRole('region', { name: 'Phòng chiếu × thời gian' })).toBeInTheDocument();
  });

  it('formats list start and end in each Showtime cinema timezone', () => {
    render(<ShowtimeTable {...defaultProps} />);
    fireEvent.click(screen.getByRole('button', { name: 'Danh sách', exact: true }));

    expect(screen.getByText('01:30')).toBeInTheDocument();
    expect(screen.getAllByText('03:00').length).toBeGreaterThan(0);
    expect(screen.getAllByText('25/07/2026').length).toBeGreaterThanOrEqual(2);
    expect(screen.queryByText('2026-07-25')).not.toBeInTheDocument();
  });

  it('shows a safe fallback indicator for invalid cinema timezone data', () => {
    render(<ShowtimeTable {...defaultProps} showtimes={[showtime('Invalid/Timezone')]} />);
    fireEvent.click(screen.getByRole('button', { name: 'Danh sách', exact: true }));

    expect(screen.getByText('18:30')).toBeInTheDocument();
    expect(screen.getByText('Giờ hiển thị tạm thời')).toBeInTheDocument();
  });

  it('clears ordinary filters and only preserves batch context while preparing a batch', () => {
    const onClearFilters = vi.fn();
    const { rerender } = render(<ShowtimeTable {...defaultProps} onClearFilters={onClearFilters} />);

    fireEvent.click(screen.getByRole('button', { name: 'Đặt lại bộ lọc' }));
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

  it('hides expired drafts from operational views and keeps them in the audit list', () => {
    const expiredDraft = {
      ...showtime(),
      status: 'DRAFT',
      startTime: '2020-07-24T18:30:00Z',
      endTime: '2020-07-24T20:00:00Z',
    };
    render(<ShowtimeTable {...defaultProps} showtimes={[expiredDraft]} />);

    expect(screen.getByRole('region', { name: 'Tóm tắt lịch chiếu' })).toHaveTextContent('1 đã quá giờ');
    expect(screen.getByText('Không còn suất nào có thể xử lý trong trang này')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Phòng chiếu × thời gian' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Xem dữ liệu đối soát' }));
    expect(screen.getByRole('region', { name: 'Danh sách suất chiếu' })).toHaveTextContent('Đã quá giờ');
    expect(screen.getByRole('region', { name: 'Danh sách suất chiếu' })).toHaveTextContent('không thể mở bán');
  });

  it('blocks the room diagram when the selected operational day is still paginated', () => {
    render(<ShowtimeTable {...defaultProps} totalElements={196} totalPages={2} />);

    expect(screen.getByRole('status', { name: 'Sơ đồ vận hành chưa sẵn sàng' })).toHaveTextContent('chỉ có 1/196 suất');
    expect(screen.queryByRole('region', { name: 'Phòng chiếu × thời gian' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Chuyển sang danh sách' }));
    expect(screen.getByRole('region', { name: 'Danh sách suất chiếu' })).toBeInTheDocument();
  });

  it('requires one cinema and one date before opening the operational diagram', () => {
    render(<ShowtimeTable {...defaultProps} cinemaSlug="" date="" />);

    expect(screen.getByRole('region', { name: 'Phạm vi vận hành' })).toHaveTextContent('Chưa chọn rạp');
    expect(screen.getByRole('status', { name: 'Sơ đồ vận hành chưa sẵn sàng' })).toHaveTextContent('Cần chọn rạp và ngày vận hành');
    expect(screen.queryByRole('region', { name: 'Phòng chiếu × thời gian' })).not.toBeInTheDocument();
  });

  it('updates the server-backed date filter from the quick picker inside the diagram', () => {
    const setDate = vi.fn();
    const setCurrentPage = vi.fn();
    render(
      <ShowtimeTable
        {...defaultProps}
        defaultDate="2026-07-25"
        setDate={setDate}
        setCurrentPage={setCurrentPage}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Xem lịch ngày 26/07/2026' }));
    expect(setDate).toHaveBeenCalledWith('2026-07-26');
    expect(setCurrentPage).toHaveBeenCalledWith(0);
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

  it('keeps the open-sale action available while a cached result is being verified in the background', () => {
    render(
      <MemoryRouter>
        <ShowtimeTable
          {...defaultProps}
          batchId="preview-1"
          isBatchReadinessLoading
          batchReadiness={{
            batchId: 'preview-1',
            totalCount: 84,
            eligibleCount: 84,
            alreadyTargetCount: 0,
            skippedCount: 0,
            atomic: true,
            actionAllowed: true,
            reasonGroups: [],
          }}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText('Đang xác minh lại ở nền; bạn vẫn có thể tiếp tục thao tác với kết quả gần nhất.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mở bán 84 suất' })).toBeEnabled();
  });

  it('defaults a batch to the day view, supports movie view, and paginates at 25 items', () => {
    const setPageSize = vi.fn();
    render(
      <MemoryRouter>
        <ShowtimeTable
          {...defaultProps}
          batchId="preview-1"
          totalElements={84}
          totalPages={4}
          setPageSize={setPageSize}
          movies={[{ title: 'Phim thử nghiệm', primaryPoster: '/poster.jpg' }]}
        />
      </MemoryRouter>,
    );

    expect(screen.getByRole('region', { name: 'Lịch chiếu theo ngày' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Phân trang lịch theo ngày' })).toHaveTextContent('1–25 trong 84 suất');
    fireEvent.change(screen.getByRole('combobox', { name: 'Mỗi trang' }), { target: { value: '50' } });
    expect(setPageSize).toHaveBeenCalledWith(50);

    fireEvent.click(screen.getByRole('button', { name: 'Theo phim' }));
    expect(screen.getByRole('region', { name: 'Lịch chiếu theo phim' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Xem nhanh Phim thử nghiệm/ }));
    expect(screen.getByRole('dialog', { name: 'Phim thử nghiệm' })).toBeInTheDocument();
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

  it('routes an expired auto-schedule batch to the replacement flow', () => {
    render(
      <MemoryRouter>
        <ShowtimeTable
          {...defaultProps}
          batchId="preview-expired"
          batchReadiness={{
            batchId: 'preview-expired',
            totalCount: 1,
            eligibleCount: 0,
            alreadyTargetCount: 0,
            skippedCount: 1,
            atomic: true,
            actionAllowed: false,
            reasonGroups: [{ reasonCode: 'SHOWTIME_CANNOT_OPEN_AFTER_START', count: 1 }],
          }}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText('Suất chiếu đã bắt đầu hoặc thời điểm bắt đầu đã qua')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Tạo lịch thay thế' })).toHaveAttribute(
      'href',
      '/admin/showtimes/auto?replaceBatchId=preview-expired',
    );
    expect(screen.getByRole('button', { name: 'Mở bán 0 suất' })).toBeDisabled();
  });
});
