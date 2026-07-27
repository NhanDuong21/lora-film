import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BookingDetailPage from './BookingDetailPage';
import {
  cancelBooking,
  getBookingDetails,
  getBookingTickets
} from '../services/bookingService';

vi.mock('../services/bookingService', () => ({
  cancelBooking: vi.fn(),
  getBookingDetails: vi.fn(),
  getBookingTickets: vi.fn()
}));

describe('BookingDetailPage customer presentation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getBookingDetails.mockResolvedValue({
      publicId: '11111111-1111-4111-8111-111111111111',
      bookingCode: 'LORAFILM-20260726-000015',
      status: 'PENDING_PAYMENT',
      ticketAmount: 270000,
      foodAmount: 45000,
      totalAmount: 315000,
      currency: 'VND',
      paymentDeadline: '2099-07-26T12:05:00Z',
      createdAt: '2026-07-26T12:00:00Z',
      presentation: {
        movieTitle: 'Mưa đỏ',
        moviePosterUrl: 'https://cdn.lorafilm.test/mua-do.jpg',
        cinemaName: 'LoraFilm Sense City Cần Thơ',
        auditoriumName: 'Phòng 3',
        showtimeStart: '2099-07-26T13:00:00Z',
        seats: [
          { seatPublicId: 'seat-a1', label: 'A1', type: 'STANDARD', price: 135000 },
          { seatPublicId: 'seat-a2', label: 'A2', type: 'STANDARD', price: 135000 }
        ]
      },
      food: {
        totalQuantity: 2,
        totalAmount: 45000,
        items: [
          {
            name: 'Bắp rang caramel',
            quantity: 1,
            unitPrice: 45000,
            totalAmount: 45000
          }
        ]
      }
    });
    getBookingTickets.mockResolvedValue([]);
    cancelBooking.mockResolvedValue({});
  });

  it('shows the movie, visit details, seats, food, and price breakdown', async () => {
    render(
      <MemoryRouter initialEntries={['/bookings/11111111-1111-4111-8111-111111111111']}>
        <Routes>
          <Route path="/bookings/:bookingId" element={<BookingDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('heading', { name: 'Mưa đỏ' })).toBeInTheDocument();
    expect(screen.getByText('LoraFilm Sense City Cần Thơ')).toBeInTheDocument();
    expect(screen.getByText('Phòng 3')).toBeInTheDocument();
    expect(screen.getByText('A1')).toBeInTheDocument();
    expect(screen.getByText('A2')).toBeInTheDocument();
    expect(screen.getByText('Bắp rang caramel')).toBeInTheDocument();
    expect(screen.getByText('270.000đ')).toBeInTheDocument();
    expect(screen.getAllByText('45.000đ')).toHaveLength(2);
    expect(screen.getByText('315.000đ')).toBeInTheDocument();
    expect(screen.queryByText(/price breakdown/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/seat hold released/i)).not.toBeInTheDocument();
  });
});
