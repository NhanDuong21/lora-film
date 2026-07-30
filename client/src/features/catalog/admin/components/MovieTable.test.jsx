import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MovieTable from './MovieTable';

const movie = {
  publicId: 'movie-public-1',
  title: 'Paper Tiger',
  status: 'DRAFT',
  source: 'TMDB',
  ageRating: 'P',
  durationMinutes: 115,
  releaseDate: '2026-11-13',
  activeVersionCount: 1,
  mediaCount: 1,
  showtimeCount: 0,
  readiness: {
    healthStatus: 'READY',
    blockers: [],
    warnings: [],
  },
};

const renderMovieTable = (callbacks = {}) => render(
  <MovieTable
    movies={[movie]}
    isInitialLoading={false}
    isRefreshing={false}
    error=""
    emptyDatabase={false}
    currentPage={0}
    pageSize={10}
    totalElements={1}
    totalPages={1}
    onRetry={vi.fn()}
    onPageChange={vi.fn()}
    onOpenDetail={callbacks.onOpenDetail || vi.fn()}
    onOpenEdit={callbacks.onOpenEdit || vi.fn()}
    onDelete={callbacks.onDelete || vi.fn()}
  />,
);

describe('MovieTable', () => {
  it('passes the movie object to detail and edit actions', () => {
    const onOpenDetail = vi.fn();
    const onOpenEdit = vi.fn();

    renderMovieTable({ onOpenDetail, onOpenEdit });

    fireEvent.click(screen.getByRole('button', { name: 'Xem và xử lý' }));
    fireEvent.click(screen.getByRole('button', { name: 'Chỉnh sửa' }));

    expect(onOpenDetail).toHaveBeenCalledWith(movie);
    expect(onOpenEdit).toHaveBeenCalledWith(movie);
  });

  it('passes the movie identifier and title to the delete action', () => {
    const onDelete = vi.fn();

    renderMovieTable({ onDelete });

    fireEvent.click(screen.getByRole('button', { name: 'Xóa bản nháp' }));

    expect(onDelete).toHaveBeenCalledWith(movie.publicId, movie.title);
  });
});
