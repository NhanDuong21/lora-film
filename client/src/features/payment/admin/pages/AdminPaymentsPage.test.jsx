import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminPaymentsPage from './AdminPaymentsPage';
import {
  getPaymentOperations,
  replayPaymentOperation,
  searchAdminPayments,
} from '../../services/paymentService';

const context = vi.hoisted(() => ({
  role: 'ACCOUNTANT',
  navigate: vi.fn(),
  triggerToast: vi.fn(),
  triggerAlert: vi.fn(),
  triggerConfirm: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => context.navigate,
  useOutletContext: () => ({
    triggerToast: context.triggerToast,
    triggerAlert: context.triggerAlert,
    triggerConfirm: context.triggerConfirm,
  }),
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ userRole: context.role, accountId: 99 }),
}));

vi.mock('../../services/paymentService', () => ({
  exportAdminPayments: vi.fn(),
  assignReconciliation: vi.fn(),
  resolveReconciliation: vi.fn(),
  paymentErrorMessage: () => 'Không thể tải dữ liệu thanh toán.',
  searchAdminPayments: vi.fn(),
  getPaymentOperations: vi.fn(),
  replayPaymentOperation: vi.fn(),
}));

const emptyPage = { content: [], number: 0, totalPages: 0, totalElements: 0 };
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

describe('AdminPaymentsPage role operations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    context.role = 'ACCOUNTANT';
    context.triggerConfirm.mockResolvedValue(true);
    searchAdminPayments.mockResolvedValue(emptyPage);
    getPaymentOperations.mockResolvedValue(webhookPage);
    replayPaymentOperation.mockResolvedValue({});
  });

  it('keeps ACCOUNTANT read-only while allowing transaction export', async () => {
    render(<AdminPaymentsPage />);
    await waitFor(() => expect(searchAdminPayments).toHaveBeenCalled());

    expect(screen.getByRole('button', { name: /Xuất CSV/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Webhook' }));
    expect(await screen.findByText('DELIVERY_FAILED')).toBeInTheDocument();
    expect(screen.getByText(/Kế toán: quyền chỉ đọc/)).toBeInTheDocument();
    expect(screen.queryByTitle('Xử lý lại')).not.toBeInTheDocument();
  });

  it('lets ADMIN confirm replay and calls the canonical operation API', async () => {
    context.role = 'ADMIN';
    render(<AdminPaymentsPage />);
    await waitFor(() => expect(searchAdminPayments).toHaveBeenCalled());

    fireEvent.click(screen.getByRole('button', { name: 'Webhook' }));
    expect(await screen.findByText('DELIVERY_FAILED')).toBeInTheDocument();
    fireEvent.click(screen.getByTitle('Xử lý lại'));

    await waitFor(() => expect(context.triggerConfirm).toHaveBeenCalled());
    await waitFor(() => expect(replayPaymentOperation)
      .toHaveBeenCalledWith('webhooks', 17));
  });
});
