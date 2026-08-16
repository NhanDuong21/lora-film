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
    window.sessionStorage.clear();
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

  it('remembers the cinema selected on the homepage for the movie detail flow', async () => {
    getBookingOptions.mockResolvedValue([{
      showtimePublicId: 'showtime-1',
      cinemaPublicId: 'cinema-1',
      cinemaSlug: 'lorafilm-landmark-81',
      cinemaName: 'LoraFilm Landmark 81',
      cinemaCity: 'Ho Chi Minh City',
      serviceDate: '2026-08-16',
      localStartTime: '2026-08-16T18:00:00'
    }]);
    render(
      <MemoryRouter>
        <HeroSection />
      </MemoryRouter>
    );

    fireEvent.change(await screen.findByRole('combobox', { name: 'Phim' }), {
      target: { value: 'phim-dang-chieu' }
    });
    await screen.findByRole('option', { name: 'LoraFilm Landmark 81' });
    fireEvent.change(screen.getByRole('combobox', { name: 'Rạp' }), {
      target: { value: 'cinema-1' }
    });

    expect(JSON.parse(window.sessionStorage.getItem('lorafilm:preferred-cinema')))
      .toEqual(expect.objectContaining({
        publicId: 'cinema-1',
        slug: 'lorafilm-landmark-81',
        city: 'Ho Chi Minh City'
      }));
  });
});
