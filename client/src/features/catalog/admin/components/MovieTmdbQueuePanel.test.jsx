import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MovieTmdbQueuePanel from './MovieTmdbQueuePanel';

describe('MovieTmdbQueuePanel', () => {
  it('shows separated future, old and undated counts', () => {
    render(
      <MovieTmdbQueuePanel
        breakdown={{ total: 17, future: 8, old: 6, undated: 3 }}
        onApprove={vi.fn()}
        onArchive={vi.fn()}
      />,
    );

    expect(screen.getByText('Phim tương lai')).toBeInTheDocument();
    expect(screen.getByText('Phim đã phát hành')).toBeInTheDocument();
    expect(screen.getByText('Thiếu ngày phát hành')).toBeInTheDocument();
    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('routes each action to its matching handler', () => {
    const onApprove = vi.fn();
    const onArchive = vi.fn();
    render(
      <MovieTmdbQueuePanel
        breakdown={{ total: 2, future: 1, old: 1, undated: 0 }}
        onApprove={onApprove}
        onArchive={onArchive}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Duyệt 1 phim' }));
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trữ 1 phim' }));
    expect(onApprove).toHaveBeenCalledTimes(1);
    expect(onArchive).toHaveBeenCalledTimes(1);
  });
});
