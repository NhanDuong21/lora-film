import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import EmployeeCashPaymentPage from './EmployeeCashPaymentPage';
import {
  collectCashPayment,
  createCashPayment,
  lookupCashBooking,
} from '../../services/paymentService';

vi.mock('../../services/paymentService', () => ({
  lookupCashBooking: vi.fn(),
  createCashPayment: vi.fn(),
  collectCashPayment: vi.fn(),
  cancelCashPayment: vi.fn(),
  paymentErrorMessage: () => 'Không thể xử lý giao dịch lúc này.',
}));

describe('EmployeeCashPaymentPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'cash-operation-key') });
  });

  it('looks up authoritative Booking amount and collects cash through confirmation modal', async () => {
    lookupCashBooking.mockResolvedValue({
      bookingPublicId: 'booking-1',
      bookingStatus: 'PENDING_PAYMENT',
      accountId: 15,
      movieTitle: 'Nhà Có Năm Nàng Tiên',
      ticketCount: 2,
      amount: 325000,
      currency: 'VND',
      expiresAt: '2026-07-29T03:00:00Z',
    });
    createCashPayment.mockResolvedValue({
      paymentPublicId: 'payment-cash-1',
      status: 'PENDING',
    });
    collectCashPayment.mockResolvedValue({
      paymentPublicId: 'payment-cash-1',
      status: 'SUCCESS',
      receivedAmount: 400000,
      changeAmount: 75000,
    });

    render(<EmployeeCashPaymentPage />);

    fireEvent.change(screen.getByPlaceholderText(/Nhập mã đơn LORAFILM/), {
      target: { value: 'LORAFILM-20260729-000001' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Tra cứu đơn' }));

    expect(await screen.findByText('Nhà Có Năm Nàng Tiên')).toBeInTheDocument();
    expect(screen.getByText(/325\.000/)).toBeInTheDocument();
    expect(lookupCashBooking).toHaveBeenCalledWith('LORAFILM-20260729-000001');

    fireEvent.click(screen.getByRole('button', { name: /Tạo giao dịch tiền mặt/ }));
    expect(await screen.findByText('Đã mở giao dịch tiền mặt')).toBeInTheDocument();
    expect(createCashPayment).toHaveBeenCalledWith(
      { bookingPublicId: 'booking-1' },
      'cash-operation-key',
    );
    fireEvent.click(screen.getByRole('button', { name: 'Đã hiểu' }));

    fireEvent.change(screen.getByLabelText('Tiền khách đưa'), {
      target: { value: '400000' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đã thu' }));

    expect(screen.getByText('Xác nhận đã nhận đủ tiền?')).toBeInTheDocument();
    const confirmButtons = screen.getAllByRole('button', { name: 'Xác nhận đã thu' });
    fireEvent.click(confirmButtons.at(-1));

    await waitFor(() => expect(collectCashPayment).toHaveBeenCalledWith(
      'payment-cash-1',
      400000,
      'cash-operation-key',
    ));
    expect(await screen.findByText('Đã ghi nhận thu tiền')).toBeInTheDocument();
  });
});
