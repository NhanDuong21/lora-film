import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminPaymentsPage from './AdminPaymentsPage';
import {
  assignReconciliation,
  getAdminRefunds,
  getPaymentOperations,
  replayPaymentOperation,
  reviewAccountingRefund,
  retryAdminRefund,
  resolveReconciliation,
  searchAdminPayments,
} from '../../services/paymentService';

const context = vi.hoisted(() => ({
  role: 'ACCOUNTANT',
  permissions: [],
  navigate: vi.fn(),
  triggerToast: vi.fn(),
  triggerAlert: vi.fn(),
  triggerConfirm: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => context.navigate,
  useSearchParams: () => [new URLSearchParams(), vi.fn()],
  useOutletContext: () => ({
    triggerToast: context.triggerToast,
    triggerAlert: context.triggerAlert,
    triggerConfirm: context.triggerConfirm,
  }),
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    userRole: context.role,
    accountId: 99,
    user: { role: context.role, permissions: context.permissions },
  }),
}));

vi.mock('../../services/paymentService', () => ({
  exportAdminPayments: vi.fn(),
  assignReconciliation: vi.fn(),
  resolveReconciliation: vi.fn(),
  paymentErrorMessage: () => 'Không thể tải dữ liệu thanh toán.',
  searchAdminPayments: vi.fn(),
  getAdminRefunds: vi.fn(),
  getPaymentOperations: vi.fn(),
  replayPaymentOperation: vi.fn(),
  reviewAccountingRefund: vi.fn(),
  retryAdminRefund: vi.fn(),
}));

const emptyPage = { content: [], number: 0, totalPages: 0, totalElements: 0 };
const transactionPage = {
  content: [{
    paymentPublicId: 'payment-public-id',
    paymentTransactionCode: 'TXN-OLD-ROW',
    status: 'SUCCESS',
    paymentMethod: 'ONLINE',
    provider: 'VNPAY',
    amount: 100000,
    currency: 'VND',
  }],
  number: 0,
  totalPages: 1,
  totalElements: 1,
};
const webhookPage = {
  content: [{
    id: 17,
    providerCode: 'VNPAY',
    eventType: 'IPN',
    processingStatus: 'FAILED',
    retryCount: 1,
    lastErrorSanitized: 'DELIVERY_FAILED',
    receivedAt: '2026-07-29T01:00:00Z',
  }],
  number: 0,
  totalPages: 1,
  totalElements: 1,
};
const refundPage = {
  content: [{
    refundPublicId: 'refund-public-id',
    refundCode: 'REF-20260729-000001',
    paymentPublicId: 'payment-public-id',
    bookingPublicId: 'booking-public-id',
    provider: 'VNPAY',
    refundType: 'PARTIAL',
    refundComponent: 'CONCESSION',
    reasonCode: 'CONCESSION_UNAVAILABLE',
    amount: 50000,
    currency: 'VND',
    automatic: false,
    status: 'FAILED',
    requestedAt: '2026-07-29T01:00:00Z',
  }],
  number: 0,
  totalPages: 1,
  totalElements: 1,
};
const pendingRefundPage = {
  ...refundPage,
  content: [{
    ...refundPage.content[0],
    status: 'PENDING_APPROVAL',
    requestedByAccountId: 42,
  }],
};

