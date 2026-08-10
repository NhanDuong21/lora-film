import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import CinemaDetailPage from './CinemaDetailPage';
import {
  getCinemaBySlug,
  getMovies,
  getSeatLayout,
  getShowtimes
} from '@/features/catalog/customer/services/movieService';

vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getCinemaBySlug: vi.fn(),
  getMovies: vi.fn(),
  getSeatLayout: vi.fn(),
  getShowtimes: vi.fn()
}));

const operatingHours = Array.from({ length: 7 }, (_, index) => ({
  dayOfWeek: index + 1,
  openTime: '08:30',
  closeTime: '01:00',
  isClosed: false
}));

function MovieRouteProbe() {
  const location = useLocation();
  return (
    <p>
      {location.state?.moviePreview?.title} · {location.state?.moviePreview?.primaryPoster}
    </p>
  );
}

describe('CinemaDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getCinemaBySlug.mockResolvedValue({
      publicId: 'cinema-public-1',
      slug: 'lorafilm-01',
      name: 'LoraFilm Sense City Cần Thơ',
      city: 'Cần Thơ',
      district: 'Ninh Kiều',
      address: '01 Đại lộ Hòa Bình',
      hotline: '1900-6801',
      latitude: 10.034185,
      longitude: 105.783461,
      timezone: 'Asia/Ho_Chi_Minh',
      status: 'ACTIVE',
      description: 'Cụm rạp hiện đại của hệ thống LoraFilm.',
      operatingHours,
      gallery: [{
        publicId: 'banner-1',
        mediaType: 'BANNER',
        url: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==',
        title: 'Sảnh rạp',
        displayOrder: 1,
        isPrimary: true,
        status: 'ACTIVE'
      }],
      activeAuditoriums: [{
        publicId: 'auditorium-1',
        name: 'Phòng IMAX',
        screenType: 'IMAX',
        status: 'ACTIVE'
      }]
    });
    getMovies.mockResolvedValue({ data: [] });
    getShowtimes.mockResolvedValue({
      data: [{
        showtimePublicId: 'showtime-1',
        movie: {
          publicId: 'movie-1',
          slug: 'phim-1',
          title: 'Phim kiểm thử',
          posterUrl: 'https://cdn.lorafilm.test/phim-1.jpg'
        },
        movieVersion: { versionName: '2D Vietsub' },
        startTime: '2026-07-30T13:00:00Z'
      }]
    });
    getSeatLayout.mockResolvedValue({
      seats: [{
        seatType: 'STANDARD',
        seatTypeName: 'Ghế thường',
        price: 66000,
        currency: 'VND',
        priced: true
      }]
    });
  });

  it('renders cinema information and ticket prices returned by public APIs', async () => {
    render(
      <MemoryRouter initialEntries={['/cinema/lorafilm-01']}>
        <Routes>
          <Route path="/cinema/:id" element={<CinemaDetailPage />} />
          <Route path="/movies/:movieId" element={<MovieRouteProbe />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('heading', {
      name: 'LoraFilm Sense City Cần Thơ',
      level: 1
    })).toBeInTheDocument();
    expect(screen.getAllByText(/1900-6801/)).not.toHaveLength(0);
    expect(screen.getByText(/Cụm rạp hiện đại/)).toBeInTheDocument();
    expect(screen.getByText(/Phòng IMAX · IMAX/)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText(/66.000/)).toBeInTheDocument();
    });
    expect(getCinemaBySlug).toHaveBeenCalledWith('lorafilm-01');
    expect(getShowtimes).toHaveBeenCalledWith(expect.objectContaining({
      cinemaSlug: 'lorafilm-01',
      page: 0,
      size: 100
    }));
    expect(getSeatLayout).toHaveBeenCalledWith('showtime-1');
    expect(screen.getByRole('img', { name: 'Phim kiểm thử' })).toHaveAttribute(
      'src',
      'https://cdn.lorafilm.test/phim-1.jpg'
    );

    fireEvent.click(screen.getByRole('heading', { name: 'Phim kiểm thử', level: 3 }));
    expect(await screen.findByText(
      'Phim kiểm thử · https://cdn.lorafilm.test/phim-1.jpg'
    )).toBeInTheDocument();
  });
});
