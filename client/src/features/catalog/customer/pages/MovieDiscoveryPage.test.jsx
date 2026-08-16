import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom';
import MovieDiscoveryView from './MovieDiscoveryPage';
import { getGenres, getMovies } from '../services/movieService';

vi.mock('../services/movieService', () => ({
  getMovies: vi.fn(),
  getGenres: vi.fn()
}));

function MovieDetailProbe() {
  const { movieId } = useParams();
  return <div>Chi tiết phim: {movieId}</div>;
}

function renderDiscovery() {
  return render(
    <MemoryRouter initialEntries={['/movies']}>
      <Routes>
        <Route path="/movies" element={<MovieDiscoveryView />} />
        <Route path="/movies/:movieId" element={<MovieDetailProbe />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('MovieDiscoveryView', () => {
  let consoleError;

  beforeEach(() => {
    consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    getGenres.mockResolvedValue([{
      publicId: 'genre-public-1',
      name: 'Hành động'
    }]);
    getMovies.mockResolvedValue({
      content: [{
        publicId: 'movie-public-1',
        id: 'movie-public-1',
        slug: 'nha-co-nam-nang-tien',
        title: 'Nhà Có Năm Nàng Tiên',
        posterUrl: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==',
        description: 'Nội dung phim',
        durationMinutes: 120
      }],
      totalPages: 1
    });
  });

  afterEach(() => {
    consoleError.mockRestore();
    vi.clearAllMocks();
  });

  it('renders public movie and genre identities without duplicate-key warnings', async () => {
    render(
      <MemoryRouter>
        <MovieDiscoveryView />
      </MemoryRouter>
    );

    expect(await screen.findAllByText('Nhà Có Năm Nàng Tiên')).not.toHaveLength(0);
    expect(screen.getByRole('option', { name: 'Hành động' })).toHaveValue(
      'genre-public-1'
    );

    await waitFor(() => {
      const keyWarnings = consoleError.mock.calls.filter(call =>
        String(call[0]).includes('unique "key" prop'));
      expect(keyWarnings).toHaveLength(0);
    });
  });

  it('applies search and status filters received from the header URL', async () => {
    render(
      <MemoryRouter initialEntries={['/movies?search=Dune&status=UPCOMING']}>
        <MovieDiscoveryView />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(getMovies).toHaveBeenCalledWith(expect.objectContaining({
        search: 'Dune',
        status: 'UPCOMING'
      }));
    });
  });

  it('opens the movie detail from the main discovery card', async () => {
    renderDiscovery();

    fireEvent.click(await screen.findByRole('link', {
      name: 'Xem chi tiết phim Nhà Có Năm Nàng Tiên'
    }));

    expect(screen.getByText('Chi tiết phim: nha-co-nam-nang-tien')).toBeInTheDocument();
  });

  it('opens the movie detail from the featured movie list', async () => {
    renderDiscovery();

    fireEvent.click(await screen.findByRole('link', {
      name: 'Xem phim nổi bật Nhà Có Năm Nàng Tiên'
    }));

    expect(screen.getByText('Chi tiết phim: nha-co-nam-nang-tien')).toBeInTheDocument();
  });
});
