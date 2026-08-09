import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import EmployeeRefundRequestPage from './EmployeeRefundRequestPage';
import {
  completeEmployeeCashRefund,
  createEmployeeRefundRequest,
  getEmployeeCashRefunds,
  lookupRefundCandidate,
} from '../../services/paymentService';

vi.mock('../../services/paymentService', () => ({
  lookupRefundCandidate: vi.fn(),
  createEmployeeRefundRequest: vi.fn(),
  getEmployeeCashRefunds: vi.fn(),
  completeEmployeeCashRefund: vi.fn(),
  paymentErrorMessage: vi.fn(() => 'Không thể xử lý'),
}));

const payment = {
  paymentPublicId: '11111111-1111-1111-1111-111111111111',
  paymentTransactionCode: 'PAY-001',
  movieTitle: 'Đào, Phở và Piano',
  status: 'SUCCESS',
  amount: 180000,
  refundedAmount: 0,
  refundableAmount: 180000,
  currency: 'VND',
};

describe('EmployeeRefundRequestPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    lookupRefundCandidate.mockResolvedValue(payment);
    createEmployeeRefundRequest.mockResolvedValue({ refundCode: 'RFD-001', status: 'PENDING_APPROVAL' });
    getEmployeeCashRefunds.mockResolvedValue({ content: [] });
    completeEmployeeCashRefund.mockResolvedValue({ refundCode: 'RFD-CASH-001', status: 'SUCCESS' });
  });

  it('thể hiện đúng quy trình nhân viên tạo và Manager duyệt', () => {
    render(<EmployeeRefundRequestPage />);

    expect(screen.getByRole('heading', { name: 'Tạo yêu cầu hoàn tiền' })).toBeInTheDocument();
    expect(screen.getByText('Nhân viên tạo yêu cầu')).toBeInTheDocument();
    expect(screen.getByText('Quản lý rạp kiểm tra & duyệt')).toBeInTheDocument();
  });

  it('tra cứu giao dịch trong phạm vi rạp và gửi yêu cầu chờ duyệt', async () => {
    render(<EmployeeRefundRequestPage />);

    fireEvent.change(screen.getByLabelText('Mã giao dịch cần hoàn tiền'), { target: { value: 'PAY-001' } });
    fireEvent.click(screen.getByRole('button', { name: 'Tra cứu giao dịch' }));
    expect((await screen.findAllByText(/180\.000/)).length).toBeGreaterThan(0);

    fireEvent.change(screen.getByPlaceholderText(/Ghi rõ tình huống/), { target: { value: 'Khách yêu cầu hoàn do suất chiếu đã bị hủy' } });
    fireEvent.click(screen.getByRole('button', { name: /Gửi quản lý rạp duyệt/ }));

    await waitFor(() => expect(createEmployeeRefundRequest).toHaveBeenCalledWith(
      payment.paymentPublicId,
      expect.objectContaining({ refundType: 'FULL', refundComponent: 'FULL_ORDER' }),
      expect.any(String),
    ));
    expect(await screen.findByText('Đã gửi yêu cầu cho quản lý rạp')).toBeInTheDocument();
  });

  it('cho nhân viên xác nhận khoản hoàn tiền mặt sau khi quản lý rạp duyệt', async () => {
    getEmployeeCashRefunds.mockResolvedValueOnce({
      content: [{
        refundPublicId: 'refund-cash-001',
        refundCode: 'RFD-CASH-001',
        amount: 75000,
        currency: 'VND',
        reasonDetail: 'Suất chiếu bị hủy',
      }],
    }).mockResolvedValue({ content: [] });

    render(<EmployeeRefundRequestPage />);

    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận đã trả tiền' }));
    fireEvent.change(screen.getByPlaceholderText(/PC-LM81/), { target: { value: 'PC-LM81-001' } });
    fireEvent.change(screen.getByPlaceholderText(/Đã kiểm đếm/), { target: { value: 'Đã giao đủ tiền cho khách' } });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đã giao tiền cho khách' }));

    await waitFor(() => expect(completeEmployeeCashRefund).toHaveBeenCalledWith(
      'refund-cash-001',
      { providerReference: 'PC-LM81-001', note: 'Đã giao đủ tiền cho khách' },
    ));
    expect(await screen.findByText('Đã xác nhận hoàn tiền mặt')).toBeInTheDocument();
  });
});
