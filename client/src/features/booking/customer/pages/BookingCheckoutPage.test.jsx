import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BookingCheckoutPage from './BookingCheckoutPage';
import {
  cancelBooking,
  finalizeCheckout,
  getOrCreateScoreRedemptionKey,
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
import scoreCustomerService from '@/features/score/customer/services/scoreCustomerService';

vi.mock('../services/bookingService', () => ({
  BOOKING_CHANGED_EVENT: 'lorafilm:booking-changed',
  cancelBooking: vi.fn(),
  finalizeCheckout: vi.fn(),
  getOrCreateScoreRedemptionKey: vi.fn(),
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

vi.mock('@/features/score/customer/services/scoreCustomerService', () => ({
  default: {
    getScoreBalance: vi.fn(),
    redeemPreview: vi.fn()
  }
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
      totalAmount: 335000,
      snapshot: {
        movieTitle: 'Phim thử nghiệm',
        moviePosterUrl: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==',
        originalTitle: 'Test Movie',
        duration: 120,
        ageRating: 'T13',
        cinemaName: 'LoraFilm',
        auditoriumName: 'Phòng 1',
        showtimeStart: '2099-07-26T13:00:00Z',
        seats: [
          { seatPublicId: 'seat-d6', label: 'D6', type: 'VIP', price: 142500 },
          { seatPublicId: 'seat-d7', label: 'D7', type: 'VIP', price: 142500 }
        ]
      }
    });
    getBookingFoodOrder.mockResolvedValue({
      items: [{
        id: 10,
        productName: 'Bắp rang lớn',
        quantity: 1,
        finalAmount: 50000
      }],
      finalAmount: 50000
    });
    getConcessions.mockResolvedValue([]);
    scoreCustomerService.getScoreBalance.mockResolvedValue({
      data: {
        currentPoints: 100,
        heldPoints: 0
      }
    });
    finalizeCheckout.mockResolvedValue({});
    getOrCreateScoreRedemptionKey.mockReturnValue('score-redemption-key');
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
      name: /tiếp tục thanh toán/i
    }));
    const momoLogo = screen.getByRole('img', { name: 'Logo MoMo' });
    expect(momoLogo).toHaveAttribute(
      'src',
      'https://upload.wikimedia.org/wikipedia/commons/a/a0/MoMo_Logo_App.svg'
    );
    fireEvent.error(momoLogo);
    expect(momoLogo).toHaveAttribute(
      'src',
      expect.stringContaining('res.cloudinary.com/dqc4hufot')
    );
    fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', {
      name: /thanh toán qua vnpay/i
    }));

    await waitFor(() => {
      expect(finalizeCheckout).toHaveBeenCalledWith(
        '11111111-1111-4111-8111-111111111111',
        {
          scorePoints: 0,
          scoreIdempotencyKey: null
        }
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

  it('lets the customer apply Score points and sends the selection while finalizing', async () => {
    scoreCustomerService.redeemPreview.mockResolvedValue({
      data: {
        eligible: true,
        requestedPoints: 50,
        discountAmount: 50000,
        remainingAmount: 285000
      }
    });

    render(
      <MemoryRouter initialEntries={[
        '/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111'
      ]}>
        <BookingCheckoutPage />
      </MemoryRouter>
    );

    fireEvent.change(await screen.findByRole('spinbutton', {
      name: /số điểm muốn dùng/i
    }), { target: { value: '50' } });
    fireEvent.click(screen.getByRole('button', { name: /^dùng điểm$/i }));

    expect(await screen.findByText(/đã chọn 50 điểm/i)).toBeInTheDocument();
    expect(scoreCustomerService.redeemPreview).toHaveBeenCalledWith({
      bookingPublicId: '11111111-1111-4111-8111-111111111111',
      points: 50
    });

    fireEvent.click(screen.getByRole('button', {
      name: /tiếp tục thanh toán/i
    }));
    fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', {
      name: /thanh toán qua vnpay/i
    }));

    await waitFor(() => {
      expect(finalizeCheckout).toHaveBeenCalledWith(
        '11111111-1111-4111-8111-111111111111',
        {
          scorePoints: 50,
          scoreIdempotencyKey: 'score-redemption-key'
        }
      );
    });
  });

  it('revalidates the Booking before Payment and does not call MoMo for a cancelled order', async () => {
    getBookingDetails
      .mockResolvedValueOnce({
        publicId: '11111111-1111-4111-8111-111111111111',
        bookingCode: 'BK-CHECKOUT',
        status: 'PENDING_PAYMENT',
        paymentDeadline: '2099-07-26T12:05:00Z',
        ticketAmount: 285000,
        totalAmount: 285000,
        snapshot: {
          movieTitle: 'Phim thử nghiệm',
          seats: [{ seatPublicId: 'seat-d6', label: 'D6', type: 'VIP' }]
        }
      })
      .mockResolvedValueOnce({
        publicId: '11111111-1111-4111-8111-111111111111',
        bookingCode: 'BK-CHECKOUT',
        status: 'CANCELLED',
        paymentDeadline: '2099-07-26T12:05:00Z',
        ticketAmount: 285000,
        totalAmount: 285000,
        snapshot: {
          movieTitle: 'Phim thử nghiệm',
          seats: [{ seatPublicId: 'seat-d6', label: 'D6', type: 'VIP' }]
        }
      });

    render(
      <MemoryRouter initialEntries={[
        '/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111'
      ]}>
        <BookingCheckoutPage />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', {
      name: /tiếp tục thanh toán/i
    }));
    fireEvent.click(screen.getByRole('button', { name: /momo/i }));
    fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', {
      name: /thanh toán qua momo/i
    }));

    expect(await screen.findAllByText('Đơn đã được hủy')).not.toHaveLength(0);
    expect(screen.getAllByText(/ghế đã được trả lại/i)).not.toHaveLength(0);
    expect(finalizeCheckout).not.toHaveBeenCalled();
    expect(createPaymentHandoff).not.toHaveBeenCalled();
  });

  it('updates checkout immediately when the recovery banner cancels the same Booking', async () => {
    getBookingDetails
      .mockResolvedValueOnce({
        publicId: '11111111-1111-4111-8111-111111111111',
        bookingCode: 'BK-CHECKOUT',
        status: 'PENDING_PAYMENT',
        paymentDeadline: '2099-07-26T12:05:00Z',
        ticketAmount: 285000,
        totalAmount: 285000,
        snapshot: {
          movieTitle: 'Phim thử nghiệm',
          seats: [{ seatPublicId: 'seat-d6', label: 'D6', type: 'VIP' }]
        }
      })
      .mockResolvedValueOnce({
        publicId: '11111111-1111-4111-8111-111111111111',
        bookingCode: 'BK-CHECKOUT',
        status: 'CANCELLED',
        paymentDeadline: '2099-07-26T12:05:00Z',
        ticketAmount: 285000,
        totalAmount: 285000,
        snapshot: {
          movieTitle: 'Phim thử nghiệm',
          seats: [{ seatPublicId: 'seat-d6', label: 'D6', type: 'VIP' }]
        }
      });

    render(
      <MemoryRouter initialEntries={[
        '/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111'
      ]}>
        <BookingCheckoutPage />
      </MemoryRouter>
    );

    await screen.findByText('Phim thử nghiệm');
    window.dispatchEvent(new CustomEvent('lorafilm:booking-changed', {
      detail: {
        action: 'CANCELLED',
        publicId: '11111111-1111-4111-8111-111111111111'
      }
    }));

    expect(await screen.findAllByText('Đơn đã được hủy')).not.toHaveLength(0);
    expect(screen.getByText(/VNPay và MoMo đã được khóa/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', {
      name: /thanh toán qua/i
    })).not.toBeInTheDocument();
  });

  it('renders the authoritative movie, seats, food lines and price breakdown', async () => {
    render(
      <MemoryRouter initialEntries={[
        '/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111'
      ]}>
        <BookingCheckoutPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Phim thử nghiệm')).toBeInTheDocument();
    expect(screen.getByRole('img', {
      name: 'Áp phích phim Phim thử nghiệm'
    })).toHaveAttribute('src', expect.stringContaining('data:image/gif'));
    expect(screen.getByText(/D6 · VIP/)).toBeInTheDocument();
    expect(screen.getByText(/D7 · VIP/)).toBeInTheDocument();
    expect(screen.getByText('Tiền vé (2 ghế):')).toBeInTheDocument();
    expect(screen.getByText('Bắp rang lớn')).toBeInTheDocument();
    expect(screen.getAllByText('50.000đ')).toHaveLength(2);
    expect(screen.getByText('335.000đ')).toBeInTheDocument();
    expect(screen.queryByText(/Tiền vé \(0 ghế\)/)).not.toBeInTheDocument();
  });

  it('paginates the catalog and requests thumbnail-sized lazy images', async () => {
    getConcessions.mockResolvedValue(Array.from({ length: 13 }, (_, index) => ({
      id: index + 1,
      name: `Bắp nước ${index + 1}`,
      description: 'Sản phẩm dùng khi xem phim',
      type: 'FOOD',
      price: 39000,
      imageUrl: `https://images.unsplash.com/photo-${index + 1}`
    })));

    render(
      <MemoryRouter initialEntries={[
        '/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111'
      ]}>
        <BookingCheckoutPage />
      </MemoryRouter>
    );

    const firstThumbnail = await screen.findByRole('img', { name: 'Bắp nước 1' });
    expect(firstThumbnail).toHaveAttribute('loading', 'lazy');
    expect(firstThumbnail).toHaveAttribute('decoding', 'async');
    expect(firstThumbnail.getAttribute('src')).toContain('w=192');
    expect(firstThumbnail.getAttribute('src')).toContain('h=192');
    expect(screen.queryByText('Bắp nước 13')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Xem tất cả' }));
    fireEvent.click(screen.getByRole('button', { name: 'Trang sau' }));
    expect(await screen.findByText('Bắp nước 13')).toBeInTheDocument();
    expect(screen.queryByText('Bắp nước 1')).not.toBeInTheDocument();
  });

  it('does not rerender concession cards when the hold countdown ticks', async () => {
    let nameReads = 0;
    const concession = {
      id: 99,
      description: 'Sản phẩm kiểm tra render',
      type: 'FOOD',
      price: 39000,
      imageUrl: 'https://images.unsplash.com/photo-render-test'
    };
    Object.defineProperty(concession, 'name', {
      enumerable: true,
      get: () => {
        nameReads += 1;
        return 'Bắp tối ưu render';
      }
    });
    getConcessions.mockResolvedValue([concession]);

    render(
      <MemoryRouter initialEntries={[
        '/bookings/checkout?bookingId=11111111-1111-4111-8111-111111111111'
      ]}>
        <BookingCheckoutPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Bắp tối ưu render')).toBeInTheDocument();
    const readsAfterInitialRender = nameReads;

    await act(async () => {
      await new Promise(resolve => window.setTimeout(resolve, 1100));
    });

    expect(nameReads).toBe(readsAfterInitialRender);
  });
});
