import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminDashboardView from './AdminDashboardPage';
import { getBookingMonitoringSummary } from '@/features/booking/admin/services/adminBookingService';
import { getDashboard } from '@/features/internal-staff/admin/services/userAdminService';

vi.mock('@/features/booking/admin/services/adminBookingService', () => ({
  getBookingMonitoringSummary: vi.fn()
}));
vi.mock('@/features/internal-staff/admin/services/userAdminService', () => ({
  getDashboard: vi.fn()
}));

describe('AdminDashboardView', () => {
  const renderDashboard = () => render(
    <MemoryRouter>
      <AdminDashboardView />
    </MemoryRouter>
  );

  beforeEach(() => {
    vi.clearAllMocks();
    getDashboard.mockResolvedValue({
      totalCustomers: 14,
      totalEmployees: 6,
      pendingPayrolls: 5
    });
  });

  it('renders only monitoring values returned by the backend', async () => {
    getBookingMonitoringSummary.mockResolvedValue({
      bookingToday: 7,
      paymentFailed: 2,
      expiredBooking: 11,
      pendingRetry: 3
    });

    renderDashboard();

    expect(await screen.findByText('7')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('11')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.queryByText('1.24B VNĐ')).not.toBeInTheDocument();
    expect(screen.queryByText(/KH#1402/)).not.toBeInTheDocument();
  });

  it('keeps user-service statistics available when booking monitoring is down', async () => {
    getBookingMonitoringSummary.mockRejectedValue(new Error('unavailable'));

    renderDashboard();

    expect(await screen.findByText(/Dữ liệu đặt vé tạm thời chưa khả dụng/i))
      .toBeInTheDocument();
    expect(screen.getByText('14')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('shows the retry state when every dashboard source is unavailable', async () => {
    getBookingMonitoringSummary.mockRejectedValue(new Error('booking unavailable'));
    getDashboard.mockRejectedValue(new Error('user unavailable'));

    renderDashboard();

    expect(await screen.findByText(/Không thể tải dữ liệu giám sát hệ thống/i))
      .toBeInTheDocument();
    expect(screen.getByRole('button', { name: /thử lại/i })).toBeInTheDocument();
  });
});
