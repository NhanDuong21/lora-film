import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminDashboardView from './AdminDashboardPage';
import {
  getBookingMonitoringSummary,
  getBookings
} from '@/features/booking/admin/services/adminBookingService';
import { getAnalyticsDashboard } from '@/features/analytics/admin/services/analyticsAdminService';
import { getDashboard } from '@/features/internal-staff/admin/services/userAdminService';

vi.mock('@/features/booking/admin/services/adminBookingService', () => ({
  getBookingMonitoringSummary: vi.fn(),
  getBookings: vi.fn()
}));
vi.mock('@/features/analytics/admin/services/analyticsAdminService', () => ({
  getAnalyticsDashboard: vi.fn()
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
    getAnalyticsDashboard.mockResolvedValue({
      daily: [],
      summary: {}
    });
    getBookings.mockResolvedValue({ content: [] });
  });

  it('renders today KPIs and operational alerts from the backend', async () => {
    getBookingMonitoringSummary.mockResolvedValue({
      bookingToday: 7,
      paymentFailed: 2,
      expiredBooking: 11,
      pendingRetry: 3
    });
    getAnalyticsDashboard.mockResolvedValue({
      daily: [{
        statDate: '2026-08-05',
        netRevenue: 1250000,
        ticketCount: 9,
        occupancyRate: 0.35,
        bookingCount: 7
      }],
      summary: {}
    });

    renderDashboard();

    expect(await screen.findByText(/1\.250\.000/)).toBeInTheDocument();
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByText('35.0%')).toBeInTheDocument();
    expect(screen.getByText(/2 đơn cần được kiểm tra lại/i)).toBeInTheDocument();
    expect(screen.getByText(/11 đơn chưa hoàn tất/i)).toBeInTheDocument();
    expect(screen.getByText(/3 tác vụ đang chờ/i)).toBeInTheDocument();
    expect(screen.queryByText('1.24B VNĐ')).not.toBeInTheDocument();
    expect(screen.queryByText(/KH#1402/)).not.toBeInTheDocument();
  });

  it('keeps user-service statistics available when booking monitoring is down', async () => {
    getBookingMonitoringSummary.mockRejectedValue(new Error('unavailable'));

    renderDashboard();

    expect(await screen.findByText(/Một số dữ liệu vận hành tạm thời chưa khả dụng/i))
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
