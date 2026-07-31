import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MasterBookingFunnelPage from './MasterBookingFunnelPage';
import { getCinemas, getMovies, getShowtimes } from '@/features/catalog/customer/services/movieService';

vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getCinemas: vi.fn(),
  getMovies: vi.fn(),
  getShowtimes: vi.fn()
}));

const pageOf = data => ({
  data,
  content: data,
  pageNo: 0,
  totalPages: 1,
  totalElements: data.length
});

describe('MasterBookingFunnelPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getCinemas.mockResolvedValue(pageOf([{
      publicId: 'cinema-1',
      slug: 'lorafilm-can-tho',
      name: 'LoraFilm Cần Thơ',
      city: 'Cần Thơ',
      address: '01 Đại lộ Hòa Bình'
    }]));
    getMovies.mockImplementation(({ status }) => Promise.resolve(pageOf(
      status === 'NOW_SHOWING'
        ? [{
            publicId: 'movie-now',
            slug: 'phim-dang-chieu',
            title: 'Phim Đang Chiếu',
            status: 'NOW_SHOWING',
            genres: ['Hành động'],
            durationMinutes: 120
          }]
        : [{
            publicId: 'movie-upcoming',
            slug: 'phim-sap-chieu',
            title: 'Phim Sắp Chiếu',
            status: 'UPCOMING',
            releaseDate: '2099-10-01',
            genres: ['Phiêu lưu'],
            durationMinutes: 105
          }]
    )));
    getShowtimes.mockResolvedValue(pageOf([]));
  });

  it('uses real cinema cities and shows both movie groups in the booking flow', async () => {
    render(
      <MemoryRouter>
        <MasterBookingFunnelPage />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: 'Cần Thơ' }));
    fireEvent.click(screen.getByText('LoraFilm Cần Thơ'));

    expect(await screen.findByRole('region', { name: 'Phim đang chiếu' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Phim sắp chiếu' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Phim Đang Chiếu/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Phim Sắp Chiếu/ })).toBeInTheDocument();
    expect(getMovies).toHaveBeenCalledWith(expect.objectContaining({ status: 'NOW_SHOWING' }));
    expect(getMovies).toHaveBeenCalledWith(expect.objectContaining({ status: 'UPCOMING' }));
  });

  it('starts an upcoming movie date picker from its release date', async () => {
    render(
      <MemoryRouter>
        <MasterBookingFunnelPage />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: 'Cần Thơ' }));
    fireEvent.click(screen.getByText('LoraFilm Cần Thơ'));
    fireEvent.click(await screen.findByRole('button', { name: /Phim Sắp Chiếu/ }));

    await waitFor(() => {
      expect(getShowtimes).toHaveBeenCalledWith(expect.objectContaining({
        movieSlug: 'phim-sap-chieu',
        cinemaSlug: 'lorafilm-can-tho',
        date: '2099-10-01'
      }));
    });
  });
});
