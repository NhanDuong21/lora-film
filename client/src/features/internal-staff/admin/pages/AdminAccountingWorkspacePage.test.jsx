import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminAccountingWorkspacePage from './AdminAccountingWorkspacePage';
import { getAnalyticsDashboard } from '@/features/analytics/admin/services/analyticsAdminService';
import {
  getAccountingOverview,
  getAdminRefunds,
  getPaymentOperations,
  searchAdminPayments,
} from '@/features/payment/services/paymentService';
import { getPayrollSummary } from '../services/userAdminService';

const context = {
  userRole: 'EMPLOYEE',
  user: {
    role: 'EMPLOYEE',
    permissions: [
      'PAYMENT_VIEW',
      'PAYMENT_RECONCILE',
      'ANALYTICS_VIEW',
      'PAYROLL_VIEW',
    ],
  },
};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => context,
}));

vi.mock('@/features/analytics/admin/services/analyticsAdminService', () => ({
  getAnalyticsDashboard: vi.fn(),
}));

vi.mock('@/features/payment/services/paymentService', () => ({
  exportAdminPayments: vi.fn(),
  getAccountingOverview: vi.fn(),
  getAdminRefunds: vi.fn(),
  getPaymentOperations: vi.fn(),
  searchAdminPayments: vi.fn(),
}));

vi.mock('../services/userAdminService', () => ({
  getPayrollSummary: vi.fn(),
}));

const page = (content = [], totalElements = content.length) => ({
  content,
  number: 0,
  totalPages: totalElements ? 1 : 0,
  totalElements,
});

describe('AdminAccountingWorkspacePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    context.user.permissions = [
      'PAYMENT_VIEW',
      'PAYMENT_RECONCILE',
      'ANALYTICS_VIEW',
      'PAYROLL_VIEW',
    ];
    getAnalyticsDashboard.mockResolvedValue({
      summary: { netRevenue: 1250000, grossRevenue: 1400000, currency: 'VND' },
    });
    searchAdminPayments.mockResolvedValue(page([{
      paymentPublicId: 'payment-1',
      paymentTransactionCode: 'TXN-1',
      movieTitle: 'Phim kiểm thử',
      provider: 'VNPAY',
      status: 'SUCCESS',
      reconciliationStatus: 'NONE',
      amount: 150000,
      currency: 'VND',
      createdAt: '2026-08-11T02:00:00Z',
    }]));
    getPaymentOperations.mockImplementation((kind, params) => Promise.resolve(
      page([], params.status === 'OPEN' ? 2 : 1),
    ));
    getAdminRefunds.mockResolvedValue(page([], 1));
    getPayrollSummary.mockResolvedValue({
      pendingApproval: 2,
      approved: 1,
      paymentPending: 3,
      paid: 5,
    });
    getAccountingOverview.mockResolvedValue({
      settlementBatchesNeedReview: 0,
      reconciliationCasesOpen: 0,
      cashSessionsNeedVerification: 0,
      accountingPeriodsOpen: 0,
    });
  });

  it('summarizes the full operational accounting loop in one starting screen', async () => {
    render(
      <MemoryRouter>
        <AdminAccountingWorkspacePage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Bàn vận hành kế toán')).toBeInTheDocument();
    expect(screen.getByText(/1\.250\.000/)).toBeInTheDocument();
    expect(screen.getByText('3', { selector: 'p' })).toBeInTheDocument();
    expect(screen.getByText('6', { selector: 'p' })).toBeInTheDocument();
    expect(screen.getByText('Phim kiểm thử')).toBeInTheDocument();
    expect(screen.getByText('Nắm số doanh thu')).toBeInTheDocument();
    expect(screen.getByText('Nhập và khớp lô ngân hàng')).toBeInTheDocument();
    expect(screen.getByText('Xác minh tiền mặt cuối ca')).toBeInTheDocument();
    expect(screen.getByText('Xử lý chênh lệch và lập đề nghị hoàn')).toBeInTheDocument();
    expect(screen.getByText('Chuẩn bị và gửi bảng lương')).toBeInTheDocument();
    expect(screen.getByText('Hoàn tất đối soát kỳ')).toBeInTheDocument();

    await waitFor(() => expect(getPaymentOperations).toHaveBeenCalledWith(
      'reconciliations',
      expect.objectContaining({ status: 'OPEN' }),
    ));
  });

  it('shows a dedicated decision queue for the independent controller', async () => {
    context.user.permissions = [
      'PAYMENT_VIEW',
      'PAYMENT_RECONCILE',
      'ANALYTICS_VIEW',
      'PAYROLL_VIEW',
      'PAYROLL_APPROVE',
      'REFUND_APPROVE',
      'SETTLEMENT_LOCK',
      'ACCOUNTING_PERIOD_VIEW',
      'ACCOUNTING_PERIOD_CLOSE',
      'AUDIT_VIEW',
    ];

    getAccountingOverview.mockResolvedValue({
      settlementBatchesNeedReview: 2,
      reconciliationCasesOpen: 1,
      cashSessionsNeedVerification: 0,
      accountingPeriodsOpen: 1,
    });

    render(
      <MemoryRouter>
        <AdminAccountingWorkspacePage mode="control" />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Bàn kiểm soát kế toán')).toBeInTheDocument();
    expect(screen.getByText('Hàng đợi kiểm soát độc lập')).toBeInTheDocument();
    expect(screen.getByText('Duyệt đề nghị hoàn tiền')).toBeInTheDocument();
    expect(screen.getByText('Duyệt phiếu lương')).toBeInTheDocument();
    expect(screen.getByText('Khóa lô đối soát ngân hàng')).toBeInTheDocument();
    expect(screen.getByText('Khóa kỳ kế toán')).toBeInTheDocument();
    expect(screen.queryByText('Nhập và khớp lô ngân hàng')).not.toBeInTheDocument();
  });
});
