import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ManagerPaymentDetailPage from './ManagerPaymentDetailPage';
import managerOperationsService from '../services/managerOperationsService';

const cinemaId = 'b1575c2d-9081-11f1-bf65-0ebab02bf6f5';

vi.mock('react-router-dom', async importOriginal => ({
  ...(await importOriginal()),
  useOutletContext: () => ({
    selectedCinemaId: cinemaId,
    selectedCinema: { publicId: cinemaId, name: 'LoraFilm Landmark 81' },
    cinemaState: { loading: false, error: '' },
  }),
}));

vi.mock('../services/managerOperationsService', () => ({
  default: {
    getPaymentDetail: vi.fn(),
    approveRefund: vi.fn(),
    rejectRefund: vi.fn(),
  },
}));

const refund = {
  refundPublicId: 'refund-uuid',
  refundCode: 'RFD-001',
  amount: 75000,
  currency: 'VND',
  refundComponent: 'FULL_ORDER',
  reasonCode: 'SHOWTIME_CANCELLED',
  reasonDetail: 'Suất chiếu bị hủy',
  status: 'PENDING_APPROVAL',
  requestedAt: '2026-08-09T09:00:00Z',
};

const detail = {
  payment: {
    paymentPublicId: 'payment-uuid',
    paymentTransactionCode: 'PAY-001',
    bookingPublicId: 'booking-uuid',
    status: 'SUCCESS',
    paymentMethod: 'ONLINE',
    provider: 'VNPAY',
    amount: 225000,
    currency: 'VND',
    attemptNumber: 1,
    reconciliationStatus: 'NOT_REQUIRED',
    bookingDeliveryStatus: 'DELIVERED',
    ticketCount: 2,
    ticketAmount: 180000,
    foodAmount: 50000,
    discountAmount: 5000,
    refundableAmount: 225000,
    refundedAmount: 0,
    createdAt: '2026-08-09T08:00:00Z',
    updatedAt: '2026-08-09T08:05:00Z',
  },
  bookingSnapshot: { movieTitle: 'Đào, Phở và Piano', ticketCount: 2, ticketAmount: 180000, foodAmount: 50000, discountAmount: 5000 },
  refundRequests: [refund],
};

describe('ManagerPaymentDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    managerOperationsService.getPaymentDetail.mockResolvedValue(detail);
    managerOperationsService.approveRefund.mockResolvedValue({ ...refund, status: 'REQUESTED' });
  });

  it('hiển thị toàn cảnh giao dịch và cho duyệt yêu cầu hoàn tiền tại trang riêng', async () => {
    render(
      <MemoryRouter initialEntries={['/manager/payments/payment-uuid']}>
        <Routes><Route path="/manager/payments/:paymentPublicId" element={<ManagerPaymentDetailPage />} /></Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByRole('heading', { name: 'PAY-001' })).toBeInTheDocument();
    expect(screen.getByText('Cơ cấu số tiền')).toBeInTheDocument();
    expect(screen.getByText('Diễn biến giao dịch')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Duyệt' }));
    fireEvent.change(screen.getByPlaceholderText(/Nêu căn cứ/), { target: { value: 'Đã xác minh suất chiếu bị hủy' } });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận duyệt' }));

    await waitFor(() => expect(managerOperationsService.approveRefund)
      .toHaveBeenCalledWith(cinemaId, 'refund-uuid', 'Đã xác minh suất chiếu bị hủy'));
  });
});
