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
    const blocker = 'Muốn chuyển sang Đang chiếu, phim phải có ít nhất một suất chiếu hiện tại hoặc tương lai chưa bị hủy.';
    renderPanel({ ...readyMovie, releaseDate: '2020-01-01' }, {
      approvalTarget: 'NOW_SHOWING',
      canApprove: false,
      approvalBlockers: [blocker],
    });

    expect(screen.getByRole('button', { name: 'Duyệt sang Đang chiếu' })).toBeDisabled();
    expect(screen.getByText(blocker)).toBeInTheDocument();
  });

  it('requires a future exhibition period before replaying an ended movie', () => {
    renderPanel({ ...readyMovie, source: 'MANUAL', status: 'ENDED', releaseDate: '2020-01-01' });

    expect(screen.getByRole('button', { name: 'Đưa vào đợt chiếu lại' })).toBeDisabled();
    expect(screen.getByText('Hãy lập một đợt khai thác mới có ngày bắt đầu sau hôm nay.')).toBeInTheDocument();
  });
});
