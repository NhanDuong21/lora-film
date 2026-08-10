import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CustomerNotificationBell from './CustomerNotificationBell';
import { notificationCustomerService } from '../services/notificationCustomerService';

vi.mock('../services/notificationCustomerService', () => ({
  NOTIFICATIONS_CHANGED_EVENT: 'lorafilm:notifications-changed',
  notificationCustomerService: {
    unreadCount: vi.fn()
  }
}));

describe('CustomerNotificationBell', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    notificationCustomerService.unreadCount.mockResolvedValue(3);
  });

  it('shows unread count and opens the notification profile tab', async () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<CustomerNotificationBell />} />
          <Route
            path="/profile"
            element={<div>Hộp thư khách hàng</div>}
          />
        </Routes>
      </MemoryRouter>
    );

    const button = await screen.findByRole('button', {
      name: 'Thông báo, 3 chưa đọc'
    });
    expect(button).toHaveTextContent('3');

    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Hộp thư khách hàng')).toBeInTheDocument();
    });
  });
});
