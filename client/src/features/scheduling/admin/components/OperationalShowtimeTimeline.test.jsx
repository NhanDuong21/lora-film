import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import OperationalShowtimeTimeline from './OperationalShowtimeTimeline';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';

vi.mock('./AutoScheduleTimeline', () => ({
  default: ({ candidates, onOpenDetails }) => (
    <div>
      <span data-testid="operational-palettes">{candidates.map(item => item.palette.solid).join('|')}</span>
      <button type="button" onClick={() => onOpenDetails(candidates[0])}>
        Mở chi tiết suất thử nghiệm
      </button>
    </div>
  ),
}));

vi.mock('@/features/scheduling/admin/services/adminShowtimeService', () => ({
  default: { getPricing: vi.fn() },
}));

const showtime = {
  showtimePublicId: 'showtime-1',
  startTime: '2026-08-08T02:00:00Z',
  endTime: '2026-08-08T03:37:00Z',
  serviceDate: '2026-08-08',
  status: 'DRAFT',
  movie: { publicId: 'movie-1', title: 'Phim thử nghiệm' },
  movieVersion: { versionName: '2D - Phụ đề' },
  cinema: { publicId: 'cinema-1', name: 'Rạp thử nghiệm', timezone: 'Asia/Ho_Chi_Minh' },
  auditorium: { publicId: 'auditorium-1', name: 'Phòng 1', cleaningBufferMinutes: 15 },
};

describe('OperationalShowtimeTimeline pricing status', () => {
  it('loads pricing status when an admin opens a timeline showtime', async () => {
    adminShowtimeService.getPricing.mockResolvedValue({
      success: true,
      data: { complete: true, missingSeatTypes: [], ambiguousSeatTypes: [] },
    });

    render(<OperationalShowtimeTimeline showtimes={[showtime]} />);
    fireEvent.click(screen.getByRole('button', { name: 'Mở chi tiết suất thử nghiệm' }));

    expect(screen.getByText('Đang kiểm tra…')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('Đã đủ giá')).toBeInTheDocument());
    expect(adminShowtimeService.getPricing).toHaveBeenCalledWith('showtime-1');
  });

  it('shows a useful incomplete pricing status from the pricing response', async () => {
    adminShowtimeService.getPricing.mockResolvedValue({
      success: true,
      data: { complete: false, missingSeatTypes: [{ seatTypeId: 'seat-1' }], ambiguousSeatTypes: [] },
    });

    render(<OperationalShowtimeTimeline showtimes={[showtime]} />);
    fireEvent.click(screen.getByRole('button', { name: 'Mở chi tiết suất thử nghiệm' }));

    await waitFor(() => expect(screen.getByText('Chưa có giá đầy đủ')).toBeInTheDocument());
  });

  it('offers a seven-day quick picker that updates the outer operational filter', () => {
    const onRequestedDateChange = vi.fn();
    render(
      <OperationalShowtimeTimeline
        showtimes={[showtime]}
        requestedDate="2026-08-08"
        dateOptions={['2026-08-08', '2026-08-09', '2026-08-10']}
        onRequestedDateChange={onRequestedDateChange}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Xem lịch ngày 09/08/2026' }));
    expect(onRequestedDateChange).toHaveBeenCalledWith('2026-08-09');
  });

  it('uses one calm palette for showtimes sharing the same operational status', () => {
    const openShowtime = { ...showtime, status: 'OPEN_FOR_BOOKING' };
    render(<OperationalShowtimeTimeline showtimes={[
      openShowtime,
      {
        ...openShowtime,
        showtimePublicId: 'showtime-2',
        movie: { publicId: 'movie-2', title: 'Phim khác' },
        auditorium: { ...showtime.auditorium, publicId: 'auditorium-2', name: 'Phòng 2' },
      },
    ]} />);

    expect(screen.getByTestId('operational-palettes')).toHaveTextContent('#153b38|#153b38');
  });
});
