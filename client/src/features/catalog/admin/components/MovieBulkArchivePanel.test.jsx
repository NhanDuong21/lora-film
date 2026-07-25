import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MovieBulkArchivePanel from './MovieBulkArchivePanel';

describe('MovieBulkArchivePanel', () => {
  it('starts the old TMDB movie archive action', () => {
    const onArchive = vi.fn();

    render(<MovieBulkArchivePanel limit={100} onArchive={onArchive} />);

    fireEvent.click(screen.getByRole('button', { name: /Đưa tối đa 100 phim cũ/i }));
    expect(onArchive).toHaveBeenCalledTimes(1);
  });

  it('renders archived totals and per-movie skip reasons', () => {
    render(
      <MovieBulkArchivePanel
        result={{
          requested: 2,
          archived: 1,
          skipped: 1,
          errors: 0,
          results: [
            { moviePublicId: 'movie-1', title: 'Phim cũ', outcome: 'ARCHIVED', newStatus: 'INACTIVE' },
            { moviePublicId: 'movie-2', title: 'Phim thiếu ngày', outcome: 'SKIPPED', reason: 'Release date changed' },
          ],
        }}
        onArchive={vi.fn()}
      />,
    );

    expect(screen.getByText(/Đã chuyển 1 phim cũ/i)).toBeInTheDocument();
    fireEvent.click(screen.getByText(/Xem 1 phim chưa được lưu trữ/i));
    expect(screen.getByText('Phim thiếu ngày')).toBeInTheDocument();
    expect(screen.getByText('Release date changed')).toBeInTheDocument();
  });
});
