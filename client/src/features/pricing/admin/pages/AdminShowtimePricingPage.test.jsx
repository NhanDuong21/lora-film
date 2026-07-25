import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import AdminShowtimePricingPage from './AdminShowtimePricingPage';

vi.mock('@/features/scheduling/admin/services/adminShowtimeService', () => ({
  default: {
    getShowtimeDetail: vi.fn(),
    getPricing: vi.fn(),
    resolvePricing: vi.fn(),
  },
}));

const detail = {
  success: true,
  data: {
    showtimePublicId: 'showtime-1',
    version: 2,
    status: 'DRAFT',
    movie: { title: 'Superman' },
    cinema: { name: 'Lora Cinema' },
    auditorium: { name: 'Room 1' },
  },
};

const incomplete = {
  success: true,
  data: {
    complete: false,
    currency: 'VND',
    prices: [],
    missingSeatTypes: [{ seatTypeId: 'vip-1', seatTypeCode: 'VIP', seatTypeName: 'Ghế VIP' }],
    ambiguousSeatTypes: [],
  },
};

const renderPage = () => render(
  <MemoryRouter initialEntries={['/admin/showtimes/showtime-1/pricing']}>
    <Routes>
      <Route path="/admin/showtimes/:id/pricing" element={<AdminShowtimePricingPage />} />
    </Routes>
  </MemoryRouter>,
);

describe('AdminShowtimePricingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminShowtimeService.getShowtimeDetail.mockResolvedValue(detail);
    adminShowtimeService.getPricing.mockResolvedValue(incomplete);
    adminShowtimeService.resolvePricing.mockResolvedValue(incomplete);
  });

  it('shows incomplete SeatTypes and resolves with the Showtime version', async () => {
    renderPage();

    expect(await screen.findByText('Snapshot chưa đầy đủ')).toBeInTheDocument();
    expect(screen.getByText('Ghế VIP (VIP)')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Phân giải lại' }));

    await waitFor(() => expect(adminShowtimeService.resolvePricing)
      .toHaveBeenCalledWith('showtime-1', 2));
  });
});
