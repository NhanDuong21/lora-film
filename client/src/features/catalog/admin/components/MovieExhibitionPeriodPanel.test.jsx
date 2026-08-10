import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import MovieExhibitionPeriodPanel from './MovieExhibitionPeriodPanel';

vi.mock('@/features/catalog/admin/services/adminMovieService', () => ({
  default: {
    getExhibitionPeriods: vi.fn(),
    createExhibitionPeriod: vi.fn(),
  },
}));

describe('MovieExhibitionPeriodPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminMovieService.getExhibitionPeriods.mockResolvedValue([]);
  });

  it('creates a new exhibition period for an old movie', async () => {
    adminMovieService.createExhibitionPeriod.mockResolvedValue({ publicId: 'period-1' });
    const onUpdate = vi.fn();

    render(
      <MemoryRouter>
        <MovieExhibitionPeriodPanel movie={{ publicId: 'movie-1', status: 'ENDED' }} onUpdate={onUpdate} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Chưa có lịch sử riêng. Thời gian hiện tại của phim sẽ được lưu lại khi bạn lập đợt khai thác mới.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Lập đợt khai thác mới' }));
    fireEvent.change(screen.getByLabelText(/Ngày bắt đầu khai thác/), {
      target: { value: '2026-08-20' },
    });
    fireEvent.change(screen.getByLabelText(/Ngày kết thúc khai thác/), {
      target: { value: '2026-09-05' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đợt khai thác' }));

    await waitFor(() => {
      expect(adminMovieService.createExhibitionPeriod).toHaveBeenCalledWith('movie-1', {
        startDate: '2026-08-20',
        endDate: '2026-09-05',
        note: null,
      });
    });
    expect(onUpdate).toHaveBeenCalled();
  });
});
