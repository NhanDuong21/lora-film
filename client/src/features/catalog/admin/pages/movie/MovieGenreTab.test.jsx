import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminGenreService from '@/features/catalog/admin/services/adminGenreService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import MovieGenreTab from './MovieGenreTab';

vi.mock('@/features/catalog/admin/services/adminGenreService', () => ({
  default: {
    getAllGenres: vi.fn(),
  },
}));

vi.mock('@/features/catalog/admin/services/adminMovieService', () => ({
  default: {
    assignGenres: vi.fn(),
  },
}));

describe('MovieGenreTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminGenreService.getAllGenres.mockResolvedValue({
      success: true,
      data: {
        content: [
          { publicId: 'genre-active', name: 'Hành động', status: 'ACTIVE' },
          { publicId: 'genre-inactive-selected', name: 'Viễn tưởng cũ', status: 'INACTIVE' },
          { publicId: 'genre-inactive-hidden', name: 'Không còn dùng', status: 'INACTIVE' },
        ],
      },
    });
    adminMovieService.assignGenres.mockResolvedValue({ success: true });
  });

  it('keeps an inactive existing genre visible but does not offer other inactive genres', async () => {
    render(
      <MemoryRouter>
        <MovieGenreTab
          movie={{
            publicId: 'movie-1',
            genres: ['Viễn tưởng cũ'],
          }}
          onUpdate={vi.fn()}
        />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Viễn tưởng cũ')).toBeInTheDocument();
    expect(screen.getByText('Đã ngừng sử dụng')).toBeInTheDocument();
    expect(screen.queryByText('Không còn dùng')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Viễn tưởng cũ/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Hành động' }));
    fireEvent.click(screen.getByRole('button', { name: 'Lưu lựa chọn' }));

    await waitFor(() => {
      expect(adminMovieService.assignGenres).toHaveBeenCalledWith('movie-1', ['genre-active']);
    });
  });
});
