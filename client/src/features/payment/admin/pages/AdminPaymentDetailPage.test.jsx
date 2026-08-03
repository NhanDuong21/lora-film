import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminPaymentDetailPage from './AdminPaymentDetailPage';
import { getAdminPayment } from '../../services/paymentService';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ userRole: 'ADMIN' }),
}));

vi.mock('../../services/paymentService', () => ({
  completeCashRefund: vi.fn(),
  createAdminRefund: vi.fn(),
  getAdminPayment: vi.fn(),
  paymentErrorMessage: () => 'Không thể tải giao dịch thử nghiệm.',
  retryAdminRefund: vi.fn(),
}));

function LocationProbe() {
  return <div data-testid="location">{useLocation().pathname}</div>;
}

describe('AdminPaymentDetailPage read failure', () => {
  beforeEach(() => vi.clearAllMocks());

  it('keeps the detail URL and offers retry instead of silently redirecting', async () => {
    getAdminPayment.mockRejectedValue(new Error('backend unavailable'));

    render(
      <MemoryRouter initialEntries={['/admin/payments/payment-1']}>
        <Routes>
          <Route
            path="/admin/payments/:paymentPublicId"
            element={<><AdminPaymentDetailPage /><LocationProbe /></>}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText('Không thể tải chi tiết giao dịch')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument();
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/payments/payment-1');
  });
});
