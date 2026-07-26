import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import CustomerBookingHistory from './CustomerBookingHistory';
import { cancelBooking, getBookingHistory } from '../services/bookingService';

vi.mock('../services/bookingService', () => ({
  cancelBooking: vi.fn(),
  getBookingHistory: vi.fn()
}));

const bookingPage = {
  content: [
    {
      publicId: '11111111-1111-4111-8111-111111111111',
      bookingCode: 'BK-ACTIVE',
      status: 'PENDING_PAYMENT',
      ticketAmount: 170000,
      foodAmount: 25000,
      totalAmount: 195000,
      expiredAt: '2099-07-26T12:05:00Z',
      createdAt: '2026-07-26T12:00:00Z',
      presentation: {
        movieTitle: 'Mưa đỏ',
        moviePosterUrl: 'https://cdn.lorafilm.test/mua-do.jpg',
        cinemaName: 'LoraFilm Sense City Cần Thơ',
        auditoriumName: 'Phòng 3',
        showtimeStart: '2099-07-26T13:00:00Z',
        seats: [
          { seatPublicId: 'seat-a1', label: 'A1', type: 'STANDARD', price: 85000 },
          { seatPublicId: 'seat-a2', label: 'A2', type: 'STANDARD', price: 85000 }
        ]
      },
      food: {
        totalQuantity: 1,
        totalAmount: 25000,
        items: [{ name: 'Bắp rang', quantity: 1, unitPrice: 25000, totalAmount: 25000 }]
      }
    },
    {
      publicId: '22222222-2222-4222-8222-222222222222',
      bookingCode: 'BK-EXPIRED',
      status: 'PENDING_PAYMENT',
      totalAmount: 85000,
      expiredAt: '2000-07-26T12:00:00Z',
      createdAt: '2026-07-26T11:00:00Z'
    },
    {
      publicId: '33333333-3333-4333-8333-333333333333',
      bookingCode: 'BK-CANCELLED',
      status: 'CANCELLED',
      totalAmount: 85000,
      expiredAt: '2099-07-26T12:05:00Z',
      createdAt: '2026-07-26T10:00:00Z'
    }
  ],
  totalPages: 1
};

describe('CustomerBookingHistory pending recovery actions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getBookingHistory.mockResolvedValue(bookingPage);
    cancelBooking.mockResolvedValue({ status: 'CANCELLED' });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('offers checkout and cancellation only for a live pending booking', async () => {
    render(
      <MemoryRouter>
        <CustomerBookingHistory />
      </MemoryRouter>
    );

    const resumeLink = await screen.findByRole('link', {
      name: /tiếp tục thanh toán/i
    });
    expect(resumeLink).toHaveAttribute(
      'href',
      '/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111'
    );
    expect(screen.getAllByRole('button', { name: /hủy giữ ghế/i })).toHaveLength(1);
    expect(screen.getByText(/thời gian giữ ghế đã kết thúc/i)).toBeInTheDocument();
    expect(screen.getByText('Mưa đỏ')).toBeInTheDocument();
    expect(screen.getByText(/LoraFilm Sense City Cần Thơ · Phòng 3/)).toBeInTheDocument();
    expect(screen.getByText('A1, A2')).toBeInTheDocument();
    expect(screen.getByText('Bắp rang x1')).toBeInTheDocument();
    expect(screen.getByText('170.000đ')).toBeInTheDocument();
    expect(screen.getByText('25.000đ')).toBeInTheDocument();
  });

  it('cancels the active hold and refreshes the history', async () => {
    render(
      <MemoryRouter>
        <CustomerBookingHistory />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: /hủy giữ ghế/i }));
    expect(screen.getByRole('dialog', { name: /xác nhận hủy giữ ghế/i }))
      .toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy' }));

    await waitFor(() => {
      expect(cancelBooking).toHaveBeenCalledWith(
        '11111111-1111-4111-8111-111111111111',
        'Khách hàng chủ động hủy giữ ghế từ lịch sử đặt vé'
      );
      expect(getBookingHistory).toHaveBeenCalledTimes(2);
    });
  });
});
