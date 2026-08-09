import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import EmployeeBoxOfficePage from './EmployeeBoxOfficePage';
import { getMyEmployeeWorkContext } from '../services/employeeBoxOfficeService';
import { getCinemas, getSeatLayout, getShowtimes } from '@/features/catalog/customer/services/movieService';
import { getSeatAvailability } from '@/features/booking/customer/services/seatReservationService';
import {
  cancelBooking,
  createBooking,
  finalizeCheckout,
  getBookingTickets,
} from '@/features/booking/customer/services/bookingService';
import {
  cancelCashPayment,
  collectCashPayment,
  createCashPayment,
} from '@/features/payment/services/paymentService';

vi.mock('../services/employeeBoxOfficeService', () => ({ getMyEmployeeWorkContext: vi.fn() }));
vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getCinemas: vi.fn(),
  getSeatLayout: vi.fn(),
  getShowtimes: vi.fn(),
}));
vi.mock('@/features/booking/customer/services/seatReservationService', () => ({ getSeatAvailability: vi.fn() }));
vi.mock('@/features/booking/customer/services/bookingService', () => ({
  cancelBooking: vi.fn(),
  createBooking: vi.fn(),
  finalizeCheckout: vi.fn(),
  getBookingTickets: vi.fn(),
}));
vi.mock('@/features/payment/services/paymentService', () => ({
  cancelCashPayment: vi.fn(),
  collectCashPayment: vi.fn(),
  createCashPayment: vi.fn(),
  paymentErrorMessage: vi.fn(() => 'Không thể xử lý'),
}));

const cinema = {
  publicId: 'cinema-1',
  slug: 'cinema-1',
  name: 'LoraFilm Landmark 81',
};
const showtime = {
  showtimePublicId: 'showtime-1',
  movie: { title: 'Người Nhện: Khởi Đầu Mới' },
  movieVersion: { versionName: '2D - Phụ đề' },
  auditorium: { name: 'Phòng 01' },
  startTime: '2099-08-10T02:00:00Z',
  endTime: '2099-08-10T04:30:00Z',
  status: 'OPEN_FOR_BOOKING',
};

describe('EmployeeBoxOfficePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'operation-key') });
    vi.spyOn(window, 'print').mockImplementation(() => undefined);
    getMyEmployeeWorkContext.mockResolvedValue({ cinemaPublicId: cinema.publicId });
    getCinemas.mockResolvedValue({ data: [cinema] });
    getShowtimes.mockResolvedValue({ data: [showtime] });
    getSeatLayout.mockResolvedValue({
      showtimePublicId: showtime.showtimePublicId,
      seats: [{
        id: 1,
        publicId: 'seat-1',
        seatCode: 'A1',
        rowLabel: 'A',
        positionRow: 1,
        positionColumn: 1,
        seatType: 'STANDARD',
        price: 90000,
        priced: true,
        sellable: true,
        operationalStatus: 'ACTIVE',
      }],
    });
    getSeatAvailability.mockResolvedValue({ occupiedSeats: [], maxSeatsPerBooking: 8 });
    createBooking.mockResolvedValue({ publicId: 'booking-1', bookingCode: 'LORAFILM-001' });
    finalizeCheckout.mockResolvedValue({ publicId: 'booking-1', finalAmount: 90000 });
    createCashPayment.mockResolvedValue({ paymentPublicId: 'payment-1', status: 'PENDING' });
    cancelCashPayment.mockResolvedValue({ status: 'CANCELLED' });
    cancelBooking.mockResolvedValue({ status: 'CANCELLED' });
    collectCashPayment.mockResolvedValue({ status: 'SUCCESS', changeAmount: 10000 });
    getBookingTickets.mockResolvedValue([{ publicId: 'ticket-1', ticketCode: 'TICKET-A1', seatLabel: 'A1' }]);
  });

  it('hoàn tất luồng chọn suất, chọn ghế, thu tiền và phát hành vé', async () => {
    render(<EmployeeBoxOfficePage />);

    expect(await screen.findByText('LoraFilm Landmark 81')).toBeInTheDocument();
    fireEvent.click((await screen.findAllByRole('button', { name: /Người Nhện/ }))[0]);
    fireEvent.click(await screen.findByRole('button', { name: 'A1' }));
    fireEvent.click(screen.getByRole('button', { name: /Tạo đơn & chuyển sang thu tiền/ }));

    await waitFor(() => expect(createBooking).toHaveBeenCalledWith(expect.objectContaining({
      showtimePublicId: 'showtime-1',
      seatPublicIds: ['seat-1'],
    })));
    expect(finalizeCheckout).toHaveBeenCalledWith('booking-1', { paymentMethod: 'CASH' });
    expect(createCashPayment).toHaveBeenCalledWith(
      { bookingPublicId: 'booking-1' },
      expect.any(String),
    );

    fireEvent.change(await screen.findByLabelText('Tiền khách đưa tại quầy'), { target: { value: '100000' } });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đã nhận đủ tiền' }));
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đã thu' }));

    await waitFor(() => expect(collectCashPayment).toHaveBeenCalledWith(
      'payment-1',
      100000,
      expect.any(String),
    ));
    expect(await screen.findByText('TICKET-A1')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /In vé/ })).toBeInTheDocument();
  });

  it('hủy giao dịch chưa thu và trả lại ghế khi khách đổi ý', async () => {
    render(<EmployeeBoxOfficePage />);

    fireEvent.click((await screen.findAllByRole('button', { name: /Người Nhện/ }))[0]);
    fireEvent.click(await screen.findByRole('button', { name: 'A1' }));
    fireEvent.click(screen.getByRole('button', { name: /Tạo đơn & chuyển sang thu tiền/ }));

    fireEvent.click(await screen.findByRole('button', { name: 'Hủy đơn chưa thu' }));
    fireEvent.click(screen.getByRole('button', { name: 'Hủy đơn và trả ghế' }));

    await waitFor(() => expect(cancelCashPayment).toHaveBeenCalledWith(
      'payment-1',
      expect.any(String),
    ));
    expect(cancelBooking).toHaveBeenCalledWith(
      'booking-1',
      'Khách đổi ý trước khi thu tiền tại quầy',
    );
    expect(await screen.findByText('Đã hủy đơn chưa thu')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tạo đơn & chuyển sang thu tiền/ })).toBeInTheDocument();
  });
});