describe('AdminPaymentsPage role operations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    context.role = 'ACCOUNTANT';
    context.permissions = [];
    context.triggerConfirm.mockResolvedValue(true);
    searchAdminPayments.mockResolvedValue(emptyPage);
    getAdminRefunds.mockResolvedValue(refundPage);
    getPaymentOperations.mockResolvedValue(webhookPage);
    replayPaymentOperation.mockResolvedValue({});
    reviewAccountingRefund.mockResolvedValue({});
    retryAdminRefund.mockResolvedValue({});
    assignReconciliation.mockResolvedValue({});
    resolveReconciliation.mockResolvedValue({});
  });

  it('keeps ACCOUNTANT away from technical tools while allowing transaction export', async () => {
    render(<AdminPaymentsPage />);
    await waitFor(() => expect(searchAdminPayments).toHaveBeenCalled());

    expect(screen.getByRole('button', { name: /Xuất danh sách/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Công cụ kỹ thuật' })).not.toBeInTheDocument();
    expect(screen.getByText(/Kế toán có quyền xem/)).toBeInTheDocument();
    expect(screen.queryByTitle('Xử lý lại tác vụ lỗi')).not.toBeInTheDocument();
  });

  it('lets ADMIN confirm replay and calls the canonical operation API', async () => {
    context.role = 'ADMIN';
    render(<AdminPaymentsPage />);
    await waitFor(() => expect(searchAdminPayments).toHaveBeenCalled());

    fireEvent.click(screen.getByRole('button', { name: 'Công cụ kỹ thuật' }));
    fireEvent.click(screen.getByRole('button', { name: 'Thông báo nhà cung cấp' }));
    expect(await screen.findByText('Chưa chuyển được kết quả đến hệ thống đích'))
      .toBeInTheDocument();
    fireEvent.click(screen.getByTitle('Xử lý lại tác vụ lỗi'));

    await waitFor(() => expect(context.triggerConfirm).toHaveBeenCalled());
    await waitFor(() => expect(replayPaymentOperation)
      .toHaveBeenCalledWith('webhooks', 17));
  });

  it('shows refund state to ACCOUNTANT without mutation controls', async () => {
    render(<AdminPaymentsPage />);
    await waitFor(() => expect(searchAdminPayments).toHaveBeenCalled());

    fireEvent.click(screen.getByRole('button', { name: 'Theo dõi hoàn tiền' }));
    expect(await screen.findByText('REF-20260729-000001')).toBeInTheDocument();
    expect(getAdminRefunds).toHaveBeenCalled();
    expect(screen.queryByRole('button', { name: /Thử lại/ })).not.toBeInTheDocument();
  });

  it('lets ADMIN retry a failed provider refund without creating a new request', async () => {
    context.role = 'ADMIN';
    render(<AdminPaymentsPage />);
    await waitFor(() => expect(searchAdminPayments).toHaveBeenCalled());

    fireEvent.click(screen.getByRole('button', { name: 'Theo dõi hoàn tiền' }));
    fireEvent.click(await screen.findByRole('button', { name: /Thử lại/ }));

    await waitFor(() => expect(context.triggerConfirm).toHaveBeenCalled());
    await waitFor(() => expect(retryAdminRefund)
      .toHaveBeenCalledWith('refund-public-id'));
  });

  it('lets an independent accounting controller review a refund request', async () => {
    context.role = 'EMPLOYEE';
    context.permissions = ['PAYMENT_VIEW', 'REFUND_APPROVE'];
    getAdminRefunds.mockResolvedValue(pendingRefundPage);

    render(<AdminPaymentsPage />);
    await waitFor(() => expect(searchAdminPayments).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: 'Theo dõi hoàn tiền' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Duyệt' }));
    fireEvent.change(screen.getByLabelText(/Căn cứ quyết định/), {
      target: { value: 'Đã kiểm tra giao dịch gốc và chứng từ.' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận duyệt' }));

    await waitFor(() => expect(reviewAccountingRefund).toHaveBeenCalledWith(
      'refund-public-id',
      'approve',
      'Đã kiểm tra giao dịch gốc và chứng từ.',
    ));
  });

  it('lets an accounting employee receive a reconciliation case without technical replay access', async () => {
    context.role = 'EMPLOYEE';
    context.permissions = ['PAYMENT_VIEW', 'PAYMENT_RECONCILE'];
    getPaymentOperations.mockResolvedValue({
      content: [{
        publicId: 'reconciliation-public-id',
        status: 'OPEN',
        reasonCode: 'PAYMENT_AMOUNT_MISMATCH',
        openedAt: '2026-08-11T02:00:00Z',
      }],
      number: 0,
      totalPages: 1,
      totalElements: 1,
    });

    render(<AdminPaymentsPage />);
    await waitFor(() => expect(searchAdminPayments).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: 'Cần xử lý' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Tiếp nhận' }));
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận' }));

    await waitFor(() => expect(assignReconciliation)
      .toHaveBeenCalledWith('reconciliation-public-id', 99));
    expect(screen.queryByTitle('Xử lý lại tác vụ lỗi')).not.toBeInTheDocument();
  });

  it('clears rows from the previous tab when an operation request fails', async () => {
    context.role = 'ADMIN';
    searchAdminPayments.mockResolvedValueOnce(transactionPage);
    getPaymentOperations.mockRejectedValueOnce(new Error('operation request failed'));

    render(<AdminPaymentsPage />);
    expect(await screen.findByText('TXN-OLD-ROW')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Công cụ kỹ thuật' }));
    fireEvent.click(screen.getByRole('button', { name: 'Thông báo nhà cung cấp' }));

    await waitFor(() => expect(context.triggerAlert).toHaveBeenCalled());
    expect(screen.queryByText('TXN-OLD-ROW')).not.toBeInTheDocument();
    expect(screen.getByText('Chưa có thông báo nhà cung cấp')).toBeInTheDocument();
  });
});
