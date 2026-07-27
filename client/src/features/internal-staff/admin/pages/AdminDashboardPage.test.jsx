import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminDashboardView from './AdminDashboardPage';
import { getBookingMonitoringSummary } from '@/features/booking/admin/services/adminBookingService';

vi.mock('@/features/booking/admin/services/adminBookingService', () => ({
  getBookingMonitoringSummary: vi.fn()
}));

describe('AdminDashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders only monitoring values returned by the backend', async () => {
    getBookingMonitoringSummary.mockResolvedValue({
      bookingToday: 7,
      paymentFailed: 2,
      expiredBooking: 11,
      pendingRetry: 3
    });

    render(<AdminDashboardView />);

    expect(await screen.findByText('7')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('11')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.queryByText('1.24B VNĐ')).not.toBeInTheDocument();
    expect(screen.queryByText(/KH#1402/)).not.toBeInTheDocument();
  });

  it('does not replace a failed API request with fabricated zero values', async () => {
    getBookingMonitoringSummary.mockRejectedValue(new Error('unavailable'));

    render(<AdminDashboardView />);

    expect(await screen.findByText(/Không thể tải dữ liệu giám sát hệ thống/i))
      .toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });
});
