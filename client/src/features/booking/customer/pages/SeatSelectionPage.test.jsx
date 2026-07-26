import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SeatSelectionPage from './SeatSelectionPage';
import { getSeatLayout } from '@/features/catalog/customer/services/movieService';
import { createBooking, getBookingHistory } from '../services/bookingService';
import { getSeatAvailability } from '../services/seatReservationService';

const socket = {
  connect: vi.fn(),
  disconnect: vi.fn(),
  emit: vi.fn(),
  off: vi.fn(),
  on: vi.fn()
};

vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getSeatLayout: vi.fn()
}));

vi.mock('../services/bookingService', () => ({
  createBooking: vi.fn(),
  getBookingDetails: vi.fn(),
  getBookingHistory: vi.fn(),
  getBookingTickets: vi.fn()
}));

vi.mock('../services/seatReservationService', () => ({
  getSeatAvailability: vi.fn()
}));

vi.mock('../services/seatAvailabilitySocket', () => ({
  applySeatAvailabilityUpdates: seats => seats,
  createSeatAvailabilitySocket: () => socket
}));

vi.mock('../components/BookingStepper', () => ({
  default: () => <div>Booking stepper</div>
}));

describe('SeatSelectionPage customer errors', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    getBookingHistory.mockResolvedValue({ content: [] });
    getSeatAvailability.mockResolvedValue({
      maxSeatsPerBooking: 8,
      occupiedSeats: []
    });
    getSeatLayout.mockResolvedValue({
      showtimeId: 9,
      showtimePublicId: 'showtime-public-9',
      serviceDate: '2099-07-27',
      localStartTime: '19:30:00',
      movie: { title: 'Phim thử nghiệm', slug: 'phim-thu-nghiem' },
      movieVersion: { versionName: '2D Phụ đề Việt' },
      cinema: { name: 'LoraFilm Cần Thơ' },
      auditorium: { name: 'Phòng 1' },
      seats: [{
        id: 1,
        publicId: 'seat-public-a1',
        seatCode: 'A1',
        rowLabel: 'A',
        positionRow: 0,
        positionColumn: 0,
        seatType: 'STANDARD',
        seatTypeName: 'Ghế thường',
        price: 85000,
        sellable: true,
        priced: true,
        blockedForShowtime: false,
        operationalStatus: 'ACTIVE'
      }]
    });
  });

  it('shows a Vietnamese modal instead of leaking an English backend toast', async () => {
    createBooking.mockRejectedValue({
      errorCode: 'IDEMPOTENCY_PAYLOAD_CONFLICT',
      message: 'The idempotency key was reused with a different request payload'
    });

    render(
      <MemoryRouter initialEntries={['/booking/seats?showtimeId=showtime-public-9']}>
        <SeatSelectionPage />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: /Ghế A1/i }));
    fireEvent.click(screen.getByRole('button', { name: /Tiếp tục/i }));

    const dialog = await screen.findByRole('alertdialog', {
      name: /Phiên đặt vé đã thay đổi/i
    });
    expect(within(dialog).getByText(/Phiên đặt vé cũ không còn phù hợp/i))
      .toBeInTheDocument();
    expect(within(dialog).queryByText(/idempotency key/i)).not.toBeInTheDocument();

    await waitFor(() => {
      expect(sessionStorage.getItem('booking:create:showtime-public-9')).toBeNull();
    });
  });
});
