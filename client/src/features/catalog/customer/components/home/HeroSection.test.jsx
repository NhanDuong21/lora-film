import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HeroSection from './HeroSection';
import { getBookingOptions, getMovies } from '@/features/catalog/customer/services/movieService';

vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getMovies: vi.fn(),
  getBookingOptions: vi.fn()
}));

const pageOf = movies => ({
  data: movies,
  content: movies,
  pageNo: 0,
  totalPages: 1,
  totalElements: movies.length,
  first: true,
  last: true
});

describe('HeroSection quick booking', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getMovies.mockImplementation(({ status }) => Promise.resolve(pageOf(
      status === 'NOW_SHOWING'
        ? [{
            publicId: 'movie-now',
            slug: 'phim-dang-chieu',
            title: 'Phim Đang Chiếu',
            releaseDate: '2026-07-01',
            status: 'NOW_SHOWING'
          }]
        : [{
            publicId: 'movie-upcoming',
            slug: 'phim-sap-chieu',
            title: 'Phim Sắp Chiếu',
            releaseDate: '2099-10-01',
            status: 'UPCOMING'
          }]
    )));
    getBookingOptions.mockResolvedValue([]);
  });

  it('loads and groups both now-showing and upcoming movies from the API', async () => {
    render(
      <MemoryRouter>
        <HeroSection />
      </MemoryRouter>
    );

    expect(await screen.findByRole('option', { name: 'Phim Đang Chiếu' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Phim Sắp Chiếu' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Phim đang chiếu' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Phim sắp chiếu' })).toBeInTheDocument();

    expect(getMovies).toHaveBeenCalledWith(expect.objectContaining({
      status: 'NOW_SHOWING',
      size: 100
    }));
    expect(getMovies).toHaveBeenCalledWith(expect.objectContaining({
      status: 'UPCOMING',
      size: 100
    }));
  });

  it('searches the 14-day opening window from an upcoming movie release date', async () => {
    render(
      <MemoryRouter>
        <HeroSection />
      </MemoryRouter>
    );

    const movieSelect = await screen.findByRole('combobox', { name: 'Phim' });
    fireEvent.change(movieSelect, { target: { value: 'phim-sap-chieu' } });

    await waitFor(() => {
      expect(getBookingOptions).toHaveBeenCalledWith('phim-sap-chieu', expect.objectContaining({
        from: '2099-10-01',
        to: '2099-10-14'
      }));
    });
    expect(await screen.findByRole('option', { name: 'Chưa có rạp mở bán' })).toBeInTheDocument();
  });
});
