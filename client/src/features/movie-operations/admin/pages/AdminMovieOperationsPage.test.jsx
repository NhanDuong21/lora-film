import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminMovieOperationsService from '../services/adminMovieOperationsService';
import AdminMovieOperationsPage from './AdminMovieOperationsPage';

vi.mock('../services/adminMovieOperationsService', () => ({
  default: {
    getOverview: vi.fn(),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useOutletContext: () => ({ triggerToast: vi.fn() }),
  };
});

describe('AdminMovieOperationsPage', () => {
  beforeEach(() => {
    adminMovieOperationsService.getOverview.mockResolvedValue({
      movies: {
        total: 12,
        ready: 8,
        draft: 2,
        warning: 1,
        blocked: 1,
      },
      activeCinemas: 3,
      draftShowtimes: 4,
      openShowtimes: 10,
      activePricePolicies: 2,
      unavailableSections: [],
    });
  });

  it('presents operational tasks in plain Vietnamese', async () => {
    render(
      <MemoryRouter>
        <AdminMovieOperationsPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('Trung tâm vận hành phim')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('1 phim chưa thể đưa vào lịch')).toBeInTheDocument());
    expect(screen.getByText('4 suất chiếu chưa mở bán')).toBeInTheDocument();
    expect(screen.getByText('Chuỗi sẵn sàng mở bán')).toBeInTheDocument();
    expect(screen.queryByText(/candidate|snapshot|preview/i)).not.toBeInTheDocument();
  });
});
