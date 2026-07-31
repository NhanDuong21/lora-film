import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MovieDetailPage from './MovieDetailPage';
import {
  getBookingOptions,
  getMovieById
} from '@/features/catalog/customer/services/movieService';

vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getBookingOptions: vi.fn(),
  getMovieById: vi.fn()
}));

describe('MovieDetailPage cinema preview fallback', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getMovieById.mockRejectedValue({ status: 404 });
    getBookingOptions.mockResolvedValue([]);
  });

  it('keeps the movie detail visible when a bookable cinema movie is temporarily absent from the public catalog', async () => {
    render(
      <MemoryRouter initialEntries={[{
        pathname: '/movies/phim-1',
        state: {
          moviePreview: {
            publicId: 'movie-1',
            slug: 'phim-1',
            title: 'Phim từ lịch chiếu',
            primaryPoster: 'https://cdn.lorafilm.test/phim-1.jpg',
            durationMinutes: 120,
            genres: ['Chính kịch']
          }
        }
      }]}>
        <Routes>
          <Route path="/movies/:movieId" element={<MovieDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('heading', {
      name: 'Phim từ lịch chiếu',
      level: 1
    })).toBeInTheDocument();
    expect(screen.getByRole('img', {
      name: 'Áp phích Phim từ lịch chiếu'
    })).toHaveAttribute('src', 'https://cdn.lorafilm.test/phim-1.jpg');
    expect(screen.queryByText('Không tìm thấy thông tin phim.')).not.toBeInTheDocument();
    expect(getMovieById).toHaveBeenCalledWith('phim-1');
  });
});
