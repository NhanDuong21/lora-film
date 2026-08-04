import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import useAutoScheduleHistory from '../hooks/useAutoScheduleHistory';
import AdminAutoScheduleHistoryPage from './AdminAutoScheduleHistoryPage';

vi.mock('../hooks/useAutoScheduleHistory');

const statuses = ['GENERATING', 'PREVIEWED', 'APPLYING', 'APPLIED', 'EXPIRED', 'FAILED', 'CANCELLED'];
const labels = ['Đang xếp lịch', 'Chờ kiểm tra', 'Đang tạo suất chiếu', 'Đã tạo suất chiếu', 'Đã hết hạn', 'Thất bại', 'Đã hủy'];

const preview = (status, index) => ({
  previewPublicId: `preview-${index}`,
  version: 1,
  cinemaPublicId: 'cinema-1',
  cinemaName: 'LoraFilm Quận 1',
  timezoneSnapshot: 'Asia/Ho_Chi_Minh',
  scheduleFrom: '2026-07-22',
  scheduleTo: '2026-07-23',
  strategyVersion: 'BALANCED_V1_S3',
  applyMode: 'ALL_OR_NOTHING',
  persistedStatus: status,
  displayStatus: status,
  editable: status === 'PREVIEWED',
  applicable: status === 'PREVIEWED',
  totalCandidateCount: 10,
  validCandidateCount: 8,
  rejectedCandidateCount: 2,
  selectedCandidateCount: 4,
  appliedShowtimeCount: status === 'APPLIED' ? 4 : null,
  createdAt: '2026-07-22T10:00:00Z',
  expiresAt: '2026-07-22T11:00:00Z',
  appliedAt: status === 'APPLIED' ? '2026-07-22T10:30:00Z' : null,
  failureReasonSafe: status === 'FAILED' ? 'Auto schedule generation failed' : null,
});

const defaultHookValue = {
  previews: statuses.map(preview),
  cinemas: [],
  query: {
    cinemaPublicId: '', status: '', strategyVersion: '', scheduleFrom: '', scheduleTo: '',
    createdFrom: '', createdTo: '', page: 0, size: 10, sort: 'createdAt,desc',
  },
  rangeError: '',
  isInitialLoading: false,
  isRefreshing: false,
  isCinemaLoading: false,
  error: '',
  cinemaError: '',
  totalElements: 7,
  totalPages: 1,
  commitQuery: vi.fn(),
  resetFilters: vi.fn(),
  fetchHistory: vi.fn(),
  fetchCinemas: vi.fn(),
};

function LocationProbe() {
  const location = useLocation();
  return <span data-testid="location">{location.pathname}{location.search}</span>;
}

const renderPage = () => render(
  <MemoryRouter initialEntries={['/admin/showtime-schedules']}>
    <Routes>
      <Route path="*" element={<><AdminAutoScheduleHistoryPage /><LocationProbe /></>} />
    </Routes>
  </MemoryRouter>,
);

describe('AdminAutoScheduleHistoryPage', () => {
  afterEach(cleanup);

  beforeEach(() => {
    vi.clearAllMocks();
    useAutoScheduleHistory.mockReturnValue({ ...defaultHookValue });
  });

  it('renders every backend display status and authoritative action flags', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: 'Lịch đang soạn' })).toBeInTheDocument();
    labels.forEach(label => expect(screen.getAllByText(label).length).toBeGreaterThan(0));
    expect(screen.getByText('Sẵn sàng tạo suất chiếu')).toBeInTheDocument();
    expect(screen.getByText('Auto schedule generation failed')).toBeInTheDocument();
    expect(screen.getByText('4 suất chiếu đã tạo')).toBeInTheDocument();
    expect(screen.getAllByText('Tiếp tục kiểm tra')).toHaveLength(1);
    expect(screen.getAllByText('22/07/2026 – 23/07/2026').length).toBeGreaterThan(0);
    expect(screen.queryByText('2026-07-22 – 2026-07-23')).not.toBeInTheDocument();
  });

  it('emphasizes applied time/count and links an applied row to its operational batch', () => {
    renderPage();

    expect(screen.getByText('4 suất chiếu đã tạo')).toBeInTheDocument();
    expect(screen.getByText(/Đã tạo suất lúc/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Xem suất chiếu đã tạo' }));
    expect(screen.getByTestId('location')).toHaveTextContent(
      '/admin/showtimes?source=AUTO&batchId=preview-3',
    );
    expect(screen.getByTestId('location')).not.toHaveTextContent('status=DRAFT');
  });

  it('exposes recreate actions for previewed and applied schedules', () => {
    const { unmount } = renderPage();

    expect(screen.getAllByText((_, element) => (
      element.tagName === 'P' && element.textContent === '10 phương án đã xét'
    ))).toHaveLength(statuses.length);
    fireEvent.click(screen.getByRole('button', { name: 'Bỏ & tạo lại' }));
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/showtime-schedules/preview-1');
    unmount();

    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Thay lịch' }));
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/showtime-schedules/preview-3');
  });

  it('uses five compact information groups without the 1450px table floor', () => {
    renderPage();

    const table = screen.getByRole('table');
    expect(within(table).getAllByRole('columnheader')).toHaveLength(5);
    expect(table).not.toHaveClass('min-w-[1450px]');
    expect(table).toHaveAttribute('data-layout', 'laptop-five-groups');
  });

  it('navigates to the existing detail route with previewPublicId', () => {
    renderPage();
    fireEvent.click(screen.getAllByRole('button', { name: 'Xem chi tiết' })[0]);
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/showtime-schedules/preview-0');
  });

  it('uses human-readable identity while retaining the full UUID for routing and technical details', () => {
    renderPage();

    expect(screen.getAllByRole('button', { name: 'LoraFilm Quận 1' }).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Mã lịch: PREVIEW0/).length).toBeGreaterThan(0);
    expect(screen.getByText('previewPublicId: preview-0')).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: 'LoraFilm Quận 1' })[0]);
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/showtime-schedules/preview-0');
  });

  it('shows initial API errors with retry and filtered empty reset states', () => {
    const fetchHistory = vi.fn();
    useAutoScheduleHistory.mockReturnValue({
      ...defaultHookValue,
      previews: [],
      totalElements: 0,
      totalPages: 0,
      error: 'Mất kết nối',
      fetchHistory,
    });
    const { unmount } = renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }));
    expect(fetchHistory).toHaveBeenCalled();
    unmount();

    const resetFilters = vi.fn();
    useAutoScheduleHistory.mockReturnValue({
      ...defaultHookValue,
      previews: [],
      totalElements: 0,
      totalPages: 0,
      query: { ...defaultHookValue.query, status: 'FAILED' },
      resetFilters,
    });
    renderPage();
    expect(screen.getByText('Không có lịch phù hợp với bộ lọc.')).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: 'Xóa bộ lọc' }).at(-1));
    expect(resetFilters).toHaveBeenCalled();
  });
});
