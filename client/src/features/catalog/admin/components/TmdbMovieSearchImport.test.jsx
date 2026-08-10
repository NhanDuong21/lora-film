import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminTmdbService from '@/features/catalog/admin/services/adminTmdbService';
import TmdbMovieSearchImport from './TmdbMovieSearchImport';

vi.mock('@/features/catalog/admin/services/adminTmdbService', () => ({
  default: {
    searchMovies: vi.fn(),
    syncMovieById: vi.fn(),
  },
}));

describe('TmdbMovieSearchImport', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('searches by title, lets the admin select a suggestion, then imports it', async () => {
    adminTmdbService.searchMovies.mockResolvedValue([
      {
        tmdbId: 19995,
        title: 'Avatar',
        originalTitle: 'Avatar',
        originalReleaseDate: '2009-12-18',
        posterUrl: null,
        alreadyImported: false,
      },
    ]);
    adminTmdbService.syncMovieById.mockResolvedValue({
      data: 'Đã nhập phim vào danh sách Chờ hoàn thiện.',
    });

    render(<TmdbMovieSearchImport />);

    fireEvent.change(screen.getByLabelText('Tên phim cần tìm trên TMDB'), {
      target: { value: 'Avatar' },
    });
    const suggestion = await screen.findByRole('option', { name: /Avatar.*2009/ }, { timeout: 2000 });
    fireEvent.click(suggestion);
    fireEvent.click(screen.getByRole('button', { name: 'Nhập phim đã chọn' }));

    await waitFor(() => expect(adminTmdbService.syncMovieById).toHaveBeenCalledWith(19995));
    expect(await screen.findByText('Đã nhập phim vào danh sách Chờ hoàn thiện.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Phim đã được nhập' })).toBeDisabled();
  });

  it('does not allow importing a movie that already exists', async () => {
    adminTmdbService.searchMovies.mockResolvedValue([
      {
        tmdbId: 550,
        title: 'Fight Club',
        originalReleaseDate: '1999-10-15',
        alreadyImported: true,
        localMovieStatus: 'DRAFT',
      },
    ]);

    render(<TmdbMovieSearchImport />);
    fireEvent.change(screen.getByLabelText('Tên phim cần tìm trên TMDB'), {
      target: { value: 'Fight Club' },
    });
    fireEvent.click(await screen.findByRole('option', { name: /Fight Club.*1999/ }, { timeout: 2000 }));

    expect(screen.getByRole('button', { name: 'Phim đã được nhập' })).toBeDisabled();
    expect(screen.getByText(/Phim đã có trong hệ thống/)).toBeInTheDocument();
  });

  it('chuyển lỗi kết nối kỹ thuật thành thông báo tiếng Việt dễ hiểu', async () => {
    adminTmdbService.searchMovies.mockRejectedValue({
      response: { data: { message: 'Connection prematurely closed BEFORE response' } },
    });

    render(<TmdbMovieSearchImport />);
    fireEvent.change(screen.getByLabelText('Tên phim cần tìm trên TMDB'), {
      target: { value: 'Avatar' },
    });

    expect(await screen.findByText(/Kết nối tới nguồn TMDB đang bị gián đoạn/)).toBeInTheDocument();
    expect(screen.queryByText(/Connection prematurely closed/i)).not.toBeInTheDocument();
  });
});
