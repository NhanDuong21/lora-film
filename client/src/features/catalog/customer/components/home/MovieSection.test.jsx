import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MovieSection from './MovieSection';
import { useMoviesQuery } from '@/features/catalog/customer/hooks/useHomepageMovies';

vi.mock('@/features/catalog/customer/hooks/useHomepageMovies', () => ({
  useMoviesQuery: vi.fn(),
}));

const queryResult = movies => ({
  movies,
  loading: false,
  isRefreshing: false,
  error: null,
  retry: vi.fn(),
});

describe('MovieSection homepage discovery', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useMoviesQuery.mockImplementation(({ status }) => queryResult(status === 'NOW_SHOWING'
      ? [{
          publicId: 'movie-now',
          slug: 'phim-dang-chieu',
          title: 'Một tựa phim có tên khá dài để kiểm tra card',
          status: 'NOW_SHOWING',
          bookable: true,
          ageRating: 'T16',
          durationMinutes: 120,
          priceFrom: 60000,
          trailerUrl: 'https://www.youtube.com/watch?v=abc123',
          genres: [
            { genreName: 'Phim Hành Động' },
            { genreName: 'Phim Chính Kịch' },
            { genreName: 'Phim Phiêu Lưu' },
          ],
        }]
      : [{
          publicId: 'movie-upcoming',
          slug: 'phim-sap-chieu',
          title: 'Phim Sắp Chiếu',
          status: 'UPCOMING',
          bookable: true,
          ageRating: 'P',
          durationMinutes: 95,
          releaseDate: '2026-09-01',
          genres: [{ genreName: 'Phim Gia Đình' }],
        }]));
  });

  it('shows compact metadata, a clear CTA and no numeric pagination', () => {
    render(
      <MemoryRouter>
        <MovieSection />
      </MemoryRouter>,
    );

    expect(screen.getByText('T16 · 120 phút · Hành Động, Chính Kịch')).toBeInTheDocument();
    expect(screen.queryByText(/Phiêu Lưu/)).not.toBeInTheDocument();
    expect(screen.getByText('Từ 60.000đ')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mua vé' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Xem trailer' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Trang sau' })).not.toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: /Xem tất cả/ })).toHaveLength(2);
  });

  it('changes to upcoming movies in place', () => {
    render(
      <MemoryRouter>
        <MovieSection />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Phim sắp chiếu' }));

    expect(screen.getByText('Phim Sắp Chiếu')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mua vé' })).toBeEnabled();
    expect(screen.queryByRole('button', { name: 'Xem trailer' })).not.toBeInTheDocument();
    expect(screen.getByText('Khởi chiếu 01/09/2026')).toBeInTheDocument();
  });

  it('opens and closes the trailer modal from the hover action', () => {
    render(
      <MemoryRouter>
        <MovieSection />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Xem trailer' }));

    expect(screen.getByRole('dialog', { name: 'Một tựa phim có tên khá dài để kiểm tra card' })).toBeInTheDocument();
    expect(screen.getByTitle('Trailer phim Một tựa phim có tên khá dài để kiểm tra card')).toHaveAttribute(
      'src',
      'https://www.youtube.com/embed/abc123?autoplay=1&rel=0&modestbranding=1',
    );

    fireEvent.click(screen.getByRole('button', { name: 'Đóng trailer' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
