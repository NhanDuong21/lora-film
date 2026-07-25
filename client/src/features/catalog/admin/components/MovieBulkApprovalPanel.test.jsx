import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MovieBulkApprovalPanel from './MovieBulkApprovalPanel';

describe('MovieBulkApprovalPanel', () => {
  it('caps each filter-based approval run and calls the action once', () => {
    const onApprove = vi.fn();

    render(
      <MovieBulkApprovalPanel
        totalElements={145}
        limit={100}
        onApprove={onApprove}
      />,
    );

    expect(screen.getByText(/Có 145 phim phù hợp/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Duyệt 100 phim' }));
    expect(onApprove).toHaveBeenCalledTimes(1);
  });

  it('renders approved totals and per-movie skip reasons', () => {
    render(
      <MovieBulkApprovalPanel
        totalElements={2}
        result={{
          requested: 2,
          approved: 1,
          skipped: 1,
          errors: 0,
          results: [
            {
              moviePublicId: 'movie-1',
              title: 'Phim hợp lệ',
              outcome: 'APPROVED',
              newStatus: 'UPCOMING',
            },
            {
              moviePublicId: 'movie-2',
              title: 'Phim thiếu poster',
              outcome: 'SKIPPED',
              reasonCode: 'MOVIE_PRIMARY_POSTER_REQUIRED',
              reason: 'Movie must have at least one active primary poster to publish',
            },
          ],
        }}
        onApprove={vi.fn()}
      />,
    );

    expect(screen.getByText('Đã chuyển 1 phim sang trạng thái Sắp chiếu.')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Xem 1 phim chưa được duyệt'));
    expect(screen.getByText('Phim thiếu poster')).toBeInTheDocument();
    expect(screen.getByText(/active primary poster/)).toBeInTheDocument();
  });
});
