import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CustomerNotificationCenter from './CustomerNotificationCenter';
import { notificationCustomerService } from '../services/notificationCustomerService';

vi.mock('../services/notificationCustomerService', () => ({
  NOTIFICATIONS_CHANGED_EVENT: 'lorafilm:notifications-changed',
  announceNotificationChange: vi.fn(),
  notificationCustomerService: {
    list: vi.fn(),
    markRead: vi.fn(),
    markAllRead: vi.fn()
  }
}));

const ticketNotification = {
  publicId: 'notification-1',
  title: 'Vé của bạn đã sẵn sàng',
  body: 'Thanh toán thành công. Chúc bạn xem phim vui vẻ!',
  notificationType: 'TICKET_PURCHASED',
  category: 'TRANSACTIONAL',
  priority: 'HIGH',
  actionUrl: '/bookings/booking-123',
  readAt: null,
  createdAt: '2026-07-30T01:00:00Z',
  data: {
    bookingPublicId: 'booking-123',
    bookingCode: 'BK-20260730',
    movieTitle: 'Chiến địa',
    cinemaName: 'LoraFilm Nguyễn Du',
    auditoriumName: 'Phòng 2',
    showtime: '2026-08-01T12:30:00Z',
    seatNames: ['A1', 'A2'],
    ticketCodes: ['TICKET-A1', 'TICKET-A2'],
    totalPaid: 190000,
    currency: 'VND',
    foodItems: [{ name: 'Bắp caramel', quantity: 1 }]
  }
};

describe('CustomerNotificationCenter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    notificationCustomerService.list.mockResolvedValue({
      content: [ticketNotification],
      totalPages: 1
    });
    notificationCustomerService.markRead.mockResolvedValue({
      ...ticketNotification,
      readAt: '2026-07-30T02:00:00Z'
    });
  });

  it('shows structured ticket details and opens the owned booking', async () => {
    render(
      <MemoryRouter initialEntries={['/profile?tab=notifications']}>
        <Routes>
          <Route path="/profile" element={<CustomerNotificationCenter />} />
          <Route path="/bookings/:bookingId" element={<div>Chi tiết đơn đặt vé</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Chiến địa')).toBeInTheDocument();
    expect(screen.getByText('BK-20260730')).toBeInTheDocument();
    expect(screen.getByText(/Ghế A1, A2/)).toBeInTheDocument();
    expect(screen.getByText(/190.000/)).toBeInTheDocument();
    expect(screen.getByText(/Bắp caramel ×1/)).toBeInTheDocument();

    fireEvent.click(screen.getByText('Vé của bạn đã sẵn sàng').closest('button'));

    await waitFor(() => {
      expect(notificationCustomerService.markRead).toHaveBeenCalledWith('notification-1');
      expect(screen.getByText('Chi tiết đơn đặt vé')).toBeInTheDocument();
    });
  });

  it('renders an extensible generic notification without ticket fields', async () => {
    notificationCustomerService.list.mockResolvedValue({
      content: [{
        publicId: 'notification-2',
        title: 'Điểm thưởng vừa được cộng',
        body: 'Bạn đã nhận thêm 20 điểm.',
        notificationType: 'SCORE_EARNED',
        category: 'TRANSACTIONAL',
        data: {},
        readAt: '2026-07-30T02:00:00Z',
        createdAt: '2026-07-30T01:00:00Z'
      }],
      totalPages: 1
    });

    render(
      <MemoryRouter>
        <CustomerNotificationCenter />
      </MemoryRouter>
    );

    expect(await screen.findByText('Điểm thưởng vừa được cộng')).toBeInTheDocument();
    expect(screen.queryByText('Vé xem phim')).not.toBeInTheDocument();
  });
});
