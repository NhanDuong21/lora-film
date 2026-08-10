import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ManagerBookingDetailPage from './ManagerBookingDetailPage';
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
    getBookingDetail: vi.fn(),
    getBookingFoods: vi.fn(),
    cancelBookingHold: vi.fn(),
  },
}));

const booking = {
  publicId: 'booking-uuid',
  bookingCode: 'LORAFILM-001',
  userId: 12,
  bookingStatus: 'CONFIRMED',
  paymentStatus: 'SUCCESS',
  paymentProvider: 'VNPAY',
  paymentReference: 'PAY-001',
  ticketAmount: 180000,
  foodAmount: 50000,
  serviceFee: 5000,
  taxAmount: 0,
  promotionDiscount: 10000,
  voucherDiscount: 0,
  finalAmount: 225000,
  currency: 'VND',
  createdAt: '2026-08-09T08:00:00Z',
  snapshot: {
    movieTitle: 'Đào, Phở và Piano',
    cinemaName: 'LoraFilm Landmark 81',
    auditoriumName: 'Phòng 1',
    showtimeStart: '2026-08-09T10:00:00Z',
    showtimeEnd: '2026-08-09T12:00:00Z',
    duration: 120,
    ageRating: 'T13',
  },
  operationalInfo: {
    reservationState: 'BOOKED',
    bookedSeatCount: 2,
    heldSeatCount: 0,
    paymentAttempted: true,
    stateChangedAt: '2026-08-09T08:05:00Z',
  },
  reservations: [{ publicId: 'reservation-1', seatLabel: 'A1', seatType: 'VIP', status: 'BOOKED' }],
  tickets: [{ publicId: 'ticket-1', ticketCode: 'TICKET-001', seatLabel: 'A1', seatType: 'VIP', ticketPrice: 90000, status: 'ACTIVE' }],
  statusHistories: [{ id: 1, toStatus: 'CONFIRMED', reason: 'PAYMENT_SUCCESS', changedBy: 'PAYMENT_SERVICE', createdAt: '2026-08-09T08:05:00Z' }],
};

describe('ManagerBookingDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    managerOperationsService.getBookingDetail.mockResolvedValue(booking);
    managerOperationsService.getBookingFoods.mockResolvedValue({
      status: 'CONFIRMED',
      totalQuantity: 1,
      finalAmount: 50000,
      items: [{ id: 1, productName: 'Combo bắp nước', quantity: 1, unitPrice: 50000, finalAmount: 50000 }],
    });
  });

  it('hiển thị toàn cảnh đơn gồm suất chiếu, ghế vé, tiền, bắp nước và lịch sử', async () => {
    render(
      <MemoryRouter initialEntries={['/manager/bookings/booking-uuid']}>
        <Routes><Route path="/manager/bookings/:bookingPublicId" element={<ManagerBookingDetailPage />} /></Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByRole('heading', { name: 'LORAFILM-001' })).toBeInTheDocument();
    expect(screen.getByText('Phim và suất chiếu')).toBeInTheDocument();
    expect(screen.getByText('120 phút')).toBeInTheDocument();
    expect(screen.getByText('T13 · Từ đủ 13 tuổi')).toBeInTheDocument();
    expect(screen.getByText('Ghế và vé (1)')).toBeInTheDocument();
    expect(screen.getByText('Combo bắp nước')).toBeInTheDocument();
    expect(screen.getByText('Lịch sử trạng thái đơn')).toBeInTheDocument();
    expect(managerOperationsService.getBookingDetail).toHaveBeenCalledWith(cinemaId, 'booking-uuid');
    expect(managerOperationsService.getBookingFoods).toHaveBeenCalledWith(cinemaId, 'booking-uuid');
  });
});
