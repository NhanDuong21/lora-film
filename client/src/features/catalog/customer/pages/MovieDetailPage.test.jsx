import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MovieDetailPage from './MovieDetailPage';
import {
  getBookingOptions,
  getMovieById,
  getMovies
} from '@/features/catalog/customer/services/movieService';

vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getBookingOptions: vi.fn(),
  getMovieById: vi.fn(),
  getMovies: vi.fn()
}));

describe('MovieDetailPage cinema preview fallback', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
    getMovieById.mockRejectedValue({ status: 404 });
    getBookingOptions.mockResolvedValue([]);
    getMovies.mockResolvedValue({ data: [] });
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

    expect(await screen.findByRole('button', {
      name: 'Chọn suất 09:00 tại LoraFilm Cái Răng, Phòng 1'
    })).toBeInTheDocument();
    expect(screen.getByRole('button', {
      name: 'Chọn suất 09:00 tại LoraFilm Cái Răng, Phòng 2'
    })).toBeInTheDocument();
    expect(screen.getAllByText('09:00')).toHaveLength(2);
  });

  it('prioritizes booking, localizes customer data and keeps the actor list compact', async () => {
    getMovieById.mockResolvedValue({
      publicId: 'movie-1',
      slug: 'phim-1',
      title: 'Phim Nhật demo',
      durationMinutes: 100,
      releaseDate: '2098-12-01',
      status: 'UPCOMING',
      country: 'JP',
      genres: ['Phim Hành Động', 'Phim Hoạt Hình'],
      directors: [{ publicId: 'director-1', fullName: 'Đạo diễn A' }],
      productionCompanies: [{ publicId: 'company-1', name: 'Studio A' }],
      versions: [{ publicId: 'version-1', audioLanguage: 'EN' }],
      actors: Array.from({ length: 10 }, (_, index) => ({
        publicId: `actor-${index + 1}`,
        fullName: `Diễn viên ${index + 1}`,
        characterName: `Nhân vật ${index + 1} (voice)`,
        displayOrder: index + 1
      }))
    });

    render(
      <MemoryRouter initialEntries={['/movies/phim-1']}>
        <Routes>
          <Route path="/movies/:movieId" element={<MovieDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('button', { name: 'Xem lịch chiếu' })).toBeInTheDocument();
    expect(screen.getByText(/Khởi chiếu/)).toHaveTextContent('Khởi chiếu 01/12/2098');
    expect(screen.getByText('Hành Động')).toBeInTheDocument();
    expect(screen.getByText('Hoạt Hình')).toBeInTheDocument();
    expect(screen.getByText('Nhật Bản')).toBeInTheDocument();
    expect(screen.getByText('Tiếng Anh')).toBeInTheDocument();
    expect(screen.getByText('Nhân vật 1')).toBeInTheDocument();
    expect(screen.getAllByText('Lồng tiếng')).toHaveLength(8);
    expect(screen.queryByText('Diễn viên 9')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Xem toàn bộ 10 diễn viên' }));

    expect(screen.getByText('Diễn viên 9')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thu gọn danh sách' })).toHaveAttribute('aria-expanded', 'true');
  });

  it('disables a showtime when the backend marks it sold out', async () => {
    getMovieById.mockResolvedValue({
      publicId: 'movie-1', slug: 'phim-1', title: 'Phim demo',
      durationMinutes: 100, releaseDate: '2098-12-01', genres: []
    });
    getBookingOptions.mockResolvedValue([{
      showtimePublicId: 'sold-out-showtime',
      serviceDate: '2099-01-01',
      startTime: '2099-01-01T02:00:00Z',
      localStartTime: '2099-01-01T09:00:00',
      cinemaPublicId: 'cinema-1',
      cinemaName: 'LoraFilm Cái Răng',
      cinemaCity: 'Can Tho',
      auditoriumPublicId: 'room-1',
      auditoriumName: 'Screen 01 - Standard',
      screenType: 'STANDARD',
      soundType: 'DOLBY_ATMOS',
      movieVersionPublicId: 'version-1',
      format: '2D',
      audioLanguage: 'EN',
      subtitleLanguage: 'VI',
      status: 'SOLD_OUT',
      priceFrom: 65000,
      currency: 'VND'
    }]);

    render(
      <MemoryRouter initialEntries={['/movies/phim-1']}>
        <Routes>
          <Route path="/movies/:movieId" element={<MovieDetailPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByRole('button', {
      name: 'Hết vé 09:00 tại LoraFilm Cái Răng, Phòng 01 · Tiêu chuẩn'
    })).toBeDisabled();
    expect(screen.getByText(/Dolby Atmos/)).toBeInTheDocument();
    expect(screen.getByText('2D · Tiếng Anh · Phụ đề Việt')).toBeInTheDocument();
  });
});
