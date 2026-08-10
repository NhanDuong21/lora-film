import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminBookingDetailPage from './AdminBookingDetailPage';
import { getBookingDetail, getBookingFoods } from '../services/adminBookingService';

vi.mock('../services/adminBookingService', () => ({
  getBookingDetail: vi.fn(),
  getBookingFoods: vi.fn(),
  updateBookingStatus: vi.fn()
}));

vi.mock('@/features/auth/services/userService', () => ({
  getUserProfile: vi.fn().mockResolvedValue({
    accountId: 5,
    customerCode: 'KH000005',
    fullName: 'Minh Duy',
    email: 'duy@example.com',
    phoneNumber: '0900000001'
  })
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { role: 'ADMIN' } })
}));

vi.mock('@/components/common/ui/uiKit', () => ({
  LazyImage: ({ src, alt, className }) => (
    <img src={src} alt={alt} className={className} />
  )
}));

describe('AdminBookingDetailPage operational detail', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getBookingDetail.mockResolvedValue({
      publicId: '07fd4271-4b6a-4f5e-b5a8-85bebabe3312',
      bookingCode: 'LORAFILM-20260727-000004',
      userId: 5,
      bookingStatus: 'PENDING_PAYMENT',
      paymentStatus: 'PENDING',
      ticketAmount: 260000,
      foodAmount: 0,
      finalAmount: 260000,
      currency: 'VND',
      createdAt: '2099-07-27T10:00:00Z',
      expiresAt: '2099-07-27T10:15:00Z',
      tickets: [],
      reservations: [
        {
          publicId: 'reservation-d6',
          seatPublicId: 'seat-d6',
          seatLabel: 'D6',
          seatType: 'VIP',
          status: 'HELD',
          reservedAt: '2099-07-27T10:00:00Z',
          expiresAt: '2099-07-27T10:15:00Z'
        },
        {
          publicId: 'reservation-d7',
          seatPublicId: 'seat-d7',
          seatLabel: 'D7',
          seatType: 'VIP',
          status: 'HELD',
          reservedAt: '2099-07-27T10:00:00Z',
          expiresAt: '2099-07-27T10:15:00Z'
        }
      ],
      operationalInfo: {
        reservationState: 'HELD',
        heldSeatCount: 2,
        bookedSeatCount: 0,
        releasedSeatCount: 0,
        expiredSeatCount: 0,
        paymentAttempted: false,
        attentionCode: null,
        stateChangedAt: '2099-07-27T10:00:00Z'
      },
      statusHistories: [],
      snapshot: {
        movieTitle: 'Nhà Có Năm Nàng Tiên',
        moviePoster: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==',
        cinemaName: 'LoraFilm Hải Châu',
        auditoriumName: '4DX 01',
        showtimeStart: '2099-07-27T12:30:00Z',
        seats: [
          {
            seatPublicId: 'seat-d6',
            seatLabel: 'D6',
            seatType: 'VIP',
            price: 130000,
            currency: 'VND'
          },
          {
            seatPublicId: 'seat-d7',
            seatLabel: 'D7',
            seatType: 'VIP',
            price: 130000,
            currency: 'VND'
          }
        ]
      }
    });
    getBookingFoods.mockResolvedValue(null);
  });

  it('shows held seats and meaningful snapshot data before tickets exist', async () => {
    render(
      <MemoryRouter initialEntries={[
        '/admin/bookings/07fd4271-4b6a-4f5e-b5a8-85bebabe3312'
      ]}>
        <Routes>
          <Route path="/admin/bookings/:bookingId" element={<AdminBookingDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Nhà Có Năm Nàng Tiên')).toBeInTheDocument();
    expect(screen.getByText(/Ghế D6 \(Ghế VIP\)/)).toBeInTheDocument();
    expect(screen.getByText(/Ghế D7 \(Ghế VIP\)/)).toBeInTheDocument();
    expect(screen.getAllByText('Đang giữ ghế')).toHaveLength(3);
    expect(screen.getByText('KH000005')).toBeInTheDocument();
    expect(screen.getByText('duy@example.com')).toBeInTheDocument();
    expect(screen.getAllByText('Chưa phát sinh thanh toán').length).toBeGreaterThan(0);
    expect(screen.queryByText(/Đã có lỗi xảy ra/i)).not.toBeInTheDocument();
  });
});
