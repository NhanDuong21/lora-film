import { render, screen, within } from '@testing-library/react';
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

  it('separates showtimes from different auditoriums even when version and start time match', async () => {
    getMovieById.mockResolvedValue({
      publicId: 'movie-1',
      slug: 'phim-1',
      title: 'Phim demo',
      durationMinutes: 120,
      releaseDate: '2098-12-01',
      genres: []
    });
    const commonOption = {
      serviceDate: '2099-01-01',
      startTime: '2099-01-01T02:00:00Z',
      endTime: '2099-01-01T04:00:00Z',
      localStartTime: '2099-01-01T09:00:00',
      localEndTime: '2099-01-01T11:00:00',
      cinemaPublicId: 'cinema-1',
      cinemaName: 'LoraFilm Cái Răng',
      movieVersionPublicId: 'version-1',
      versionName: '2D - Phụ đề',
      status: 'OPEN_FOR_BOOKING',
      priceFrom: 65000,
      currency: 'VND'
    };
    getBookingOptions.mockResolvedValue([
      {
        ...commonOption,
        showtimePublicId: 'showtime-room-1',
        auditoriumPublicId: 'room-1',
        auditoriumName: 'Phòng 1',
        screenType: 'STANDARD'
      },
      {
        ...commonOption,
        showtimePublicId: 'showtime-room-2',
        auditoriumPublicId: 'room-2',
        auditoriumName: 'Phòng 2',
        screenType: 'STANDARD'
      }
    ]);

    render(
      <MemoryRouter initialEntries={['/movies/phim-1']}>
        <Routes>
          <Route path="/movies/:movieId" element={<MovieDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    const roomOneHeading = await screen.findByRole('heading', { name: 'Phòng 1', level: 4 });
    const roomTwoHeading = screen.getByRole('heading', { name: 'Phòng 2', level: 4 });
    const roomOnePanel = roomOneHeading.closest('section');
    const roomTwoPanel = roomTwoHeading.closest('section');

    expect(within(roomOnePanel).getByRole('button', {
      name: 'Chọn suất 09:00 tại LoraFilm Cái Răng, phòng Phòng 1'
    })).toBeInTheDocument();
    expect(within(roomTwoPanel).getByRole('button', {
      name: 'Chọn suất 09:00 tại LoraFilm Cái Răng, phòng Phòng 2'
    })).toBeInTheDocument();
    expect(screen.getAllByText('09:00')).toHaveLength(2);
  });
});
