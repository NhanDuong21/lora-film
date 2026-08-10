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
  it('cho phép duyệt sang Sắp chiếu khi ngày khai thác ở tương lai', () => {
    renderPanel(readyMovie, {
      approvalTarget: 'UPCOMING',
      canApprove: true,
      approvalBlockers: [],
    });

    expect(screen.getByRole('button', { name: 'Duyệt sang Sắp chiếu' })).toBeEnabled();
    expect(screen.queryByRole('button', { name: 'Duyệt sang Đang chiếu' })).not.toBeInTheDocument();
  });

  it('không cho duyệt phim có ngày khai thác là hôm nay hoặc đã qua', () => {
    const blocker = 'Muốn chuyển sang Sắp chiếu, ngày bắt đầu khai thác phải sau hôm nay.';
    renderPanel({ ...readyMovie, releaseDate: '2020-01-01' }, {
      approvalTarget: 'UPCOMING',
      canApprove: false,
      approvalBlockers: [blocker],
    });

    expect(screen.getByRole('button', { name: 'Duyệt sang Sắp chiếu' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Duyệt sang Đang chiếu' })).not.toBeInTheDocument();
    expect(screen.getByText(blocker)).toBeInTheDocument();
  });

  it('requires a future exhibition period before replaying an ended movie', () => {
    renderPanel({ ...readyMovie, source: 'MANUAL', status: 'ENDED', releaseDate: '2020-01-01' });

    expect(screen.getByRole('button', { name: 'Đưa vào đợt chiếu lại' })).toBeDisabled();
    expect(screen.getByText('Hãy lập một đợt khai thác mới có ngày bắt đầu sau hôm nay.')).toBeInTheDocument();
  });
});
