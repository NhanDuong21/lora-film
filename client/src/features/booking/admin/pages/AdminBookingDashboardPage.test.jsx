import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminBookingDashboardPage from './AdminBookingDashboardPage';
import { getBookings } from '../services/adminBookingService';

vi.mock('../services/adminBookingService', () => ({
  getBookings: vi.fn(),
  updateBookingStatus: vi.fn()
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
        ticketAmount: 190000,
        foodAmount: 0,
        promotionDiscount: 0,
        voucherDiscount: 0,
        finalAmount: 190000,
        bookingStatus: 'PENDING_PAYMENT',
        paymentStatus: 'PENDING',
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
  });

  it('uses Booking API rows and does not render page-derived analytics as system totals', async () => {
    render(
      <MemoryRouter>
        <AdminBookingDashboardPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('BOOKING-FROM-API')).toBeInTheDocument();
    expect(screen.getByText('Chờ thanh toán trên trang')).toBeInTheDocument();
    expect(screen.queryByText(/Xu hướng Đặt Vé/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Giám Sát Hệ Thống/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Doanh Thu \(Trang\)/i)).not.toBeInTheDocument();
  });
});
