import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BookingCheckoutPage from './BookingCheckoutPage';
import {
  cancelBooking,
  finalizeCheckout,
  getBookingDetails
} from '../services/bookingService';
import {
  getBookingFoodOrder,
  getConcessions
} from '../services/foodService';

vi.mock('../services/bookingService', () => ({
  cancelBooking: vi.fn(),
  finalizeCheckout: vi.fn(),
  getBookingDetails: vi.fn()
}));

vi.mock('../services/foodService', () => ({
  addFoodItem: vi.fn(),
  getBookingFoodOrder: vi.fn(),
  getConcessions: vi.fn(),
  removeFoodItem: vi.fn(),
  updateFoodQuantity: vi.fn()
}));

vi.mock('../components/BookingStepper', () => ({
  default: () => <div>Booking stepper</div>
}));

describe('BookingCheckoutPage cancellation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getBookingDetails.mockResolvedValue({
      publicId: '11111111-1111-4111-8111-111111111111',
      bookingCode: 'BK-CHECKOUT',
      status: 'PENDING_PAYMENT',
      paymentDeadline: '2099-07-26T12:05:00Z',
      ticketAmount: 285000,
      totalAmount: 285000,
      snapshot: {
        movieTitle: 'Phim thử nghiệm',
        moviePoster: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==',
        cinemaName: 'LoraFilm',
        auditoriumName: 'Phòng 1',
        seatCount: 1,
        showtimeStart: '2099-07-26T13:00:00Z'
      }
    });
    getBookingFoodOrder.mockResolvedValue({ items: [], finalAmount: 0 });
    getConcessions.mockResolvedValue([]);
    finalizeCheckout.mockResolvedValue({});
    cancelBooking.mockResolvedValue({ status: 'CANCELLED' });
  });

  it('uses a detailed modal instead of a browser confirm for cancellation', async () => {
    render(
      <MemoryRouter initialEntries={[
        '/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111'
      ]}>
        <BookingCheckoutPage />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: /hủy giao dịch/i }));
    const cancellationDialog = screen.getByRole('dialog', {
      name: /xác nhận hủy giữ ghế/i
    });
    expect(cancellationDialog).toBeInTheDocument();
    expect(screen.getByText(/ghế sẽ được trả lại ngay/i)).toBeInTheDocument();

    expect(within(cancellationDialog).queryByRole('textbox')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy' }));

    await waitFor(() => {
      expect(cancelBooking).toHaveBeenCalledWith(
        '11111111-1111-4111-8111-111111111111',
        'Khách hàng chủ động hủy đặt chỗ tại checkout'
      );
    });
  });
});
