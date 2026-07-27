import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminFinancePage from './AdminFinancePage';
import { getTopMoviesByRevenue } from '../services/adminAnalyticsService';

vi.mock('../services/adminAnalyticsService', () => ({
  getTopMoviesByRevenue: vi.fn()
}));

describe('AdminFinancePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders movie revenue returned by Analytics Service without mock transactions', async () => {
    getTopMoviesByRevenue.mockResolvedValue({
      currency: 'VND',
      lastUpdatedAt: '2026-07-27T03:00:00',
      movies: [{
        rank: 1,
        movieId: 91,
        movieTitle: 'Phim từ Analytics',
        totalTicketsSold: 24,
        totalRevenue: 1250000
      }]
    });

    render(<AdminFinancePage />);

    expect(await screen.findByText('Phim từ Analytics')).toBeInTheDocument();
    expect(screen.getByText(/24 vé/i)).toBeInTheDocument();
    expect(screen.getByText(/1\.250\.000/)).toBeInTheDocument();
    expect(screen.queryByText('TRX-9823')).not.toBeInTheDocument();
    expect(screen.queryByText('Mai')).not.toBeInTheDocument();
  });

  it('shows an honest empty state when Analytics has no rows', async () => {
    getTopMoviesByRevenue.mockResolvedValue({
      currency: 'VND',
      movies: []
    });

    render(<AdminFinancePage />);

    expect(await screen.findByText('Chưa có dữ liệu doanh thu')).toBeInTheDocument();
  });
});
