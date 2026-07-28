import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PaymentReturnPage from './PaymentReturnPage';
import { getPaymentStatus } from '../../services/paymentService';
import { resetPaymentAttemptKey } from '@/features/booking/customer/services/paymentHandoffService';

const router = vi.hoisted(() => ({
  navigate: vi.fn(),
  params: new URLSearchParams(),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => router.navigate,
  useSearchParams: () => [router.params],
}));

vi.mock('../../services/paymentService', () => ({
  getPaymentStatus: vi.fn(),
  paymentErrorMessage: () => 'Không thể kiểm tra giao dịch lúc này.',
}));

vi.mock('@/features/booking/customer/services/paymentHandoffService', () => ({
  resetPaymentAttemptKey: vi.fn(),
}));

describe('PaymentReturnPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    router.params = new URLSearchParams(
      'paymentPublicId=payment-1&bookingPublicId=booking-1&provider=VNPAY&verified=true',
    );
  });

  it('polls the authoritative status and renders success only after Booking delivery', async () => {
    let resolveStatus;
    getPaymentStatus.mockReturnValue(new Promise(resolve => {
      resolveStatus = resolve;
    }));

    render(<PaymentReturnPage />);

    expect(screen.getByRole('heading', { name: 'Đang xác nhận thanh toán' })).toBeInTheDocument();
    resolveStatus({
      paymentPublicId: 'payment-1',
      bookingPublicId: 'booking-1',
      status: 'SUCCESS',
      reconciliationStatus: 'NONE',
      bookingDeliveryStatus: 'DELIVERED',
    });

    expect(await screen.findByRole(
      'heading',
      { name: 'Thanh toán thành công' },
    )).toBeInTheDocument();
    expect(getPaymentStatus).toHaveBeenCalledWith('payment-1');

    fireEvent.click(screen.getByRole('button', { name: 'Xem chi tiết đơn' }));
    expect(router.navigate).toHaveBeenCalledWith('/bookings/booking-1');
  });

  it('shows reconciliation guidance and does not present a false success', async () => {
    getPaymentStatus.mockResolvedValue({
      paymentPublicId: 'payment-1',
      bookingPublicId: 'booking-1',
      status: 'SUCCESS',
      reconciliationStatus: 'REQUIRED',
      bookingDeliveryStatus: 'PENDING',
    });

    render(<PaymentReturnPage />);

    expect(await screen.findByRole(
      'heading',
      { name: 'Giao dịch đang được đối soát' },
    )).toBeInTheDocument();
    expect(screen.queryByText('Thanh toán thành công')).not.toBeInTheDocument();
  });

  it('ends a failed attempt and clears only its stored retry key', async () => {
    getPaymentStatus.mockResolvedValue({
      paymentPublicId: 'payment-1',
      bookingPublicId: 'booking-1',
      status: 'FAILED',
      reconciliationStatus: 'NONE',
      bookingDeliveryStatus: 'PENDING',
    });

    render(<PaymentReturnPage />);

    expect(await screen.findByRole(
      'heading',
      { name: 'Thanh toán chưa thành công' },
    )).toBeInTheDocument();
    await waitFor(() => expect(resetPaymentAttemptKey)
      .toHaveBeenCalledWith('booking-1', 'VNPAY'));
  });
});
