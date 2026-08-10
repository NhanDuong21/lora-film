import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ManagerPaymentsPage from './ManagerPaymentsPage';
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
    getPayments: vi.fn(),
    getRefundRequests: vi.fn(),
    getPaymentSummary: vi.fn(),
    getPaymentDetail: vi.fn(),
    approveRefund: vi.fn(),
    rejectRefund: vi.fn(),
  },
}));

const refund = {
  refundPublicId: 'refund-uuid',
  refundCode: 'RFD-001',
  paymentPublicId: 'payment-uuid',
  amount: 90000,
  currency: 'VND',
  refundComponent: 'FULL_ORDER',
  reasonCode: 'SHOWTIME_CANCELLED',
  reasonDetail: 'Suất chiếu bị hủy do sự cố phòng chiếu',
  status: 'PENDING_APPROVAL',
  requestedAt: '2026-08-09T09:00:00Z',
};

describe('ManagerPaymentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    managerOperationsService.getPayments.mockResolvedValue({ content: [], number: 0, totalPages: 0, totalElements: 0 });
    managerOperationsService.getRefundRequests.mockResolvedValue({ content: [refund], number: 0, totalPages: 1, totalElements: 1 });
    managerOperationsService.getPaymentSummary.mockResolvedValue({ totalTransactions: 5, successful: 4, processing: 1, needsFinanceReview: 0 });
  });

  it('giải thích rõ ranh giới giữa Manager và bộ phận đối soát', async () => {
    render(<MemoryRouter><ManagerPaymentsPage /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: 'Giao dịch tại rạp' })).toBeInTheDocument();
    expect(screen.getAllByText(/Admin đối soát|Quản trị viên hoặc kế toán/).length).toBeGreaterThan(0);
    expect(managerOperationsService.getPaymentSummary).toHaveBeenCalledWith(cinemaId);
  });

  it('cho Manager duyệt yêu cầu do nhân viên tạo và bắt buộc ghi chú', async () => {
    managerOperationsService.approveRefund.mockResolvedValue({ ...refund, status: 'REQUESTED' });
    render(<MemoryRouter><ManagerPaymentsPage /></MemoryRouter>);

    fireEvent.click(await screen.findByRole('button', { name: /Yêu cầu hoàn tiền/ }));
    fireEvent.click(await screen.findByRole('button', { name: 'Duyệt' }));
    fireEvent.change(screen.getByPlaceholderText(/Nêu căn cứ/), { target: { value: 'Đã xác minh suất chiếu bị hủy' } });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận duyệt' }));

    await waitFor(() => expect(managerOperationsService.approveRefund)
      .toHaveBeenCalledWith(cinemaId, refund.refundPublicId, 'Đã xác minh suất chiếu bị hủy'));
  });
});
