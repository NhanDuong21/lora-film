import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import MovieDiscoveryView from './MovieDiscoveryPage';
import { getGenres, getMovies } from '../services/movieService';

vi.mock('../services/movieService', () => ({
  getMovies: vi.fn(),
  getGenres: vi.fn()
}));

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
    render(<MovieDiscoveryView />);

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
});
