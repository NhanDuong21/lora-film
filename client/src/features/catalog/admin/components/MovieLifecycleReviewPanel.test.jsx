import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import MovieLifecycleReviewPanel from './MovieLifecycleReviewPanel';

const readyMovie = {
  publicId: 'movie-1',
  source: 'TMDB',
  status: 'DRAFT',
  releaseDate: '2030-01-01',
  showtimeCount: 0,
  readiness: {
    healthStatus: 'READY',
    classification: 'READY',
    blockers: [],
    warnings: [],
  },
};

const renderPanel = (movie, tmdbReview) => render(
  <MemoryRouter>
    <MovieLifecycleReviewPanel movie={movie} tmdbReview={tmdbReview} />
  </MemoryRouter>,
);

describe('MovieLifecycleReviewPanel approval target', () => {
  it('offers UPCOMING for a future TMDB movie', () => {
    renderPanel(readyMovie, {
      approvalTarget: 'UPCOMING',
      canApprove: true,
      approvalBlockers: [],
    });

    expect(screen.getByRole('button', { name: 'Duyệt sang Sắp chiếu' })).toBeEnabled();
    expect(screen.queryByRole('button', { name: 'Duyệt sang Đang chiếu' })).not.toBeInTheDocument();
  });

  it('offers NOW_SHOWING for a released movie with an operational showtime', () => {
    renderPanel({ ...readyMovie, releaseDate: '2020-01-01', showtimeCount: 1 }, {
      approvalTarget: 'NOW_SHOWING',
      canApprove: true,
      approvalBlockers: [],
    });

    expect(screen.getByRole('button', { name: 'Duyệt sang Đang chiếu' })).toBeEnabled();
    expect(screen.queryByRole('button', { name: 'Duyệt sang Sắp chiếu' })).not.toBeInTheDocument();
  });

  it('keeps a released movie blocked while no operational showtime exists', () => {
    const blocker = 'NOW_SHOWING requires at least one current or future non-cancelled showtime.';
    renderPanel({ ...readyMovie, releaseDate: '2020-01-01' }, {
      approvalTarget: 'NOW_SHOWING',
      canApprove: false,
      approvalBlockers: [blocker],
    });

    expect(screen.getByRole('button', { name: 'Duyệt sang Đang chiếu' })).toBeDisabled();
    expect(screen.getByText(blocker)).toBeInTheDocument();
  });
});
