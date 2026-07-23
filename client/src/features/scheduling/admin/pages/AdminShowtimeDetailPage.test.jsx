import { fireEvent, render, screen } from '@testing-library/react';
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
    expect(screen.queryByText('2026-07-25')).not.toBeInTheDocument();
  });

  it('warns and formats in UTC when cinema timezone is invalid', () => {
    useShowtimeDetail.mockReturnValue(detailValue('Invalid/Timezone'));
    renderPage();

    expect(screen.getByRole('status')).toHaveTextContent('UTC dự phòng');
    expect(screen.getByText('18:30')).toBeInTheDocument();
  });

  it('uses action verbs while keeping the current status badge distinct', () => {
    useShowtimeDetail.mockReturnValue({
      ...detailValue(),
      showtime: { ...detailValue().showtime, status: 'DRAFT' },
    });
    const { unmount } = renderPage();
    expect(screen.getByRole('button', { name: 'Mở bán' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hủy suất chiếu' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'OPEN' })).not.toBeInTheDocument();
    unmount();

    useShowtimeDetail.mockReturnValue({
      ...detailValue(),
      showtime: { ...detailValue().showtime, status: 'OPEN_FOR_BOOKING' },
    });
    const open = renderPage();
    expect(screen.getByLabelText('Trạng thái hiện tại: Đang mở bán')).toHaveTextContent('Đang mở bán');
    expect(screen.getByRole('button', { name: 'Đóng bán' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hủy suất chiếu' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Đã đóng bán' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Đã hủy' })).not.toBeInTheDocument();
    open.unmount();

    useShowtimeDetail.mockReturnValue({
      ...detailValue(),
      showtime: { ...detailValue().showtime, status: 'CLOSED' },
    });
    renderPage();
    expect(screen.getByLabelText('Trạng thái hiện tại: Đã đóng bán')).toHaveTextContent('Đã đóng bán');
    expect(screen.getByRole('button', { name: 'Đánh dấu đã chiếu xong' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hủy suất chiếu' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Mở bán' })).not.toBeInTheDocument();
  });

  it('explains temporarily disabled status actions without changing transition capability rules', () => {
    useShowtimeDetail.mockReturnValue({
      ...detailValue(),
      showtime: { ...detailValue().showtime, status: 'OPEN_FOR_BOOKING' },
      isUpdatingStatus: true,
    });
    renderPage();

    const close = screen.getByRole('button', { name: 'Đóng bán' });
    expect(close).toBeDisabled();
    expect(close).toHaveAttribute('title', 'Đang cập nhật trạng thái suất chiếu; vui lòng đợi.');
    expect(screen.queryByRole('button', { name: 'Mở bán' })).not.toBeInTheDocument();
  });

  it('renders pricing names and an audit timeline with localized fallbacks and preview link', () => {
    useShowtimeDetail.mockReturnValue({
      ...detailValue(),
      prices: {
        currency: 'VND',
        prices: [{
          seatTypeId: 'seat-type-uuid',
          seatTypeName: 'Ghế VIP',
          seatTypeCode: 'VIP',
          price: 120000,
        }],
      },
      history: [{
        previousStatus: null,
        newStatus: 'DRAFT',
        changedAt: '2026-07-24T18:45:00Z',
        reason: 'Showtime created',
        changedBy: 42,
        source: 'AUTO',
        previewPublicId: 'preview-1',
      }, {
        previousStatus: 'DRAFT',
        newStatus: 'OPEN_FOR_BOOKING',
        changedAt: '2026-07-24T19:00:00Z',
        reason: null,
        changedBy: null,
        source: 'MANUAL',
      }],
    });
    renderPage();

    expect(screen.getByText('Ghế VIP')).toBeInTheDocument();
    expect(screen.getByText('VIP')).toBeInTheDocument();
    expect(screen.getByText(/120.000/)).toBeInTheDocument();
    expect(screen.getByText('Khởi tạo → Bản nháp')).toBeInTheDocument();
    expect(screen.getByText('Bản nháp → Đang mở bán')).toBeInTheDocument();
    expect(screen.getByText('Đã tạo suất chiếu')).toBeInTheDocument();
    expect(screen.getByText('Không ghi nhận lý do')).toBeInTheDocument();
    expect(screen.getByText(/Người dùng #42 · Tạo tự động/)).toBeInTheDocument();
    expect(screen.getByText(/Không xác định · Tạo thủ công/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Mở bản xem trước nguồn' }));
  });
});
