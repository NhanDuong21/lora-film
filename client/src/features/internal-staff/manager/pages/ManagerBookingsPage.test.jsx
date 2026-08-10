import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ManagerBookingsPage from './ManagerBookingsPage';
import managerOperationsService from '../services/managerOperationsService';

const cinemaId = 'b1575c2d-9081-11f1-bf65-0ebab02bf6f5';

vi.mock('react-router-dom', async importOriginal => ({
  ...(await importOriginal()),
  useOutletContext: () => ({
    selectedCinemaId: cinemaId,
    selectedCinema: { publicId: cinemaId, name: 'LoraFilm Landmark 81' },
    cinemaState: { loading: false, error: '' },
  }),
}));

vi.mock('../services/managerOperationsService', () => ({
  default: {
    getBookings: vi.fn(),
    getBookingSummary: vi.fn(),
    getBookingDetail: vi.fn(),
    cancelBookingHold: vi.fn(),
  },
}));

const booking = {
  publicId: 'booking-uuid',
  bookingCode: 'LORAFILM-001',
  movieTitle: 'Đào, Phở và Piano',
  auditoriumName: 'Phòng 1',
  seatCount: 2,
  finalAmount: 180000,
  currency: 'VND',
  bookingStatus: 'PENDING_PAYMENT',
  paymentStatus: 'PENDING',
  expiresAt: '2026-08-09T10:00:00Z',
};

describe('ManagerBookingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    managerOperationsService.getBookings.mockResolvedValue({ data: [booking], pageNo: 0, totalPages: 1, totalElements: 1 });
    managerOperationsService.getBookingSummary.mockResolvedValue({ totalBookings: 1, pendingPayment: 1, confirmed: 0, completed: 0, needsAttention: 0 });
    managerOperationsService.getBookingDetail.mockResolvedValue(booking);
  });

  it('hiển thị đơn đúng phạm vi rạp và chỉ có thao tác hủy giữ ghế chưa thanh toán', async () => {
    render(<MemoryRouter><ManagerBookingsPage /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: 'Đơn đặt vé & giữ ghế' })).toBeInTheDocument();
    expect(await screen.findByText('LORAFILM-001')).toBeInTheDocument();
    expect(screen.getByText('Đào, Phở và Piano')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Hủy giữ ghế/ })).toBeInTheDocument();
    expect(managerOperationsService.getBookings).toHaveBeenCalledWith(cinemaId, expect.any(Object));
  });

  it('gửi lý do khi Manager xác nhận hủy lượt giữ ghế', async () => {
    managerOperationsService.cancelBookingHold.mockResolvedValue({ ...booking, bookingStatus: 'CANCELLED' });
    render(<MemoryRouter><ManagerBookingsPage /></MemoryRouter>);

    fireEvent.click(await screen.findByRole('button', { name: /Hủy giữ ghế/ }));
    fireEvent.change(screen.getByPlaceholderText(/Khách xác nhận/), { target: { value: 'Khách không tiếp tục thanh toán' } });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy giữ ghế' }));

    await waitFor(() => expect(managerOperationsService.cancelBookingHold)
      .toHaveBeenCalledWith(cinemaId, booking.publicId, 'Khách không tiếp tục thanh toán'));
  });
});
