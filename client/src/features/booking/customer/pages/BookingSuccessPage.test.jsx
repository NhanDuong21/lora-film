import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BookingSuccessPage from './BookingSuccessPage';
import { getBookingDetails, getBookingTickets } from '../services/bookingService';

vi.mock('../services/bookingService', () => ({
  getBookingDetails: vi.fn(),
  getBookingTickets: vi.fn(),
}));

vi.mock('../components/BookingStepper', () => ({
  default: () => <div>Booking stepper</div>,
}));

const booking = {
  publicId: 'booking-1',
  bookingCode: 'LORAFILM-001',
  status: 'CONFIRMED',
  paymentStatus: 'SUCCESS',
  createdAt: '2026-08-03T08:00:00Z',
  ticketAmount: 75000,
  totalAmount: 75000,
  promotionDiscount: 0,
  presentation: { showtimeStart: '2026-08-03T12:00:00Z' },
};

const ticket = {
  publicId: 'ticket-1',
  ticketCode: 'TICKET-001',
  seatLabel: 'A1',
  seatType: 'STANDARD',
  movieTitle: 'Lora Demo',
  cinemaName: 'LoraFilm Quận 1',
  auditoriumName: 'Phòng 1',
};

describe('BookingSuccessPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('loads the ticket endpoint before rendering a successful receipt', async () => {
    getBookingDetails.mockResolvedValue(booking);
    getBookingTickets.mockResolvedValue([ticket]);

    render(
      <MemoryRouter initialEntries={['/bookings/success?bookingId=booking-1']}>
        <BookingSuccessPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('THANH TOÁN THÀNH CÔNG')).toBeInTheDocument();
    expect(screen.getByText('Đặt giữ 1 ghế xem phim')).toBeInTheDocument();
    expect(screen.getAllByText('75.000đ')).toHaveLength(2);
    expect(screen.getByText('Thanh toán đã xác nhận')).toBeInTheDocument();
    expect(getBookingTickets).toHaveBeenCalledWith('booking-1');
  });

  it('does not claim success while tickets are not ready', async () => {
    getBookingDetails.mockResolvedValue(booking);
    getBookingTickets.mockResolvedValue([]);

    render(
      <MemoryRouter initialEntries={['/bookings/success?bookingId=booking-1']}>
        <BookingSuccessPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Vé đang được phát hành. Vui lòng thử lại sau ít phút.')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText('THANH TOÁN THÀNH CÔNG')).not.toBeInTheDocument());
  });
});
