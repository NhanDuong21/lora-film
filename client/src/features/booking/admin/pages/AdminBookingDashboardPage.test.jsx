import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminBookingDashboardPage from './AdminBookingDashboardPage';
import {
  getBookings,
  getBookingOperationsSummary
} from '../services/adminBookingService';

vi.mock('../services/adminBookingService', () => ({
  getBookings: vi.fn(),
  getBookingOperationsSummary: vi.fn(),
  updateBookingStatus: vi.fn()
}));

vi.mock('@/features/auth/services/userService', () => ({
  getUserProfiles: vi.fn().mockResolvedValue([{
    accountId: 7,
    customerCode: 'KH000007',
    fullName: 'Nguyễn Minh Duy',
    email: 'duy@example.com',
    phoneNumber: '0900000001'
  }]),
  searchUserProfiles: vi.fn().mockResolvedValue([])
}));

vi.mock('@/features/catalog/admin/services/adminMovieService', () => ({
  default: {
    getMovies: vi.fn().mockResolvedValue({ data: { data: [] } })
  }
}));

vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: {
    getCinemas: vi.fn().mockResolvedValue({ data: { data: [] } })
  }
}));

describe('AdminBookingDashboardPage data provenance', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getBookings.mockResolvedValue({
      content: [{
        id: 1,
        publicId: 'booking-public-1',
        bookingCode: 'BOOKING-FROM-API',
        userId: 7,
        movieId: 91,
        cinemaId: 31,
        auditoriumId: 4,
        showtimeId: 81,
        movieTitle: 'Nhà Có Năm Nàng Tiên',
        cinemaName: 'LoraFilm Hải Châu',
        auditoriumName: '4DX 01',
        showtimeStart: '2099-07-27T12:30:00Z',
        seatCount: 2,
        ticketAmount: 190000,
        foodAmount: 0,
        promotionDiscount: 0,
        voucherDiscount: 0,
        finalAmount: 190000,
        bookingStatus: 'PENDING_PAYMENT',
        paymentStatus: 'PENDING',
        paymentAttempted: false,
        expiresAt: '2099-07-27T10:15:00Z',
        createdAt: '2099-07-27T10:00:00Z'
      }],
      number: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true
    });
    getBookingOperationsSummary.mockResolvedValue({
      totalBookings: 12,
      pendingPayment: 3,
      confirmed: 4,
      completed: 2,
      cancelled: 2,
      expired: 1,
      refunded: 0,
      expiringSoon: 1,
      overdue: 0,
      paymentFailed: 0,
      needsAttention: 1
    });
  });

  it('uses Booking API rows and does not render page-derived analytics as system totals', async () => {
    render(
      <MemoryRouter>
        <AdminBookingDashboardPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('BOOKING-FROM-API')).toBeInTheDocument();
    expect(screen.getByText('Nhà Có Năm Nàng Tiên')).toBeInTheDocument();
    expect(screen.getByText(/LoraFilm Hải Châu · 4DX 01/)).toBeInTheDocument();
    expect(screen.getByText(/2 ghế/)).toBeInTheDocument();
    expect(screen.getByText('Nguyễn Minh Duy')).toBeInTheDocument();
    expect(screen.getByText('KH000007')).toBeInTheDocument();
    expect(screen.getByText('Chưa phát sinh thanh toán')).toBeInTheDocument();
    expect(screen.queryByText(/Mã phim: 91/)).not.toBeInTheDocument();
    expect(screen.getByText('Tổng đơn toàn hệ thống')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getAllByText('Cần xử lý').length).toBeGreaterThan(0);
    expect(screen.queryByText(/Xu hướng Đặt Vé/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Giám Sát Hệ Thống/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Doanh Thu \(Trang\)/i)).not.toBeInTheDocument();
  });
});
