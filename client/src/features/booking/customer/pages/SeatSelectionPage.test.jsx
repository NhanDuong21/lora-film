import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SeatSelectionPage from './SeatSelectionPage';
import { getSeatLayout } from '@/features/catalog/customer/services/movieService';
import {
  cancelBooking,
  createBooking,
  getActiveBookingForShowtime,
  getBookingDetails
} from '../services/bookingService';
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
  cancelBooking: vi.fn(),
  createBooking: vi.fn(),
  getActiveBookingForShowtime: vi.fn(),
  getBookingDetails: vi.fn(),
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
    getActiveBookingForShowtime.mockResolvedValue(null);
    cancelBooking.mockResolvedValue({ status: 'CANCELLED' });
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

  it('subscribes through Socket.IO and refreshes missed availability after reconnect', async () => {
    render(
      <MemoryRouter initialEntries={['/booking/seats?showtimeId=showtime-public-9']}>
        <SeatSelectionPage />
      </MemoryRouter>
    );

    await screen.findByRole('button', { name: /Ghế A1/i });
    const connectHandler = socket.on.mock.calls
      .find(([eventName]) => eventName === 'connect')?.[1];
    expect(connectHandler).toBeTypeOf('function');

    connectHandler();
    const subscribeCall = socket.emit.mock.calls
      .find(([eventName]) => eventName === 'seat:subscribe');
    expect(subscribeCall?.[1]).toBe('showtime-public-9');
    expect(subscribeCall?.[2]).toBeTypeOf('function');

    subscribeCall[2]({ ok: true });
    await waitFor(() => expect(getSeatAvailability).toHaveBeenCalledTimes(2));
  });

  it('renders a couple as one unit and submits both physical seat ids', async () => {
    getSeatLayout.mockResolvedValue({
      showtimeId: 9,
      showtimePublicId: 'showtime-public-9',
      serviceDate: '2099-07-27',
      localStartTime: '19:30:00',
      movie: { title: 'Couple movie', slug: 'couple-movie' },
      movieVersion: { versionName: '2D' },
      cinema: { name: 'LoraFilm' },
      auditorium: { name: 'Room 1' },
      seats: [
        {
          id: 91,
          publicId: 'seat-public-i1',
          seatCode: 'I1',
          rowLabel: 'I',
          positionRow: 9,
          positionColumn: 1,
          seatType: 'COUPLE',
          seatTypeName: 'Ghế đôi',
          pairGroup: 'I-01',
          price: 78000,
          sellable: true,
          priced: true,
          blockedForShowtime: false,
          operationalStatus: 'ACTIVE'
        },
        {
          id: 92,
          publicId: 'seat-public-i2',
          seatCode: 'I2',
          rowLabel: 'I',
          positionRow: 9,
          positionColumn: 2,
          seatType: 'COUPLE',
          seatTypeName: 'Ghế đôi',
          pairGroup: 'I-01',
          price: 78000,
          sellable: true,
          priced: true,
          blockedForShowtime: false,
          operationalStatus: 'ACTIVE'
        }
      ]
    });
    createBooking.mockResolvedValue({ publicId: 'booking-couple-1' });

    render(
      <MemoryRouter initialEntries={['/booking/seats?showtimeId=showtime-public-9']}>
        <SeatSelectionPage />
      </MemoryRouter>
    );

    const coupleButton = await screen.findByRole('button', { name: /Ghế đôi I1–I2/i });
    expect(screen.getAllByRole('button', { name: /Ghế đôi I1–I2/i })).toHaveLength(1);

    fireEvent.click(coupleButton);
    fireEvent.click(screen.getByRole('button', { name: /^Tiếp tục$/i }));

    await waitFor(() => {
      expect(createBooking).toHaveBeenCalledWith(expect.objectContaining({
        showtimePublicId: 'showtime-public-9',
        seatPublicIds: ['seat-public-i1', 'seat-public-i2']
      }));
    });
  });

  it('blocks booking when the selection leaves an isolated single seat', async () => {
    getSeatLayout.mockResolvedValue({
      showtimeId: 9,
      showtimePublicId: 'showtime-public-9',
      serviceDate: '2099-07-27',
      localStartTime: '19:30:00',
      movie: { title: 'Phim thử nghiệm', slug: 'phim-thu-nghiem' },
      movieVersion: { versionName: '2D' },
      cinema: { name: 'LoraFilm' },
      auditorium: { name: 'Phòng 1' },
      seats: ['A1', 'A2', 'A3'].map((seatCode, index) => ({
        id: index + 1,
        publicId: `seat-public-${seatCode.toLowerCase()}`,
        seatCode,
        rowLabel: 'A',
        positionRow: 1,
        positionColumn: index + 1,
        seatType: 'STANDARD',
        seatTypeName: 'Ghế thường',
        price: 85000,
        sellable: true,
        priced: true,
        blockedForShowtime: false,
        operationalStatus: 'ACTIVE'
      }))
    });

    render(
      <MemoryRouter initialEntries={['/booking/seats?showtimeId=showtime-public-9']}>
        <SeatSelectionPage />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: /Ghế A2/i }));
    fireEvent.click(screen.getByRole('button', { name: /Ghế A3/i }));
    fireEvent.click(screen.getByRole('button', { name: /^Tiếp tục$/i }));

    const dialog = await screen.findByRole('alertdialog', {
      name: /Không thể để trống ghế đơn lẻ/i
    });
    expect(within(dialog).queryByRole('button', { name: /Vẫn tiếp tục/i }))
      .not.toBeInTheDocument();
    expect(createBooking).not.toHaveBeenCalled();

    fireEvent.click(within(dialog).getByRole('button', { name: /Chọn lại ghế/i }));
    expect(screen.queryByRole('alertdialog', {
      name: /Không thể để trống ghế đơn lẻ/i
    })).not.toBeInTheDocument();
  });

  it('prevents a second order and offers resume or cancel for the active showtime', async () => {
    getActiveBookingForShowtime.mockResolvedValue({
      publicId: 'booking-active-1',
      bookingCode: 'LORAFILM-000001',
      expiredAt: '2099-07-27T19:45:00Z'
    });
    getBookingDetails.mockResolvedValue({
      publicId: 'booking-active-1',
      bookingCode: 'LORAFILM-000001',
      paymentDeadline: '2099-07-27T19:45:00Z',
      seatNames: 'B1, B2'
    });

    render(
      <MemoryRouter initialEntries={['/booking/seats?showtimeId=showtime-public-9']}>
        <SeatSelectionPage />
      </MemoryRouter>
    );

    expect(await screen.findByText(/Bạn đang giữ các ghế/i)).toHaveTextContent('B1, B2');
    fireEvent.click(screen.getByRole('button', { name: /Ghế A1/i }));
    fireEvent.click(screen.getByRole('button', { name: /^Tiếp tục$/i }));

    const dialog = await screen.findByRole('alertdialog', {
      name: /Bạn đã có đơn giữ ghế/i
    });
    expect(within(dialog).getByRole('button', { name: /Tiếp tục thanh toán/i }))
      .toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: /Hủy đơn cũ để chọn lại/i }))
      .toBeInTheDocument();
    expect(createBooking).not.toHaveBeenCalled();
  });

  it('cancels the old active order from the conflict popup before allowing a new choice', async () => {
    getActiveBookingForShowtime.mockResolvedValue({
      publicId: 'booking-active-1',
      bookingCode: 'LORAFILM-000001',
      expiredAt: '2099-07-27T19:45:00Z'
    });
    getBookingDetails.mockResolvedValue({
      publicId: 'booking-active-1',
      bookingCode: 'LORAFILM-000001',
      paymentDeadline: '2099-07-27T19:45:00Z',
      seatNames: 'B1, B2'
    });

    render(
      <MemoryRouter initialEntries={['/booking/seats?showtimeId=showtime-public-9']}>
        <SeatSelectionPage />
      </MemoryRouter>
    );

    await screen.findByText(/Bạn đang giữ các ghế/i);
    fireEvent.click(screen.getByRole('button', { name: /Ghế A1/i }));
    fireEvent.click(screen.getByRole('button', { name: /^Tiếp tục$/i }));
    const dialog = await screen.findByRole('alertdialog');
    fireEvent.click(within(dialog).getByRole('button', { name: /Hủy đơn cũ để chọn lại/i }));

    await waitFor(() => {
      expect(cancelBooking).toHaveBeenCalledWith('booking-active-1');
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });
    expect(screen.queryByText(/Bạn đang giữ các ghế/i)).not.toBeInTheDocument();
  });

  it('recovers a backend race into the same Vietnamese active-order popup', async () => {
    getActiveBookingForShowtime
      .mockResolvedValueOnce(null)
      .mockResolvedValueOnce({
        publicId: 'booking-race-winner',
        bookingCode: 'LORAFILM-000002',
        expiredAt: '2099-07-27T19:45:00Z'
      });
    getBookingDetails.mockResolvedValue({
      publicId: 'booking-race-winner',
      bookingCode: 'LORAFILM-000002',
      paymentDeadline: '2099-07-27T19:45:00Z',
      seatNames: 'C1, C2'
    });
    createBooking.mockRejectedValue({
      errorCode: 'BOOKING_ACTIVE_SHOWTIME_EXISTS',
      message: 'The customer already has an active Booking for this showtime'
    });

    render(
      <MemoryRouter initialEntries={['/booking/seats?showtimeId=showtime-public-9']}>
        <SeatSelectionPage />
      </MemoryRouter>
    );

    const seatButton = await screen.findByRole('button', { name: /Ghế A1/i });
    await waitFor(() => expect(getActiveBookingForShowtime).toHaveBeenCalledTimes(1));
    fireEvent.click(seatButton);
    fireEvent.click(screen.getByRole('button', { name: /^Tiếp tục$/i }));

    const dialog = await screen.findByRole('alertdialog', {
      name: /Bạn đã có đơn giữ ghế/i
    });
    expect(within(dialog).getByText(/Mỗi khách chỉ có thể giữ một đơn/i))
      .toBeInTheDocument();
    expect(within(dialog).queryByText(/already has an active Booking/i))
      .not.toBeInTheDocument();
  });
});
