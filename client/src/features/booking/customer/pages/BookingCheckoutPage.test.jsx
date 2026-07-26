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
import {
  createPaymentHandoff,
  getOrCreatePaymentAttemptKey
} from '../services/paymentHandoffService';

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

vi.mock('../services/paymentHandoffService', () => ({
  createPaymentHandoff: vi.fn(),
  getOrCreatePaymentAttemptKey: vi.fn()
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
    getOrCreatePaymentAttemptKey.mockReturnValue('payment-attempt-key');
    createPaymentHandoff.mockResolvedValue({
      paymentPublicId: '22222222-2222-4222-8222-222222222222'
    });
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

  it('locks the Booking amount before handing public identity to Payment Service', async () => {
    render(
      <MemoryRouter initialEntries={[
        '/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111'
      ]}>
        <BookingCheckoutPage />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', {
      name: /xác nhận & tiếp tục/i
    }));
    fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', {
      name: /thanh toán qua vnpay/i
    }));

    await waitFor(() => {
      expect(finalizeCheckout).toHaveBeenCalledWith(
        '11111111-1111-4111-8111-111111111111'
      );
      expect(createPaymentHandoff).toHaveBeenCalledWith({
        bookingPublicId: '11111111-1111-4111-8111-111111111111',
        paymentMethod: 'VNPAY',
        idempotencyKey: 'payment-attempt-key'
      });
    });
    expect(finalizeCheckout.mock.invocationCallOrder[0])
      .toBeLessThan(createPaymentHandoff.mock.invocationCallOrder[0]);
    expect(screen.queryByText(/mô phỏng thanh toán/i)).not.toBeInTheDocument();
  });
});
